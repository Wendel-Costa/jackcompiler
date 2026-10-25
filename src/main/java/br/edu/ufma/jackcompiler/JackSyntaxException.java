package br.edu.ufma.jackcompiler;

import java.util.Objects;

public final class JackSyntaxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public JackSyntaxException(
            Token foundToken,
            TokenType expectedType) {

        this(
                foundToken,
                Objects.requireNonNull(
                        expectedType,
                        "O tipo esperado não pode ser nulo."
                ).name()
        );
    }

    public JackSyntaxException(
            Token foundToken,
            String expectedDescription) {

        super(createMessage(foundToken, expectedDescription));

        this.line = foundToken.getLine();
        this.column = foundToken.getColumn();
    }

    private static String createMessage(
            Token foundToken,
            String expectedDescription) {

        Objects.requireNonNull(
                foundToken,
                "O token encontrado não pode ser nulo."
        );

        Objects.requireNonNull(
                expectedDescription,
                "A descrição do esperado não pode ser nula."
        );

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
                + expectedDescription
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