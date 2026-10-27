package br.edu.ufma.jackcompiler;

import java.io.StringReader;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

class JackParserConstantTest {

    private static final String PREFIX = "class Main {\n    function void main() {\n        let value = ";
    private static final String TERM = "/class/subroutineDec/subroutineBody/statements/letStatement/expression/term";

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "null", "this"})
    void shouldWriteEveryKeywordConstantAsKeywordInsideTerm(String keyword) throws Exception {
        assertSingleConstant(parseLiteral(keyword), "keyword", keyword);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "7", "32767"})
    void shouldWriteIntegerConstantsIncludingBothLimits(String literal) throws Exception {
        assertSingleConstant(parseLiteral(literal), "integerConstant", literal);
    }

    @ParameterizedTest
    @ValueSource(strings = {"32768", "2147483648", "999999999999999999999999999999999999"})
    void shouldPropagateOutOfRangeIntegerErrorFromScanner(String literal) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> parseLiteral(literal));

        assertTrue(exception.getMessage().contains("0 a 32767"));
        assertTrue(exception.getMessage().contains("linha 3"));
        assertTrue(exception.getMessage().contains(literal));
    }

    @ParameterizedTest
    @ValueSource(strings = {"class", "void", "int", "return"})
    void shouldRejectKeywordsThatAreNotConstants(String keyword) {
        JackSyntaxException exception = assertThrows(
                JackSyntaxException.class, () -> parseLiteral(keyword));

        assertTrue(exception.getMessage().contains("termo válido"));
        assertEquals(3, exception.getLine());
        assertEquals(21, exception.getColumn());
    }

    @ParameterizedTest
    @ValueSource(strings = {"trueValue", "falseValue", "nullValue", "thisValue"})
    void shouldKeepIdentifiersStartingWithConstantNamesAsIdentifiers(String name) throws Exception {
        assertSingleConstant(parseLiteral(name), "identifier", name);
    }

    @ParameterizedTest(name = "string preservada: {index}")
    @MethodSource("stringContents")
    void shouldPreserveStringContentThroughTokenAndSyntaxXml(String value, String escaped) throws Exception {
        String literal = "\"" + value + "\"";
        String tokenXml = TokenXmlWriter.toXml(new JackTokenizer(literal));
        String syntaxXml = parseLiteral(literal);
        String expectedToken = "<stringConstant> " + escaped + " </stringConstant>";

        assertEquals("<tokens>\n" + expectedToken + "\n</tokens>\n", tokenXml);
        assertTrue(syntaxXml.contains(expectedToken), syntaxXml);
        assertSingleConstant(syntaxXml, "stringConstant", value);

        Document tokens = readXml(tokenXml);
        assertEquals(1, tokens.getElementsByTagName("stringConstant").getLength());
        assertEquals(" " + value + " ", tokens.getElementsByTagName("stringConstant").item(0).getTextContent());
    }

    private static Stream<Arguments> stringContents() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("  ", "  "),
                Arguments.of("  Jack  compiler  ", "  Jack  compiler  "),
                Arguments.of("a < b & c > d", "a &lt; b &amp; c &gt; d"),
                Arguments.of("</stringConstant><injected/>", "&lt;/stringConstant&gt;&lt;injected/&gt;"),
                Arguments.of("&lt; &amp; &quot; &#65;", "&amp;lt; &amp;amp; &amp;quot; &amp;#65;"),
                Arguments.of("// not a comment /* neither */", "// not a comment /* neither */"),
                Arguments.of("ação \\ caminho", "ação \\ caminho"));
    }

    @Test
    void shouldPropagateUnterminatedStringErrorAtEndOfFile() {
        JackParser parser = new JackParser(new JackTokenizer(PREFIX + "\"unfinished"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, parser::parseClass);

        assertTrue(exception.getMessage().contains("string não terminada"));
        assertTrue(exception.getMessage().contains("linha 3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r", "\r\n"})
    void shouldRejectLineBreakInsideStringThroughParser(String lineBreak) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> parseLiteral("\"first" + lineBreak + "second\""));

        assertTrue(exception.getMessage().contains("Quebra de linha em constante string"));
        assertTrue(exception.getMessage().contains("linha 3"));
    }

    private String parseLiteral(String literal) {
        return new JackParser(new JackTokenizer(PREFIX + literal + ";\n    }\n}")).parseClass();
    }

    private void assertSingleConstant(String xml, String tag, String value) throws Exception {
        Document document = readXml(xml);
        Element term = (Element) XPathFactory.newInstance().newXPath()
                .evaluate(TERM, document, XPathConstants.NODE);
        assertNotNull(term);
        assertEquals(1, document.getElementsByTagName("term").getLength());
        assertEquals(1, term.getElementsByTagName("*").getLength());

        Element constant = (Element) term.getElementsByTagName(tag).item(0);
        assertNotNull(constant, "Tag esperada: " + tag);
        assertSame(term, constant.getParentNode());
        assertEquals(" " + value + " ", constant.getTextContent());
        assertEquals(1, constant.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, constant.getFirstChild().getNodeType());
    }

    private Document readXml(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
    }
}
