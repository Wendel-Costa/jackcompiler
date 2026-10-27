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

class JackParserComplexTermTest {

    private static final String PREFIX = "class Main {\n    function int value() {\n        ";
    private static final String BODY = "/class/subroutineDec/subroutineBody/statements";
    private static final String TERM = BODY + "/returnStatement/expression/term";

    @Test
    void shouldPreserveEveryLevelOfNestedParentheses() throws Exception {
        Document xml = expressionXml("(((x)))");
        XPath xpath = XPathFactory.newInstance().newXPath();
        String term = TERM;

        assertEquals("4", xpath.evaluate("count(//expression)", xml));
        assertEquals("4", xpath.evaluate("count(//term)", xml));
        for (int depth = 0; depth < 3; depth++) {
            assertEquals("3", xpath.evaluate("count(" + term + "/*)", xml));
            assertEquals("(", xpath.evaluate("normalize-space(" + term + "/*[1])", xml));
            assertEquals("expression", xpath.evaluate("name(" + term + "/*[2])", xml));
            assertEquals(")", xpath.evaluate("normalize-space(" + term + "/*[3])", xml));
            term += "/expression/term";
        }
        assertEquals("1", xpath.evaluate("count(" + term + "/*)", xml));
        assertEquals("x", xpath.evaluate("normalize-space(" + term + "/identifier)", xml));
    }

    @ParameterizedTest
    @CsvSource({"--x, -, -", "~~x, ~, ~", "~-x, ~, -", "-~x, -, ~"})
    void shouldNestChainedUnaryOperatorsInSourceOrder(String source, String outer, String inner)
            throws Exception {
        Document xml = expressionXml(source);
        XPath xpath = XPathFactory.newInstance().newXPath();

        assertEquals("3", xpath.evaluate("count(//term)", xml));
        assertEquals("2", xpath.evaluate("count(" + TERM + "/*)", xml));
        assertEquals(outer, xpath.evaluate("normalize-space(" + TERM + "/symbol)", xml));
        assertEquals("2", xpath.evaluate("count(" + TERM + "/term/*)", xml));
        assertEquals(inner, xpath.evaluate("normalize-space(" + TERM + "/term/symbol)", xml));
        assertEquals("x", xpath.evaluate("normalize-space(" + TERM + "/term/term/identifier)", xml));
    }

    @ParameterizedTest
    @CsvSource({"a[0], 0", "a[i + 1], i", "a[b[i]], b"})
    void shouldKeepArrayIndexExpressionInsideItsTerm(String source, String firstIndexValue)
            throws Exception {
        Document xml = expressionXml(source);
        XPath xpath = XPathFactory.newInstance().newXPath();

        assertEquals("4", xpath.evaluate("count(" + TERM + "/*)", xml));
        assertEquals("a", xpath.evaluate("normalize-space(" + TERM + "/identifier)", xml));
        assertEquals("[", xpath.evaluate("normalize-space(" + TERM + "/*[2])", xml));
        assertEquals("expression", xpath.evaluate("name(" + TERM + "/*[3])", xml));
        assertEquals("]", xpath.evaluate("normalize-space(" + TERM + "/*[4])", xml));
        assertEquals(firstIndexValue, xpath.evaluate(
                "normalize-space(" + TERM + "/expression/term[1]/*[1])", xml));
    }

    @Test
    void shouldCombineUnaryGroupingAndNestedArrayReads() throws Exception {
        Document xml = expressionXml("-(a[b[i + 1]] + (x * 2))");
        XPath xpath = XPathFactory.newInstance().newXPath();
        String sum = TERM + "/term/expression";
        String innerIndex = sum + "/term[1]/expression/term/expression";
        String product = sum + "/term[2]/expression";

        assertEquals("-", xpath.evaluate("normalize-space(" + TERM + "/symbol)", xml));
        assertEquals("(", xpath.evaluate("normalize-space(" + TERM + "/term/symbol[1])", xml));
        assertEquals(")", xpath.evaluate("normalize-space(" + TERM + "/term/symbol[2])", xml));
        assertEquals("3", xpath.evaluate("count(" + sum + "/*)", xml));
        assertEquals("+", xpath.evaluate("normalize-space(" + sum + "/symbol)", xml));
        assertEquals("a", xpath.evaluate("normalize-space(" + sum + "/term[1]/identifier)", xml));
        assertEquals("b", xpath.evaluate(
                "normalize-space(" + sum + "/term[1]/expression/term/identifier)", xml));
        assertEquals("i", xpath.evaluate("normalize-space(" + innerIndex + "/term[1]/identifier)", xml));
        assertEquals("+", xpath.evaluate("normalize-space(" + innerIndex + "/symbol)", xml));
        assertEquals("1", xpath.evaluate("normalize-space(" + innerIndex + "/term[2]/integerConstant)", xml));
        assertEquals("x", xpath.evaluate("normalize-space(" + product + "/term[1]/identifier)", xml));
        assertEquals("*", xpath.evaluate("normalize-space(" + product + "/symbol)", xml));
        assertEquals("2", xpath.evaluate("normalize-space(" + product + "/term[2]/integerConstant)", xml));
    }

    @Test
    void shouldSeparateArrayAssignmentIndexFromValueAndFollowingStatement() throws Exception {
        Document xml = readXml(parseBody("let a[i + 1] = -(b[j] + 2); let x = a[i + 1];"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String assignment = BODY + "/letStatement[1]";
        String value = assignment + "/expression[2]/term";

        assertEquals("2", xpath.evaluate("count(" + BODY + "/letStatement)", xml));
        assertEquals("8", xpath.evaluate("count(" + assignment + "/*)", xml));
        assertEquals("a", xpath.evaluate("normalize-space(" + assignment + "/identifier)", xml));
        assertEquals("[", xpath.evaluate("normalize-space(" + assignment + "/*[3])", xml));
        assertEquals("expression", xpath.evaluate("name(" + assignment + "/*[4])", xml));
        assertEquals("]", xpath.evaluate("normalize-space(" + assignment + "/*[5])", xml));
        assertEquals("=", xpath.evaluate("normalize-space(" + assignment + "/*[6])", xml));
        assertEquals("expression", xpath.evaluate("name(" + assignment + "/*[7])", xml));
        assertEquals(";", xpath.evaluate("normalize-space(" + assignment + "/*[8])", xml));
        assertEquals("i", xpath.evaluate(
                "normalize-space(" + assignment + "/expression[1]/term[1]/identifier)", xml));
        assertEquals("-", xpath.evaluate("normalize-space(" + value + "/symbol)", xml));
        assertEquals("b", xpath.evaluate(
                "normalize-space(" + value + "/term/expression/term[1]/identifier)", xml));
        assertEquals("j", xpath.evaluate(
                "normalize-space(" + value + "/term/expression/term[1]/expression/term/identifier)", xml));
        assertEquals("x", xpath.evaluate("normalize-space(" + BODY + "/letStatement[2]/identifier)", xml));
        assertEquals("a", xpath.evaluate(
                "normalize-space(" + BODY + "/letStatement[2]/expression/term/identifier)", xml));
    }

    @ParameterizedTest(name = "termo inválido: {0}")
    @CsvSource(delimiter = ':', value = {
            "() : termo válido : 17",
            "(x : RPAREN : 18",
            "(x] : RPAREN : 18",
            "a[] : termo válido : 18",
            "a[i : RBRACKET : 19",
            "a[i) : RBRACKET : 19",
            "a[i][j] : SEMICOLON : 20",
            "- : termo válido : 17",
            "~ : termo válido : 17",
            "-( ) : termo válido : 19",
            "a[~] : termo válido : 19",
            "a[i+] : termo válido : 20"
    })
    void shouldRejectMalformedComplexTermWithExactPosition(String expression, String expected, int column) {
        assertSyntaxError(() -> parseBody("return " + expression + ";"), expected, column);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ':', value = {
            "let a[] = 1; : termo válido : 15",
            "let a[i = 1; : RBRACKET : 20",
            "let a[i] 1; : EQ : 18",
            "let a[i] = ; : termo válido : 20"
    })
    void shouldRejectMalformedArrayAssignment(String statement, String expected, int column) {
        assertSyntaxError(() -> parseBody(statement), expected, column);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ':', value = {
            "(x : RPAREN : 18", "a[i : RBRACKET : 19", "- : termo válido : 17"
    })
    void shouldReportEndOfFileInsideComplexTerm(String expression, String expected, int column) {
        JackSyntaxException exception = assertSyntaxError(
                () -> new JackParser(new JackTokenizer(PREFIX + "return " + expression)).parseClass(),
                expected, column);
        assertTrue(exception.getMessage().contains("fim do arquivo"));
    }

    private JackSyntaxException assertSyntaxError(
            org.junit.jupiter.api.function.Executable action, String expected, int column) {
        JackSyntaxException exception = assertThrows(JackSyntaxException.class, action);
        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
        assertEquals(3, exception.getLine());
        assertEquals(column, exception.getColumn());
        return exception;
    }

    private String parseBody(String body) {
        return new JackParser(new JackTokenizer(PREFIX + body + "\n    }\n}")).parseClass();
    }

    private Document expressionXml(String expression) throws Exception {
        return readXml(parseBody("return " + expression + ";"));
    }

    private Document readXml(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
    }
}
