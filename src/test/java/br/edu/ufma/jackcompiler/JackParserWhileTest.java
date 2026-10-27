package br.edu.ufma.jackcompiler;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

class JackParserWhileTest {

    @Test
    void shouldWriteCompleteWhileStatementXml() {
        String xml = parseBody("while (x < 10) { let x = x + 1; }");

        String expected = """
                <class>
                  <keyword> class </keyword>
                  <identifier> Main </identifier>
                  <symbol> { </symbol>
                  <subroutineDec>
                    <keyword> function </keyword>
                    <keyword> void </keyword>
                    <identifier> main </identifier>
                    <symbol> ( </symbol>
                    <parameterList>
                    </parameterList>
                    <symbol> ) </symbol>
                    <subroutineBody>
                      <symbol> { </symbol>
                      <statements>
                        <whileStatement>
                          <keyword> while </keyword>
                          <symbol> ( </symbol>
                          <expression>
                            <term>
                              <identifier> x </identifier>
                            </term>
                            <symbol> &lt; </symbol>
                            <term>
                              <integerConstant> 10 </integerConstant>
                            </term>
                          </expression>
                          <symbol> ) </symbol>
                          <symbol> { </symbol>
                          <statements>
                            <letStatement>
                              <keyword> let </keyword>
                              <identifier> x </identifier>
                              <symbol> = </symbol>
                              <expression>
                                <term>
                                  <identifier> x </identifier>
                                </term>
                                <symbol> + </symbol>
                                <term>
                                  <integerConstant> 1 </integerConstant>
                                </term>
                              </expression>
                              <symbol> ; </symbol>
                            </letStatement>
                          </statements>
                          <symbol> } </symbol>
                        </whileStatement>
                      </statements>
                      <symbol> } </symbol>
                    </subroutineBody>
                  </subroutineDec>
                  <symbol> } </symbol>
                </class>
                """;

        assertEquals(expected, xml);
    }

    @Test
    void shouldParseWhileWithEmptyBody() throws Exception {
        Document xml = readXml(parseBody("while (false) {}"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String loop = "/class/subroutineDec/subroutineBody/statements/whileStatement";

        assertEquals("false", xpath.evaluate(
                "normalize-space(" + loop + "/expression/term/keyword)", xml));
        assertEquals("1", xpath.evaluate("count(" + loop + "/statements)", xml));
        assertEquals("0", xpath.evaluate("count(" + loop + "/statements/*)", xml));
    }

    @Test
    void shouldPreserveNestedLoopsConditionalsAndFollowingStatements() throws Exception {
        Document xml = readXml(parseBody("""
                let x = 0;
                while (x < 10) {
                    while (x < 5) { let x = x + 1; }
                    if (x = 5) {
                        while (false) {}
                    } else {
                        let x = x + 2;
                    }
                }
                while (false) {}
                let x = 10;
                """));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String body = "/class/subroutineDec/subroutineBody/statements";
        String loopBody = body + "/whileStatement[1]/statements";

        assertEquals("4", xpath.evaluate("count(" + body + "/*)", xml));
        assertEquals("letStatement", xpath.evaluate("name(" + body + "/*[1])", xml));
        assertEquals("whileStatement", xpath.evaluate("name(" + body + "/*[2])", xml));
        assertEquals("whileStatement", xpath.evaluate("name(" + body + "/*[3])", xml));
        assertEquals("letStatement", xpath.evaluate("name(" + body + "/*[4])", xml));
        assertEquals("2", xpath.evaluate("count(" + loopBody + "/*)", xml));
        assertEquals("1", xpath.evaluate(
                "count(" + loopBody + "/whileStatement/statements/letStatement)", xml));
        assertEquals("1", xpath.evaluate(
                "count(" + loopBody + "/ifStatement/statements[1]/whileStatement)", xml));
        assertEquals("1", xpath.evaluate(
                "count(" + loopBody + "/ifStatement/statements[2]/letStatement)", xml));
        assertEquals("10", xpath.evaluate(
                "normalize-space(" + body + "/letStatement[2]/expression/term/integerConstant)", xml));
    }

    @ParameterizedTest(name = "while inválido: {0}")
    @CsvSource({
            "while true) {}, LPAREN, 15",
            "while () {}, termo válido, 16",
            "while (true {}, RPAREN, 21",
            "while (true) let x = 1;, LBRACE, 22",
            "while (true) { else {}, RBRACE, 24",
            "while (true) { let x = ; }, termo válido, 32"
    })
    void shouldRejectMalformedWhileWithExactErrorPosition(
            String body, String expected, int column) {
        JackSyntaxException exception = assertThrows(
                JackSyntaxException.class,
                () -> parseBody(body));

        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
        assertEquals(3, exception.getLine());
        assertEquals(column, exception.getColumn());
    }

    @Test
    void shouldRejectEndOfFileInsideWhileBody() {
        JackParser parser = new JackParser(new JackTokenizer(
                "class Main {\n    function void main() {\n        while (true) {"));

        JackSyntaxException exception = assertThrows(
                JackSyntaxException.class,
                parser::parseClass);

        assertTrue(exception.getMessage().contains("RBRACE"));
        assertTrue(exception.getMessage().contains("fim do arquivo"));
        assertEquals(3, exception.getLine());
        assertEquals(23, exception.getColumn());
    }

    private String parseBody(String body) {
        return new JackParser(new JackTokenizer(
                "class Main {\n    function void main() {\n        "
                        + body + "\n    }\n}"))
                .parseClass();
    }

    private Document readXml(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
    }
}
