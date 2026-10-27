package br.edu.ufma.jackcompiler;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

class JackParserDoTest {

    private static final String STATEMENTS = "/class/subroutineDec/subroutineBody/statements";

    @Test
    void shouldWriteCompleteDoStatementXmlWithoutWrappingCallInTerm() {
        String xml = parseBody("do Output.printInt(1);");

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
                        <doStatement>
                          <keyword> do </keyword>
                          <identifier> Output </identifier>
                          <symbol> . </symbol>
                          <identifier> printInt </identifier>
                          <symbol> ( </symbol>
                          <expressionList>
                            <expression>
                              <term>
                                <integerConstant> 1 </integerConstant>
                              </term>
                            </expression>
                          </expressionList>
                          <symbol> ) </symbol>
                          <symbol> ; </symbol>
                        </doStatement>
                      </statements>
                      <symbol> } </symbol>
                    </subroutineBody>
                  </subroutineDec>
                  <symbol> } </symbol>
                </class>
                """;

        assertEquals(expected, xml);
    }

    @ParameterizedTest
    @CsvSource({
            "run(), run, 1",
            "game.run(), game, 2",
            "Game.run(), Game, 2"
    })
    void shouldParseCallsWithEmptyArgumentList(String call, String firstName, int identifiers)
            throws Exception {
        Document xml = readXml(parseBody("do " + call + ";"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String statement = STATEMENTS + "/doStatement";

        assertEquals(firstName, xpath.evaluate(
                "normalize-space(" + statement + "/identifier[1])", xml));
        assertEquals("run", xpath.evaluate(
                "normalize-space(" + statement + "/identifier[last()])", xml));
        assertEquals(Integer.toString(identifiers), xpath.evaluate(
                "count(" + statement + "/identifier)", xml));
        assertEquals("1", xpath.evaluate("count(" + statement + "/expressionList)", xml));
        assertEquals("0", xpath.evaluate("count(" + statement + "/expressionList/*)", xml));
        assertEquals("0", xpath.evaluate("count(" + statement + "/term)", xml));
    }

    @Test
    void shouldKeepNestedCallsAndArgumentExpressionsInsideExpressionList() throws Exception {
        Document xml = readXml(parseBody(
                "do draw(Math.multiply(2, 3), read(), a[i + 1], ~(x < 10));"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String arguments = STATEMENTS + "/doStatement/expressionList";

        assertEquals("4", xpath.evaluate("count(" + arguments + "/expression)", xml));
        assertEquals("3", xpath.evaluate("count(" + arguments + "/symbol)", xml));
        assertEquals("2", xpath.evaluate(
                "count(" + arguments + "/expression[1]/term/expressionList/expression)", xml));
        assertEquals("1", xpath.evaluate(
                "count(" + arguments + "/expression[2]/term/expressionList)", xml));
        assertEquals("0", xpath.evaluate(
                "count(" + arguments + "/expression[2]/term/expressionList/expression)", xml));
        assertEquals("i", xpath.evaluate(
                "normalize-space(" + arguments + "/expression[3]/term/expression/term[1]/identifier)",
                xml));
        assertEquals("~", xpath.evaluate(
                "normalize-space(" + arguments + "/expression[4]/term/symbol)", xml));
        assertEquals("0", xpath.evaluate("count(" + STATEMENTS + "/doStatement/term)", xml));
    }

    @Test
    void shouldParseDoInsideConditionalsAndLoopsAndContinueAfterCalls() throws Exception {
        Document xml = readXml(parseBody("""
                do start();
                while (true) {
                    if (ready) { do game.run(); } else { do wait(); }
                    do tick();
                }
                do stop();
                let x = 1;
                """));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String loopBody = STATEMENTS + "/whileStatement/statements";

        assertEquals("4", xpath.evaluate("count(" + STATEMENTS + "/*)", xml));
        assertEquals("doStatement", xpath.evaluate("name(" + STATEMENTS + "/*[1])", xml));
        assertEquals("whileStatement", xpath.evaluate("name(" + STATEMENTS + "/*[2])", xml));
        assertEquals("doStatement", xpath.evaluate("name(" + STATEMENTS + "/*[3])", xml));
        assertEquals("letStatement", xpath.evaluate("name(" + STATEMENTS + "/*[4])", xml));
        assertEquals("2", xpath.evaluate("count(" + loopBody + "/ifStatement/statements/doStatement)", xml));
        assertEquals("tick", xpath.evaluate("normalize-space(" + loopBody + "/doStatement/identifier)", xml));
    }

    @ParameterizedTest
    @ValueSource(strings = {"read()", "Memory.peek(0)"})
    void shouldPreserveCallsAsTermsInsideAssignments(String call) throws Exception {
        Document xml = readXml(parseBody("let x = " + call + " + 1; do finish();"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = STATEMENTS + "/letStatement/expression";

        assertEquals("2", xpath.evaluate("count(" + expression + "/term)", xml));
        assertEquals("1", xpath.evaluate("count(" + expression + "/term[1]/expressionList)", xml));
        assertEquals("+", xpath.evaluate("normalize-space(" + expression + "/symbol)", xml));
        assertEquals("1", xpath.evaluate("count(" + STATEMENTS + "/doStatement)", xml));
    }

    @ParameterizedTest(name = "do inválido: {0}")
    @CsvSource(delimiter = '|', value = {
            "do ; | IDENT | 12",
            "do 1(); | IDENT | 12",
            "do run; | LPAREN | 15",
            "do Output.(); | IDENT | 19",
            "do Output.printInt; | LPAREN | 27",
            "do run(1; | RPAREN | 17",
            "do run() next(); | SEMICOLON | 18",
            "do run(1,); | termo válido | 18",
            "do a[0]; | LPAREN | 13",
            "do a.b.c(); | LPAREN | 15"
    })
    void shouldRejectMalformedDoWithExactErrorPosition(String body, String expected, int column) {
        JackSyntaxException exception = assertThrows(
                JackSyntaxException.class, () -> parseBody(body));

        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
        assertEquals(3, exception.getLine());
        assertEquals(column, exception.getColumn());
    }

    @Test
    void shouldRejectEndOfFileBeforeCallCloses() {
        JackParser parser = new JackParser(new JackTokenizer(
                "class Main {\n    function void main() {\n        do run("));

        JackSyntaxException exception = assertThrows(JackSyntaxException.class, parser::parseClass);

        assertTrue(exception.getMessage().contains("RPAREN"));
        assertTrue(exception.getMessage().contains("fim do arquivo"));
        assertEquals(3, exception.getLine());
        assertEquals(16, exception.getColumn());
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
