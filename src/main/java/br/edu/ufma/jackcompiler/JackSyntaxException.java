package br.edu.ufma.jackcompiler;

import java.util.Objects;

public final class JackSyntaxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public JackSyntaxException(
            Token foundToken,
            TokenType expectedType) {

        super(createMessage(foundToken, expectedType));

        this.line = foundToken.getLine();
        this.column = foundToken.getColumn();
    }

    private static String createMessage(
            Token foundToken,
            TokenType expectedType) {

        Objects.requireNonNull(
                foundToken,
                "O token encontrado não pode ser nulo.");

        Objects.requireNonNull(
                expectedType,
                "O tipo esperado não pode ser nulo.");

        String description;

        if (foundToken.getType() == TokenType.EOF) {
            description = "fim do arquivo (EOF)";
        } else {
            description = "'"
                    + foundToken.getLexeme()
                    + "' ("
                    + foundToken.getType()
                    + ")";
        }

        return "Erro sintático na linha "
                + foundToken.getLine()
                + ", coluna "
                + foundToken.getColumn()
                + ": esperado "
                + expectedType
                + ", encontrado "
                + description
                + ".";
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}