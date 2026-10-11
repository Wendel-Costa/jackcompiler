package br.edu.ufma.jackcompiler;

import java.util.Objects;

public final class JackTokenizer {

    private final String input;
    private int current;
    private int line;

    public JackTokenizer(String input) {
        this.input = Objects.requireNonNull(
            input,
            "O código-fonte não pode ser nulo."
        );

        this.current = 0;
        this.line = 1;
    }

    boolean isAtEnd() {
        return current >= input.length();
    }

    char peek() {
        if (isAtEnd()) {
            return '\0';
        }

        return input.charAt(current);
    }

    char peekNext() {
        int next = current + 1;

        if (next >= input.length()) {
            return '\0';
        }

        return input.charAt(next);
    }

    char advance() {
        if (isAtEnd()) {
            return '\0';
        }

        char character = input.charAt(current);
        current++;

        if (character == '\n') {
            line++;
        }

        return character;
    }

    int currentLine() {
        return line;
    }
}
