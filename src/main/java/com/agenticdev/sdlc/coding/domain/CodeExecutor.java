package com.agenticdev.sdlc.coding.domain;

import java.util.List;

/**
 * Port for executing code operations against a sandboxed workspace.
 * Implementations (e.g. {@code DockerCodeExecutor}) hold the workspace lifecycle —
 * {@link #provision} must be called before any other method and {@link #destroy}
 * must be called exactly once (typically in a finally block) to release resources.
 */
public interface CodeExecutor {

    void provision(String repoUrl, String baseRef, CodingBudget budget);

    String readFile(String path);

    void writeFile(String path, String content);

    List<String> listFiles(String path, boolean recursive);

    CommandResult runCommand(String command);

    String getDiff();

    int countChangedFiles();

    void destroy();

    record CommandResult(int exitCode, String output) {
        public boolean succeeded() { return exitCode == 0; }
    }
}
