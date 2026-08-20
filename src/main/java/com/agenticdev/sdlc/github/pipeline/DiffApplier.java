package com.agenticdev.sdlc.github.pipeline;

public interface DiffApplier {

    /**
     * Clones repo, creates headBranch, applies unified diff, commits (signed-off) with
     * the given message and author, pushes to origin, returns the head SHA and the
     * raw CODEOWNERS content (if present).
     */
    Result apply(String repoUrl, String baseRef, String headBranch,
                 String diff, String commitMessage,
                 String authorName, String authorEmail,
                 String pushToken);

    record Result(String headSha, String codeownersContent, java.util.List<String> changedFiles) {}
}
