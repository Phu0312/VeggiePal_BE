package com.veggiepal.ai.prompt;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PromptTemplate {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9]*)}}", Pattern.MULTILINE);

    private PromptTemplate() { }

    public static void validate(PromptKey key, String content) {
        if (content == null || content.isBlank() || content.length() > 30_000) {
            throw new IllegalArgumentException("Prompt must contain 1 to 30000 characters");
        }
        Matcher matcher = VARIABLE.matcher(content);
        Set<String> found = new HashSet<>();
        while (matcher.find()) found.add(matcher.group(1));
        if (!found.equals(key.variables())) {
            throw new IllegalArgumentException("Required variables: " + key.variables());
        }
        if (content.contains("{{") && content.replaceAll(VARIABLE.pattern(), "").contains("{{")) {
            throw new IllegalArgumentException("Malformed template variable");
        }
    }

    public static String render(PromptKey key, String content, Map<String, ?> variables) {
        validate(key, content);
        if (!variables.keySet().equals(key.variables())) {
            throw new IllegalArgumentException("Expected variables: " + key.variables());
        }
        String rendered = content;
        for (String variable : key.variables()) {
            rendered = rendered.replace("{{" + variable + "}}", String.valueOf(variables.get(variable)));
        }
        return rendered;
    }
}
