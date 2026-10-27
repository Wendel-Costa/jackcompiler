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

class JackParserReturnTest {

    private static final String STATEMENTS = "/class/subroutineDec/subroutineBody/statements";

    @Test
    void shouldWriteEmptyReturnWithoutExpressionTag() {
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
                        <returnStatement>
                          <keyword> return </keyword>
                          <symbol> ; </symbol>
                        </returnStatement>
                      </statements>
                      <symbol> } </symbol>
                    </subroutineBody>
                  </subroutineDec>
                  <symbol> } </symbol>
                </class>
                """;

        assertEquals(expected, parseBody("void", "return;"));
    }

    @Test
    void shouldWriteReturnWithBinaryExpression() {
        String expected = """
                <class>
                  <keyword> class </keyword>
                  <identifier> Main </identifier>
                  <symbol> { </symbol>
                  <subroutineDec>
                    <keyword> function </keyword>
                    <keyword> int </keyword>
                    <identifier> main </identifier>
                    <symbol> ( </symbol>
                    <parameterList>
                    </parameterList>
                    <symbol> ) </symbol>
                    <subroutineBody>
                      <symbol> { </symbol>
                      <statements>
                        <returnStatement>
                          <keyword> return </keyword>
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
                        </returnStatement>
                      </statements>
                      <symbol> } </symbol>
                    </subroutineBody>
                  </subroutineDec>
                  <symbol> } </symbol>
                </class>
                """;

        assertEquals(expected, parseBody("int", "return x + 1;"));
    }

    @Test
    void shouldParseReturnWithCallArrayAccessAndUnaryTerm() throws Exception {
        Document xml = readXml(parseBody("int", "return Math.abs(a[i + 1]) + -x;"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = STATEMENTS + "/returnStatement/expression";
        String argument = expression + "/term[1]/expressionList/expression";

        assertEquals("1", xpath.evaluate("count(" + expression + ")", xml));
        assertEquals("2", xpath.evaluate("count(" + expression + "/term)", xml));
        assertEquals("Math", xpath.evaluate("normalize-space(" + expression + "/term[1]/identifier[1])", xml));
        assertEquals("abs", xpath.evaluate("normalize-space(" + expression + "/term[1]/identifier[2])", xml));
        assertEquals("1", xpath.evaluate("count(" + argument + ")", xml));
        assertEquals("a", xpath.evaluate("normalize-space(" + argument + "/term/identifier)", xml));
        assertEquals("i", xpath.evaluate(
                "normalize-space(" + argument + "/term/expression/term[1]/identifier)", xml));
        assertEquals("-", xpath.evaluate("normalize-space(" + expression + "/term[2]/symbol)", xml));
        assertEquals("x", xpath.evaluate("normalize-space(" + expression + "/term[2]/term/identifier)", xml));
    }

    @Test
    void shouldParseReturnsInNestedBlocksAndContinueWithNextSubroutine() throws Exception {
        String source = """
                class Main {
                    function int choose(int x) {
                        let x = x + 1;
                        while (x < 10) {
                            if (x = 5) { return x; } else { do Output.printInt(x); }
                            let x = x + 1;
                        }
                        return 0;
                    }
                    function void finish() {
                        do Output.println();
                        return;
                    }
                }
                """;
        Document xml = readXml(new JackParser(new JackTokenizer(source)).parseClass());
        XPath xpath = XPathFactory.newInstance().newXPath();
        String firstBody = "/class/subroutineDec[1]/subroutineBody/statements";
        String secondBody = "/class/subroutineDec[2]/subroutineBody/statements";
        String nestedReturn = firstBody + "/whileStatement/statements/ifStatement/statements[1]/returnStatement";

        assertEquals("2", xpath.evaluate("count(/class/subroutineDec)", xml));
        assertEquals("3", xpath.evaluate("count(//returnStatement)", xml));
        assertEquals("x", xpath.evaluate("normalize-space(" + nestedReturn + "/expression/term/identifier)", xml));
        assertEquals("returnStatement", xpath.evaluate("name(" + firstBody + "/*[3])", xml));
        assertEquals("0", xpath.evaluate(
                "normalize-space(" + firstBody + "/returnStatement/expression/term/integerConstant)", xml));
        assertEquals("doStatement", xpath.evaluate("name(" + secondBody + "/*[1])", xml));
        assertEquals("returnStatement", xpath.evaluate("name(" + secondBody + "/*[2])", xml));
        assertEquals("0", xpath.evaluate("count(" + secondBody + "/returnStatement/expression)", xml));
    }

    @ParameterizedTest(name = "return inválido: {0}")
    @CsvSource(delimiter = '|', value = {
            "return } | SEMICOLON | 16",
            "return x } | SEMICOLON | 18",
            "return x + ; | termo válido | 20",
            "return (x; | RPAREN | 18",
            "return foo(1,); | termo válido | 22",
            "return x, y; | SEMICOLON | 17",
            "return x y; | SEMICOLON | 18"
    })
    void shouldRejectMalformedReturnWithExactErrorPosition(String body, String expected, int column) {
        JackSyntaxException exception = assertThrows(
                JackSyntaxException.class, () -> parseBody("int", body));

        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
        assertEquals(3, exception.getLine());
        assertEquals(column, exception.getColumn());
    }

    @ParameterizedTest
    @CsvSource({"return, 15", "return x, 17"})
    void shouldRejectEndOfFileBeforeReturnSemicolon(String statement, int column) {
        JackParser parser = new JackParser(new JackTokenizer(
                "class Main {\n    function int main() {\n        " + statement));

        JackSyntaxException exception = assertThrows(JackSyntaxException.class, parser::parseClass);

        assertTrue(exception.getMessage().contains("SEMICOLON"));
        assertTrue(exception.getMessage().contains("fim do arquivo"));
        assertEquals(3, exception.getLine());
        assertEquals(column, exception.getColumn());
    }

    private String parseBody(String returnType, String body) {
        return new JackParser(new JackTokenizer(
                "class Main {\n    function " + returnType + " main() {\n        "
                        + body + "\n    }\n}"))
                .parseClass();
    }

    private Document readXml(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
    }
}
