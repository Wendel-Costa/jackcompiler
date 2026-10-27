package br.edu.ufma.jackcompiler;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

class SyntaxXmlWriterTest {

    @Test
    void shouldWriteNestedSyntaxElementsAndTokens() {
        SyntaxXmlWriter escritor = new SyntaxXmlWriter();

        escritor.openTag("class");

        escritor.writeToken(
                new Token(TokenType.CLASS, "class", 1, 1));

        escritor.openTag("expression");

        escritor.writeToken(
                new Token(TokenType.NUMBER, "2", 1, 7));

        escritor.closeTag("expression");
        escritor.closeTag("class");

        String esperado =
                "<class>\n"
                        + "  <keyword> class </keyword>\n"
                        + "  <expression>\n"
                        + "    <integerConstant> 2 </integerConstant>\n"
                        + "  </expression>\n"
                        + "</class>\n";

        assertEquals(esperado, escritor.toXml());
    }

    @Test
    void shouldEscapeXmlSpecialCharactersInTokenContent() {
        SyntaxXmlWriter escritor = new SyntaxXmlWriter();

        escritor.openTag("class");

        escritor.writeToken(
                new Token(
                        TokenType.STRING,
                        "a < b & c > \"d\"",
                        1,
                        1));

        escritor.closeTag("class");

        String esperado =
                "<class>\n"
                        + "  <stringConstant> "
                        + "a &lt; b &amp; c &gt; &quot;d&quot;"
                        + " </stringConstant>\n"
                        + "</class>\n";

        assertEquals(esperado, escritor.toXml());
    }

    @Test
    void shouldPreserveQuotesAndLiteralEntitiesWhenXmlIsReadBack() throws Exception {
        // Aspas internas são testadas no escritor: Jack não possui escape de aspas em strings.
        String value = "&quot; \"quoted\" <node> &amp; &";
        SyntaxXmlWriter writer = new SyntaxXmlWriter();
        writer.openTag("term");
        writer.writeToken(new Token(TokenType.STRING, value, 1, 1));
        writer.closeTag("term");

        String xml = writer.toXml();
        assertEquals("<term>\n"
                + "  <stringConstant> &amp;quot; &quot;quoted&quot; &lt;node&gt; &amp;amp; &amp; </stringConstant>\n"
                + "</term>\n", xml);

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        assertEquals(1, document.getElementsByTagName("stringConstant").getLength());
        assertEquals(0, document.getElementsByTagName("node").getLength());
        assertEquals(" " + value + " ", document.getElementsByTagName("stringConstant").item(0).getTextContent());
    }

    @Test
    void shouldRejectEndOfFileToken() {
        SyntaxXmlWriter escritor = new SyntaxXmlWriter();

        assertThrows(
                IllegalArgumentException.class,
                () -> escritor.writeToken(
                        new Token(TokenType.EOF, "EOF", 1, 1)));
    }

    @Test
    void shouldRejectMismatchedClosingTag() {
        SyntaxXmlWriter escritor = new SyntaxXmlWriter();

        escritor.openTag("class");

        assertThrows(
                IllegalStateException.class,
                () -> escritor.closeTag("expression"));
        escritor.closeTag("class");

        assertEquals(
                "<class>\n</class>\n",
                escritor.toXml());
    }

    @Test
    void shouldRejectUnclosedElementsWhenFinalizingXml() {
        SyntaxXmlWriter escritor = new SyntaxXmlWriter();

        escritor.openTag("class");

        assertThrows(
                IllegalStateException.class,
                escritor::toXml);

        escritor.closeTag("class");

        assertEquals(
                "<class>\n</class>\n",
                escritor.toXml());
    }

    @Test
    void shouldRejectClosingTagWhenNoElementIsOpen() {
        SyntaxXmlWriter escritor = new SyntaxXmlWriter();

        assertThrows(
                IllegalStateException.class,
                () -> escritor.closeTag("class"));
    }

    @Test
    void shouldRejectInvalidTagNames() {
        SyntaxXmlWriter escritor = new SyntaxXmlWriter();

        assertThrows(
                IllegalArgumentException.class,
                () -> escritor.openTag("class>"));

        assertThrows(
                IllegalArgumentException.class,
                () -> escritor.openTag("123class"));
    }
}
