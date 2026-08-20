package com.agenticdev.sdlc.github.pipeline;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCodeownersResolverTest {

    private final DefaultCodeownersResolver resolver = new DefaultCodeownersResolver();

    @Test
    void missingContent_returnsEmpty() {
        assertThat(resolver.reviewersFor(null, List.of("a.txt"))).isEmpty();
        assertThat(resolver.reviewersFor("", List.of("a.txt"))).isEmpty();
    }

    @Test
    void globalWildcard_matchesAllFiles() {
        String content = "* @alice @bob\n";
        List<String> out = resolver.reviewersFor(content, List.of("src/a.java", "README.md"));
        assertThat(out).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void lastMatchWins() {
        String content = """
                *           @default-owner
                *.js        @js-team
                /docs/*     @docs-team
                """;
        List<String> out = resolver.reviewersFor(content, List.of("app.js"));
        assertThat(out).containsExactly("js-team");
    }

    @Test
    void directoryPattern_matchesAnyFileBeneath() {
        String content = "/api/ @api-team\n";
        List<String> out = resolver.reviewersFor(content, List.of("api/v1/users.java"));
        assertThat(out).containsExactly("api-team");
    }

    @Test
    void doubleStarWildcard() {
        String content = "**/test/** @qa\n";
        List<String> out = resolver.reviewersFor(content, List.of("src/main/test/foo.java"));
        assertThat(out).containsExactly("qa");
    }

    @Test
    void commentsAndBlanksIgnored() {
        String content = """
                # ignored

                *.md @docs
                """;
        List<String> out = resolver.reviewersFor(content, List.of("README.md"));
        assertThat(out).containsExactly("docs");
    }

    @Test
    void multipleFilesAggregateOwners() {
        String content = """
                *.java @java-team
                *.js   @js-team
                """;
        List<String> out = resolver.reviewersFor(content, List.of("a.java", "b.js"));
        assertThat(out).containsExactlyInAnyOrder("java-team", "js-team");
    }
}
