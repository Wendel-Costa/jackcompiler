package br.edu.ufma.jackcompiler;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

class JackParserCallTest {

    private static final String PREFIX = "class Main {\n    function int main() {\n        ";
    private static final String BODY = "/class/subroutineDec/subroutineBody/statements";

    @ParameterizedTest(name = "{0} {1}, argumentos: {2}")
    @MethodSource("validCalls")
    void shouldPreserveCallShapeAndArgumentOrderInBothContexts(
            String context, String name, List<Integer> values) throws Exception {
        String arguments = values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
        Document xml = readXml(parseBody(context + " " + name + "(" + arguments + ");"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String call = callPath(context);
        String list = call + "/expressionList";
        boolean qualified = name.contains(".");

        assertEquals(qualified ? "2" : "1", xpath.evaluate("count(" + call + "/identifier)", xml));
        assertEquals(qualified ? name.substring(0, name.indexOf('.')) : name,
                xpath.evaluate("normalize-space(" + call + "/identifier[1])", xml));
        assertEquals("run", xpath.evaluate("normalize-space(" + call + "/identifier[last()])", xml));
        assertEquals("1", xpath.evaluate("count(" + list + ")", xml));
        assertEquals(Integer.toString(values.size()), xpath.evaluate("count(" + list + "/expression)", xml));
        assertEquals(Integer.toString(Math.max(0, values.size() - 1)),
                xpath.evaluate("count(" + list + "/symbol)", xml));
        assertEquals(Integer.toString(Math.max(0, values.size() * 2 - 1)),
                xpath.evaluate("count(" + list + "/*)", xml));
        for (int i = 0; i < values.size(); i++) {
            String argument = list + "/expression[" + (i + 1) + "]";
            assertEquals("expression", xpath.evaluate("name(" + list + "/*[" + (i * 2 + 1) + "])", xml));
            assertEquals("1", xpath.evaluate("count(" + argument + "/term)", xml));
            assertEquals(values.get(i).toString(), xpath.evaluate(
                    "normalize-space(" + argument + "/term/integerConstant)", xml));
            if (i > 0) {
                assertEquals(",", xpath.evaluate("normalize-space(" + list + "/symbol[" + i + "])", xml));
            }
        }
        assertEquals("0", xpath.evaluate("count(" + call + "/term)", xml));
        assertEquals("0", xpath.evaluate("count(//subroutineCall)", xml));
        if (context.equals("do")) {
            assertEquals(";", xpath.evaluate("normalize-space(" + call + "/symbol[last()])", xml));
        } else {
            assertEquals(")", xpath.evaluate("normalize-space(" + call + "/symbol[last()])", xml));
        }
    }

    private static Stream<Arguments> validCalls() {
        return Stream.of("do", "return").flatMap(context ->
                Stream.of("run", "game.run", "Game.run").flatMap(name ->
                        Stream.of(List.<Integer>of(), List.of(7), List.of(1, 2, 3))
                                .map(values -> Arguments.of(context, name, values))));
    }

    @Test
    void shouldKeepCommasAndEmptyListsAtTheirOwnNestingLevel() throws Exception {
        Document xml = readXml(parseBody("do f(g(1, h(2, 3)), k(), 4); do done();"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String outer = BODY + "/doStatement[1]/expressionList";
        String middle = outer + "/expression[1]/term/expressionList";
        String inner = middle + "/expression[2]/term/expressionList";

        assertEquals("3", xpath.evaluate("count(" + outer + "/expression)", xml));
        assertEquals("2", xpath.evaluate("count(" + outer + "/symbol)", xml));
        assertEquals("2", xpath.evaluate("count(" + middle + "/expression)", xml));
        assertEquals("1", xpath.evaluate("count(" + middle + "/symbol)", xml));
        assertEquals("2", xpath.evaluate("count(" + inner + "/expression)", xml));
        assertEquals("1", xpath.evaluate("count(" + inner + "/symbol)", xml));
        assertEquals("3", xpath.evaluate("normalize-space(" + inner + "/expression[2]/term/integerConstant)", xml));
        assertEquals("1", xpath.evaluate("count(" + outer + "/expression[2]/term/expressionList)", xml));
        assertEquals("0", xpath.evaluate("count(" + outer + "/expression[2]/term/expressionList/*)", xml));
        assertEquals("4", xpath.evaluate("normalize-space(" + outer + "/expression[3]/term/integerConstant)", xml));
        assertEquals("done", xpath.evaluate("normalize-space(" + BODY + "/doStatement[2]/identifier)", xml));
    }

    @Test
    void shouldNotTreatStringPunctuationAsArgumentDelimiters() throws Exception {
        Document xml = readXml(parseBody("return join(\"a,b\", \"()\", \"<&>\", x);"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String list = callPath("return") + "/expressionList";

        assertEquals("4", xpath.evaluate("count(" + list + "/expression)", xml));
        assertEquals("3", xpath.evaluate("count(" + list + "/symbol)", xml));
        assertEquals(" a,b ", xpath.evaluate("string(" + list + "/expression[1]/term/stringConstant)", xml));
        assertEquals(" () ", xpath.evaluate("string(" + list + "/expression[2]/term/stringConstant)", xml));
        assertEquals(" <&> ", xpath.evaluate("string(" + list + "/expression[3]/term/stringConstant)", xml));
        assertEquals("x", xpath.evaluate("normalize-space(" + list + "/expression[4]/term/identifier)", xml));
    }

    @Test
    void shouldParseCallsInArrayIndexesAndConditionsWithoutConsumingOuterDelimiters() throws Exception {
        Document xml = readXml(parseBody("""
                let a[index(1, 2)] = read();
                if (check(a[0], true)) { do finish(); }
                return next();
                """));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String index = BODY + "/letStatement/expression[1]/term";
        String condition = BODY + "/ifStatement/expression/term";

        assertEquals("index", xpath.evaluate("normalize-space(" + index + "/identifier)", xml));
        assertEquals("2", xpath.evaluate("count(" + index + "/expressionList/expression)", xml));
        assertEquals("read", xpath.evaluate(
                "normalize-space(" + BODY + "/letStatement/expression[2]/term/identifier)", xml));
        assertEquals("check", xpath.evaluate("normalize-space(" + condition + "/identifier)", xml));
        assertEquals("2", xpath.evaluate("count(" + condition + "/expressionList/expression)", xml));
        assertEquals("finish", xpath.evaluate(
                "normalize-space(" + BODY + "/ifStatement/statements/doStatement/identifier)", xml));
        assertEquals("next", xpath.evaluate("normalize-space(" + callPath("return") + "/identifier)", xml));
    }

    @ParameterizedTest(name = "{0} inválido: {1}")
    @MethodSource("invalidCalls")
    void shouldRejectMalformedArgumentListsInBothContexts(
            String context, String call, String expected, int offset) {
        JackSyntaxException exception = assertThrows(JackSyntaxException.class,
                () -> parseBody(context + " " + call + ";"));
        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
        assertEquals(3, exception.getLine());
        assertEquals((context.equals("do") ? 12 : 16) + offset, exception.getColumn());
    }

    private static Stream<Arguments> invalidCalls() {
        return Stream.of("do", "return").flatMap(context -> Stream.of(
                Arguments.of(context, "f(,1)", "RPAREN", 2),
                Arguments.of(context, "f(1,,2)", "termo válido", 4),
                Arguments.of(context, "f(1 2)", "RPAREN", 4),
                Arguments.of(context, "f(1,)", "termo válido", 4),
                Arguments.of(context, "f((1,2))", "RPAREN", 4),
                Arguments.of(context, "f(1;2)", "RPAREN", 3),
                Arguments.of(context, "f(1 g())", "RPAREN", 4),
                Arguments.of(context, "obj.(1)", "IDENT", 4),
                Arguments.of(context, "obj.f(1", "RPAREN", 7),
                Arguments.of(context, "f()()", "SEMICOLON", 3)));
    }

    @ParameterizedTest
    @MethodSource("unfinishedCalls")
    void shouldReportEndOfFileWithinArgumentLists(String call, String expected, int column) {
        JackParser parser = new JackParser(new JackTokenizer(PREFIX + "return " + call));
        JackSyntaxException exception = assertThrows(JackSyntaxException.class, parser::parseClass);
        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
        assertTrue(exception.getMessage().contains("fim do arquivo"));
        assertEquals(3, exception.getLine());
        assertEquals(column, exception.getColumn());
    }

    private static Stream<Arguments> unfinishedCalls() {
        return Stream.of(
                Arguments.of("f(1,", "termo válido", 20),
                Arguments.of("f(g(1)", "RPAREN", 22),
                Arguments.of("obj.", "IDENT", 20));
    }

    private String callPath(String context) {
        return BODY + (context.equals("do") ? "/doStatement" : "/returnStatement/expression/term");
    }

    private String parseBody(String body) {
        return new JackParser(new JackTokenizer(PREFIX + body + "\n    }\n}")).parseClass();
    }

    private Document readXml(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
    }
}
