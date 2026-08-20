package com.agenticdev.sdlc.github.domain;

import java.util.List;

public interface CodeownersResolver {

    /**
     * Given a CODEOWNERS file content and a list of changed file paths (relative to repo root),
     * return the deduped list of GitHub usernames/teams that should review (last-match-wins per
     * GitHub's CODEOWNERS spec). Returns empty list if content is null/empty.
     */
    List<String> reviewersFor(String codeownersContent, List<String> changedFiles);
}
