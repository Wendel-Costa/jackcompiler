
package br.edu.ufma.jackcompiler;

/** Erro semântico ou de geração de código encontrado durante a compilação Jack. */
public final class JackCompilationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public JackCompilationException(Token token, String message) {
        super("Erro de compilação na linha " + token.getLine()
                + ", coluna " + token.getColumn() + ": " + message);

        this.line = token.getLine();
        this.column = token.getColumn();
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
