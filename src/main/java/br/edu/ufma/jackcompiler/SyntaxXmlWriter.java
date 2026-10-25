package br.edu.ufma.jackcompiler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class SyntaxXmlWriter {

    private final StringBuilder xml;
    private final Deque<String> openTags;

    public SyntaxXmlWriter() {
        this.xml = new StringBuilder();
        this.openTags = new ArrayDeque<>();
    }

    public void openTag(String tag) {
        validateTagName(tag);

        appendIndentation(openTags.size());

        xml.append('<')
                .append(tag)
                .append(">\n");

        openTags.push(tag);
    }

    public void closeTag(String tag) {
        validateTagName(tag);

        if (openTags.isEmpty()) {
            throw new IllegalStateException(
                    "Não existe uma tag aberta para fechar: " + tag);
        }

        String currentTag = openTags.peek();

        if (!currentTag.equals(tag)) {
            throw new IllegalStateException(
                    "Tag incompatível: esperado fechar </"
                            + currentTag
                            + ">, mas foi solicitado </"
                            + tag
                            + ">.");
        }

        openTags.pop();

        appendIndentation(openTags.size());

        xml.append("</")
                .append(tag)
                .append(">\n");
    }

    public void writeToken(Token token) {
        Objects.requireNonNull(
                token,
                "O token não pode ser nulo.");

        if (token.getType() == TokenType.EOF) {
            throw new IllegalArgumentException(
                    "O token EOF não deve aparecer na saída XML.");
        }

        String tag = TokenXmlWriter.tagFor(token.getType());

        appendIndentation(openTags.size());

        xml.append('<')
                .append(tag)
                .append("> ")
                .append(TokenXmlWriter.escape(token.getLexeme()))
                .append(" </")
                .append(tag)
                .append(">\n");
    }

    public String toXml() {
        if (!openTags.isEmpty()) {
            throw new IllegalStateException(
                    "Não é possível finalizar o XML. "
                            + "Ainda existe uma tag aberta: "
                            + openTags.peek());
        }

        return xml.toString();
    }

    private void appendIndentation(int depth) {
        xml.append("  ".repeat(depth));
    }

    private void validateTagName(String tag) {
        Objects.requireNonNull(
                tag,
                "O nome da tag não pode ser nulo.");

        if (!tag.matches("[A-Za-z_][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException(
                    "Nome de tag XML inválido: " + tag);
        }
    }
}