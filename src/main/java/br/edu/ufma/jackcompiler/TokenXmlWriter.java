package br.edu.ufma.jackcompiler;

import java.util.Objects;

/** Serializa tokens para o formato de validação do nand2tetris. */
public final class TokenXmlWriter {

    private TokenXmlWriter() {
    }

    public static String toXml(JackTokenizer tokenizer) {
        Objects.requireNonNull(
                tokenizer,
                "O tokenizer não pode ser nulo.");

        StringBuilder xml = new StringBuilder("<tokens>\n");

        for (Token token = tokenizer.nextToken();
                token.getType() != TokenType.EOF;
                token = tokenizer.nextToken()) {
            String tag = tagFor(token.getType());

            xml.append('<')
                    .append(tag)
                    .append("> ")
                    .append(escape(token.getLexeme()))
                    .append(" </")
                    .append(tag)
                    .append(">\n");
        }

        return xml.append("</tokens>\n").toString();
    }

    static String tagFor(TokenType type) {
        return switch (type) {
            case LPAREN, RPAREN, LBRACE, RBRACE,
                    LBRACKET, RBRACKET, COMMA,
                    SEMICOLON, DOT, PLUS, MINUS,
                    ASTERISK, SLASH, AND, OR, NOT,
                    LT, GT, EQ -> "symbol";

            case NUMBER -> "integerConstant";
            case STRING -> "stringConstant";
            case IDENT -> "identifier";

            case CLASS, CONSTRUCTOR, FUNCTION, METHOD,
                    FIELD, STATIC, VAR, INT, CHAR, BOOLEAN,
                    VOID, TRUE, FALSE, NULL, THIS, LET,
                    DO, IF, ELSE, WHILE, RETURN -> "keyword";

            case EOF -> throw new IllegalArgumentException(
                    "EOF não deve ser serializado.");
        };
    }

    static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
