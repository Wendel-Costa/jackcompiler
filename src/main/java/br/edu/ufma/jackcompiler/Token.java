package br.edu.ufma.jackcompiler;

import java.util.Objects;

public final class Token {

    private final TokenType type;
    private final String lexeme;
    private final int line;
    private final int column;

    public Token(TokenType type, String lexeme, int line) {
        this(type, lexeme, line, 1);
    }

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = Objects.requireNonNull(
                type,
                "O tipo do token não pode ser nulo.");

        this.lexeme = Objects.requireNonNull(
                lexeme,
                "O lexema do token não pode ser nulo.");

        if (line < 1) {
            throw new IllegalArgumentException(
                    "A linha do token tem que ser maior ou igual a 1.");
        }

        if (column < 1) {
            throw new IllegalArgumentException(
                    "A coluna do token tem que ser maior ou igual a 1.");
        }

        this.line = line;
        this.column = column;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}