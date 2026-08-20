package com.agenticdev.sdlc.coding.executor;

import com.agenticdev.sdlc.coding.domain.CodeExecutor;
import com.agenticdev.sdlc.coding.domain.CodingBudget;
import com.agenticdev.sdlc.coding.domain.ContainerException;
import com.agenticdev.sdlc.coding.domain.RepoCloneException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provisions a disposable Docker container, runs tool calls via {@code docker exec},
 * captures the resulting diff, and destroys the container on teardown.
 *
 * <p>Container is created with {@code --network none}, configurable memory/CPU caps,
 * and an ephemeral filesystem.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DockerCodeExecutor implements CodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerCodeExecutor.class);
    private static final String WORKSPACE = "/workspace";
    private static final long EXEC_TIMEOUT_SECONDS = 300;

    private final DockerClient docker;
    private String containerId;
    private String image = "eclipse-temurin:21-jdk";

    public DockerCodeExecutor(DockerClient docker) {
        this.docker = docker;
    }

    /** Override default container image per run. */
    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public void provision(String repoUrl, String baseRef, CodingBudget budget) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new RepoCloneException("", "repoUrl is required");
        }
        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withNetworkMode("none")
                    .withMemory(parseMemory(budget.containerMemory()))
                    .withCpuQuota((long) (budget.containerCpu() * 100_000))
                    .withCpuPeriod(100_000L)
                    .withAutoRemove(false);

            CreateContainerResponse created = docker.createContainerCmd(image)
                    .withHostConfig(hostConfig)
                    .withWorkingDir(WORKSPACE)
                    .withCmd("sleep", String.valueOf(budget.maxDuration().toSeconds() + 60))
                    .withTty(false)
                    .exec();
            this.containerId = created.getId();
            docker.startContainerCmd(containerId).exec();
            log.info("Provisioned container {} (image={})", containerId, image);

            // Enable network temporarily for the clone, then detach.
            docker.connectToNetworkCmd()
                    .withContainerId(containerId)
                    .withNetworkId("bridge")
                    .exec();

            CommandResult mkdir = execRaw("mkdir -p " + WORKSPACE, "/");
            if (!mkdir.succeeded()) {
                throw new ContainerException("Failed to create workspace: " + mkdir.output());
            }

            String cloneCmd = baseRef == null || baseRef.isBlank()
                    ? String.format("git clone --depth 1 %s %s", shellQuote(repoUrl), WORKSPACE)
                    : String.format("git clone --depth 1 --branch %s %s %s",
                            shellQuote(baseRef), shellQuote(repoUrl), WORKSPACE);
            CommandResult clone = execRaw(cloneCmd, "/");
            if (!clone.succeeded()) {
                throw new RepoCloneException(repoUrl, clone.output());
            }

            docker.disconnectFromNetworkCmd()
                    .withContainerId(containerId)
                    .withNetworkId("bridge")
                    .exec();
            log.info("Cloned {} @ {} and disconnected network", repoUrl, baseRef);
        } catch (RepoCloneException | ContainerException e) {
            throw e;
        } catch (DockerException e) {
            throw new ContainerException("Docker error during provision", e);
        } catch (RuntimeException e) {
            throw new ContainerException("Unexpected error during provision", e);
        }
    }

    @Override
    public String readFile(String path) {
        ensureProvisioned();
        CommandResult r = execRaw("cat " + shellQuote(path), WORKSPACE);
        if (!r.succeeded()) {
            return "ERROR: " + r.output();
        }
        return r.output();
    }

    @Override
    public void writeFile(String path, String content) {
        ensureProvisioned();
        // Ensure parent dir exists
        String parent = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
        if (!parent.isBlank()) {
            execRaw("mkdir -p " + shellQuote(parent), WORKSPACE);
        }
        // Use a here-doc-free approach: base64 decode to avoid escaping nightmares.
        String b64 = java.util.Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        String cmd = "echo " + b64 + " | base64 -d > " + shellQuote(path);
        CommandResult r = execRaw(cmd, WORKSPACE);
        if (!r.succeeded()) {
            throw new ContainerException("writeFile failed for " + path + ": " + r.output());
        }
    }

    @Override
    public List<String> listFiles(String path, boolean recursive) {
        ensureProvisioned();
        String cmd = recursive
                ? "find " + shellQuote(path) + " -type f -not -path '*/\\.*'"
                : "ls -1 " + shellQuote(path);
        CommandResult r = execRaw(cmd, WORKSPACE);
        if (!r.succeeded()) {
            return List.of();
        }
        return Arrays.stream(r.output().split("\\n"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    @Override
    public CommandResult runCommand(String command) {
        ensureProvisioned();
        return execRaw(command, WORKSPACE);
    }

    @Override
    public String getDiff() {
        ensureProvisioned();
        execRaw("git add -A", WORKSPACE);
        CommandResult r = execRaw("git diff --cached", WORKSPACE);
        return r.succeeded() ? r.output() : "";
    }

    @Override
    public int countChangedFiles() {
        ensureProvisioned();
        CommandResult r = execRaw("git diff --cached --name-only | wc -l", WORKSPACE);
        try {
            return r.succeeded() ? Integer.parseInt(r.output().trim()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void destroy() {
        if (containerId == null) return;
        try {
            docker.removeContainerCmd(containerId).withForce(true).exec();
            log.info("Destroyed container {}", containerId);
        } catch (DockerException e) {
            log.warn("Failed to remove container {}: {}", containerId, e.getMessage());
        } finally {
            containerId = null;
        }
    }

    // --- internals ---

    private void ensureProvisioned() {
        if (containerId == null) {
            throw new ContainerException("Executor not provisioned; call provision() first");
        }
    }

    private CommandResult execRaw(String command, String workingDir) {
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
                    .awaitCompletion(EXEC_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Long exitCode = docker.inspectExecCmd(exec.getId()).exec().getExitCodeLong();
            String combined = out.toString(StandardCharsets.UTF_8) + err.toString(StandardCharsets.UTF_8);
            return new CommandResult(exitCode == null ? -1 : exitCode.intValue(), combined);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ContainerException("Interrupted while executing command: " + command, e);
        } catch (DockerException e) {
            throw new ContainerException("Docker error executing command: " + command, e);
        }
    }

    private static long parseMemory(String memory) {
        if (memory == null || memory.isBlank()) return 2L * 1024 * 1024 * 1024;
        String m = memory.trim().toLowerCase();
        long mul = 1;
        if (m.endsWith("g")) { mul = 1024L * 1024 * 1024; m = m.substring(0, m.length() - 1); }
        else if (m.endsWith("m")) { mul = 1024L * 1024; m = m.substring(0, m.length() - 1); }
        else if (m.endsWith("k")) { mul = 1024L; m = m.substring(0, m.length() - 1); }
        return Long.parseLong(m.trim()) * mul;
    }

    private static String shellQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
