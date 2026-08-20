package com.agenticdev.sdlc.github.pipeline;

import com.agenticdev.sdlc.github.domain.BranchExistsException;
import com.agenticdev.sdlc.github.domain.PushFailedException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ephemeral-container git pipeline: clone → apply patch → commit → push.
 * Mirrors M2's DockerCodeExecutor isolation model.
 */
@Component
public class DockerDiffApplier implements DiffApplier {

    private static final Logger log = LoggerFactory.getLogger(DockerDiffApplier.class);
    private static final String WORKSPACE = "/workspace";
    private static final String IMAGE = "alpine/git:latest";
    private static final long EXEC_TIMEOUT = 180;

    private final DockerClient docker;

    public DockerDiffApplier(DockerClient docker) {
        this.docker = docker;
    }

    @Override
    public Result apply(String repoUrl, String baseRef, String headBranch,
                        String diff, String commitMessage,
                        String authorName, String authorEmail,
                        String pushToken) {
        String containerId = null;
        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(1024L * 1024 * 1024)
                    .withCpuQuota(100_000L)
                    .withCpuPeriod(100_000L);

            CreateContainerResponse created = docker.createContainerCmd(IMAGE)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/")
                    .withEntrypoint("/bin/sh")
                    .withCmd("-c", "sleep 300")
                    .withTty(false)
                    .exec();
            containerId = created.getId();
            docker.startContainerCmd(containerId).exec();
            log.info("DiffApplier container {} started", containerId);

            // 1. Clone
            String authedUrl = embedToken(repoUrl, pushToken);
            CommandResult clone = exec(containerId,
                    "git clone --depth 1 --branch " + sq(baseRef) + " "
                            + sq(authedUrl) + " " + WORKSPACE, "/");
            if (!clone.succeeded()) {
                throw new PushFailedException("clone failed: " + clone.output());
            }

            // 2. Configure author
            exec(containerId, "git config user.name " + sq(authorName), WORKSPACE);
            exec(containerId, "git config user.email " + sq(authorEmail), WORKSPACE);

            // 3. Check branch doesn't already exist remotely (best-effort)
            CommandResult lsRemote = exec(containerId,
                    "git ls-remote --heads origin " + sq(headBranch), WORKSPACE);
            if (lsRemote.succeeded() && !lsRemote.output().trim().isEmpty()) {
                throw new BranchExistsException(headBranch);
            }

            // 4. New branch
            CommandResult checkout = exec(containerId,
                    "git checkout -b " + sq(headBranch), WORKSPACE);
            if (!checkout.succeeded()) {
                throw new PushFailedException("checkout failed: " + checkout.output());
            }

            // 5. Write diff to file and apply
            String b64 = Base64.getEncoder().encodeToString(diff.getBytes(StandardCharsets.UTF_8));
            exec(containerId, "echo " + b64 + " | base64 -d > /tmp/patch.diff", WORKSPACE);
            CommandResult apply = exec(containerId,
                    "git apply --whitespace=nowarn --index /tmp/patch.diff", WORKSPACE);
            if (!apply.succeeded()) {
                // fallback: 3-way
                CommandResult apply2 = exec(containerId,
                        "git apply --3way /tmp/patch.diff", WORKSPACE);
                if (!apply2.succeeded()) {
                    throw new PushFailedException("git apply failed: " + apply.output()
                            + "\n--- 3way ---\n" + apply2.output());
                }
                exec(containerId, "git add -A", WORKSPACE);
            }

            // 6. Capture changed files list
            CommandResult diffNames = exec(containerId,
                    "git diff --cached --name-only", WORKSPACE);
            List<String> changedFiles = diffNames.succeeded()
                    ? Arrays.stream(diffNames.output().split("\\R"))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList()
                    : List.of();

            // 7. Read CODEOWNERS if present
            String codeowners = readIfExists(containerId, ".github/CODEOWNERS");
            if (codeowners == null) codeowners = readIfExists(containerId, "CODEOWNERS");
            if (codeowners == null) codeowners = readIfExists(containerId, "docs/CODEOWNERS");

            // 8. Commit
            String msgB64 = Base64.getEncoder().encodeToString(commitMessage.getBytes(StandardCharsets.UTF_8));
            CommandResult commit = exec(containerId,
                    "git commit -s -F <(echo " + msgB64 + " | base64 -d)", WORKSPACE);
            if (!commit.succeeded()) {
                throw new PushFailedException("commit failed: " + commit.output());
            }

            // 9. Push
            CommandResult push = exec(containerId,
                    "git push origin " + sq(headBranch), WORKSPACE);
            if (!push.succeeded()) {
                throw new PushFailedException("push failed: " + push.output());
            }

            // 10. Capture head SHA
            CommandResult rev = exec(containerId, "git rev-parse HEAD", WORKSPACE);
            String headSha = rev.succeeded() ? rev.output().trim() : null;

            return new Result(headSha, codeowners, changedFiles);

        } catch (BranchExistsException | PushFailedException e) {
            throw e;
        } catch (DockerException e) {
            throw new PushFailedException("Docker error during diff apply", e);
        } catch (RuntimeException e) {
            throw new PushFailedException("Unexpected error during diff apply", e);
        } finally {
            if (containerId != null) {
                try {
                    docker.removeContainerCmd(containerId).withForce(true).exec();
                } catch (DockerException e) {
                    log.warn("Failed to remove diff-applier container {}: {}", containerId, e.getMessage());
                }
            }
        }
    }

    private String readIfExists(String containerId, String path) {
        CommandResult r = exec(containerId, "[ -f " + sq(path) + " ] && cat " + sq(path) + " || echo '__MISSING__'", WORKSPACE);
        if (!r.succeeded()) return null;
        String out = r.output();
        return out.contains("__MISSING__") ? null : out;
    }

    private CommandResult exec(String containerId, String command, String workingDir) {
        try {
            ExecCreateCmdResponse exec = docker.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withWorkingDir(workingDir)
                    .withCmd("sh", "-c", command)
                    .exec();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            docker.execStartCmd(exec.getId())
                    .exec(new ExecStartResultCallback(out, err))
                    .awaitCompletion(EXEC_TIMEOUT, TimeUnit.SECONDS);
            Long exitCode = docker.inspectExecCmd(exec.getId()).exec().getExitCodeLong();
            String combined = out.toString(StandardCharsets.UTF_8) + err.toString(StandardCharsets.UTF_8);
            return new CommandResult(exitCode == null ? -1 : exitCode.intValue(), combined);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PushFailedException("Interrupted: " + command, e);
        }
    }

    private static String embedToken(String repoUrl, String token) {
        if (token == null || token.isBlank()) return repoUrl;
        if (!repoUrl.startsWith("https://")) return repoUrl;
        return "https://x-access-token:" + token + "@" + repoUrl.substring("https://".length());
    }

    private static String sq(String s) { return "'" + s.replace("'", "'\\''") + "'"; }

    record CommandResult(int exitCode, String output) {
        boolean succeeded() { return exitCode == 0; }
    }
}
