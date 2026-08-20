package com.agenticdev.sdlc.github.pipeline;

import com.agenticdev.sdlc.github.domain.CodeownersResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses GitHub-style CODEOWNERS files and resolves reviewers for changed paths.
 * Implements GitHub's semantics: last-match-wins, last non-comment matching line
 * for a given path determines its owners.
 */
@Component
public class DefaultCodeownersResolver implements CodeownersResolver {

    @Override
    public List<String> reviewersFor(String codeownersContent, List<String> changedFiles) {
        if (codeownersContent == null || codeownersContent.isBlank() || changedFiles == null) {
            return List.of();
        }
        List<Rule> rules = parse(codeownersContent);
        Set<String> result = new LinkedHashSet<>();
        for (String file : changedFiles) {
            if (file == null || file.isBlank()) continue;
            String path = file.startsWith("/") ? file : "/" + file;
            Rule lastMatch = null;
            for (Rule r : rules) {
                if (r.matches(path)) lastMatch = r;
            }
            if (lastMatch != null) result.addAll(lastMatch.owners());
        }
        return new ArrayList<>(result);
    }

    private List<Rule> parse(String content) {
        List<Rule> rules = new ArrayList<>();
        for (String raw : content.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            // strip trailing inline comments
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash).trim();
            String[] tokens = line.split("\\s+");
            if (tokens.length < 2) continue;
            String pattern = tokens[0];
            List<String> owners = Arrays.stream(tokens).skip(1)
                    .filter(s -> s.startsWith("@") || s.contains("@"))
                    .map(s -> s.startsWith("@") ? s.substring(1) : s)
                    .toList();
            if (owners.isEmpty()) continue;
            rules.add(new Rule(pattern, compile(pattern), owners));
        }
        return rules;
    }

    /**
     * GitHub CODEOWNERS glob semantics:
     *  - leading / anchors at repo root
     *  - trailing / means directory (any file beneath)
     *  - ** matches any number of path segments
     *  - *  matches any chars except /
     */
    static Pattern compile(String glob) {
        boolean anchored = glob.startsWith("/");
        boolean dirOnly = glob.endsWith("/");
        String g = glob;
        if (anchored) g = g.substring(1);
        if (dirOnly) g = g.substring(0, g.length() - 1);

        StringBuilder re = new StringBuilder(anchored ? "^/" : "^(.*/)?");
        int i = 0;
        while (i < g.length()) {
            char c = g.charAt(i);
            if (c == '*' && i + 1 < g.length() && g.charAt(i + 1) == '*') {
                // ** => any number of path segments (including zero)
                re.append(".*");
                i += 2;
                if (i < g.length() && g.charAt(i) == '/') i++;
            } else if (c == '*') {
                re.append("[^/]*");
                i++;
            } else if (c == '?') {
                re.append("[^/]");
                i++;
            } else if (".()+|^$".indexOf(c) >= 0) {
                re.append('\\').append(c);
                i++;
            } else if (c == '/') {
                re.append('/');
                i++;
            } else {
                re.append(c);
                i++;
            }
        }
        if (dirOnly) {
            re.append("(/.*)?$");
        } else {
            re.append('$');
        }
        return Pattern.compile(re.toString());
    }

    record Rule(String glob, Pattern regex, List<String> owners) {
        boolean matches(String path) {
            return regex.matcher(path).matches();
        }
    }
}
