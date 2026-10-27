package br.edu.ufma.jackcompiler;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

class JackParserExpressionTest {

    @ParameterizedTest(name = "operador binário: {0}")
    @CsvSource(delimiter = ':', value = {
            "+ : +", "- : -", "* : *", "/ : /", "& : &amp;",
            "| : |", "< : &lt;", "> : &gt;", "= : ="
    })
    void shouldWriteEveryBinaryOperatorWithCorrectXmlEscape(String operator, String escaped) {
        String expected = """
                <class>
                  <keyword> class </keyword>
                  <identifier> Main </identifier>
                  <symbol> { </symbol>
                  <subroutineDec>
                    <keyword> function </keyword>
                    <keyword> int </keyword>
                    <identifier> value </identifier>
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
                              <integerConstant> 6 </integerConstant>
                            </term>
                            <symbol> %s </symbol>
                            <term>
                              <integerConstant> 2 </integerConstant>
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
                """.formatted(escaped);

        assertEquals(expected, parseExpression("6 " + operator + " 2"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "2 + 3 * 4; term:2,symbol:+,term:3,symbol:*,term:4",
            "8 - 3 - 1; term:8,symbol:-,term:3,symbol:-,term:1",
            "24 / 4 / 2; term:24,symbol:/,term:4,symbol:/,term:2"
    })
    void shouldKeepOperatorChainsInSourceOrderWithoutImplicitGrouping(
            String source, String expectedSequence) throws Exception {
        Element expression = expressionElement(source);

        assertEquals(List.of(expectedSequence.split(",")), childLabels(expression));
        assertEquals(0, expression.getElementsByTagName("expression").getLength());
        for (Element child : children(expression)) {
            if (child.getTagName().equals("term")) {
                assertEquals(1, children(child).size());
                assertEquals("integerConstant", children(child).get(0).getTagName());
            }
        }
    }

    @Test
    void shouldAcceptSingleTermWithoutOperator() throws Exception {
        Element expression = expressionElement("x");

        assertEquals(List.of("term:x"), childLabels(expression));
        assertEquals(List.of("identifier:x"), childLabels(children(expression).get(0)));
    }

    @Test
    void shouldRepresentExplicitParenthesesAsNestedExpression() throws Exception {
        Element expression = expressionElement("2 * (3 + 4)");
        List<Element> outer = children(expression);

        assertEquals(3, outer.size());
        assertEquals("term:2", label(outer.get(0)));
        assertEquals("symbol:*", label(outer.get(1)));
        assertEquals("term", outer.get(2).getTagName());

        List<Element> grouped = children(outer.get(2));
        assertEquals(3, grouped.size());
        assertEquals("symbol:(", label(grouped.get(0)));
        assertEquals("expression", grouped.get(1).getTagName());
        assertEquals(List.of("term:3", "symbol:+", "term:4"), childLabels(grouped.get(1)));
        assertEquals("symbol:)", label(grouped.get(2)));
    }

    @Test
    void shouldDistinguishBinaryMinusFromUnaryOperatorsWithoutSpaces() throws Exception {
        Element expression = expressionElement("a--b*~c");
        List<Element> parts = children(expression);

        assertEquals(5, parts.size());
        assertEquals("term:a", label(parts.get(0)));
        assertEquals("symbol:-", label(parts.get(1)));
        assertEquals("term", parts.get(2).getTagName());
        assertEquals(List.of("symbol:-", "term:b"), childLabels(parts.get(2)));
        assertEquals("symbol:*", label(parts.get(3)));
        assertEquals("term", parts.get(4).getTagName());
        assertEquals(List.of("symbol:~", "term:c"), childLabels(parts.get(4)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+", "-", "*", "/", "&", "|", "<", ">", "="})
    void shouldRequireRightOperandAfterEveryBinaryOperator(String operator) {
        JackSyntaxException exception = assertThrows(
                JackSyntaxException.class, () -> parseExpression("1 " + operator));

        assertTrue(exception.getMessage().contains("termo válido"));
        assertEquals(3, exception.getLine());
        assertEquals(19, exception.getColumn());
    }

    @ParameterizedTest(name = "expressão inválida: {0}")
    @CsvSource(delimiter = ':', value = {
            "1 + * 2 : termo válido : 20",
            "1 && 2 : termo válido : 19",
            "1 || 2 : termo válido : 19",
            "1 <= 2 : termo válido : 19",
            "1 >= 2 : termo válido : 19",
            "1 == 2 : termo válido : 19",
            "1 ~ 2 : SEMICOLON : 18",
            "1 2 : SEMICOLON : 18",
            "+1 : SEMICOLON : 16"
    })
    void shouldRejectInvalidOperatorSequencesWithExactPosition(
            String source, String expected, int column) {
        JackSyntaxException exception = assertThrows(
                JackSyntaxException.class, () -> parseExpression(source));

        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
        assertEquals(3, exception.getLine());
        assertEquals(column, exception.getColumn());
    }

    @Test
    void shouldReportEndOfFileAfterOperator() {
        JackParser parser = new JackParser(new JackTokenizer(
                "class Main {\n    function int value() {\n        return 1 +"));

        JackSyntaxException exception = assertThrows(JackSyntaxException.class, parser::parseClass);

        assertTrue(exception.getMessage().contains("termo válido"));
        assertTrue(exception.getMessage().contains("fim do arquivo"));
        assertEquals(3, exception.getLine());
        assertEquals(19, exception.getColumn());
    }

    private String parseExpression(String expression) {
        return new JackParser(new JackTokenizer(
                "class Main {\n    function int value() {\n        return "
                        + expression + ";\n    }\n}"))
                .parseClass();
    }

    private Element expressionElement(String source) throws Exception {
        return (Element) DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(parseExpression(source))))
                .getElementsByTagName("expression").item(0);
    }

    private List<Element> children(Element element) {
        List<Element> result = new ArrayList<>();
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement) {
                result.add(childElement);
            }
        }
        return result;
    }

    private String label(Element element) {
        return element.getTagName() + ":" + element.getTextContent().strip();
    }

    private List<String> childLabels(Element element) {
        return children(element).stream().map(this::label).toList();
    }
}
