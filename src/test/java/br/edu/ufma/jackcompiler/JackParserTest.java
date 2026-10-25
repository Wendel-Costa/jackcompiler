package br.edu.ufma.jackcompiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JackParserTest {

        @Test
        void shouldExposeFirstTokenWithoutConsumingIt() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class Main"));

                assertEquals(TokenType.CLASS, parser.peek().getType());
                assertEquals(TokenType.CLASS, parser.peek().getType());
        }

        @Test
        void shouldAdvanceToNextToken() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class Main"));

                Token classToken = parser.advance();

                assertEquals(TokenType.CLASS, classToken.getType());
                assertEquals("class", classToken.getLexeme());

                assertEquals(TokenType.IDENT, parser.peek().getType());
                assertEquals("Main", parser.peek().getLexeme());
        }

        @Test
        void shouldMatchAndConsumeExpectedToken() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class Main"));

                assertTrue(parser.match(TokenType.CLASS));

                assertEquals(TokenType.IDENT, parser.peek().getType());
                assertEquals("Main", parser.peek().getLexeme());
        }

        @Test
        void shouldNotConsumeTokenWhenMatchFails() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class Main"));

                assertFalse(parser.match(TokenType.LBRACE));

                assertEquals(TokenType.CLASS, parser.peek().getType());
        }

        @Test
        void shouldConsumeAndReturnExpectedToken() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class Main"));

                Token token = parser.consume(TokenType.CLASS);

                assertEquals(TokenType.CLASS, token.getType());
                assertEquals("class", token.getLexeme());
                assertEquals(TokenType.IDENT, parser.peek().getType());
        }

        @Test
        void shouldReportUnexpectedTokenWithLineAndColumn() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class Main"));

                parser.advance();

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                () -> parser.consume(TokenType.LBRACE));

                assertEquals(1, exception.getLine());
                assertEquals(7, exception.getColumn());

                assertTrue(exception.getMessage().contains("linha 1, coluna 7"));
                assertTrue(exception.getMessage().contains("LBRACE"));
                assertTrue(exception.getMessage().contains("Main"));
        }

        @Test
        void shouldKeepEndOfFileAsCurrentToken() {
                JackParser parser = new JackParser(
                                new JackTokenizer(""));

                Token firstEof = parser.advance();

                assertEquals(TokenType.EOF, firstEof.getType());
                assertSame(firstEof, parser.peek());
                assertSame(firstEof, parser.advance());
        }

        @Test
        void shouldReportUnexpectedEndOfFile() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class"));

                parser.advance();

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                () -> parser.consume(TokenType.LBRACE));

                assertTrue(exception.getMessage().contains("fim do arquivo"));
                assertEquals(TokenType.EOF, parser.peek().getType());
        }

        @Test
        void shouldParseAnEmptyClass() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class Main {}"));

                String xml = parser.parseClass();

                String expected = "<class>\n"
                                + "  <keyword> class </keyword>\n"
                                + "  <identifier> Main </identifier>\n"
                                + "  <symbol> { </symbol>\n"
                                + "  <symbol> } </symbol>\n"
                                + "</class>\n";

                assertEquals(expected, xml);
        }

        @Test
        void shouldParseStaticAndFieldDeclarations() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { "
                                                                + "static int contador; "
                                                                + "field boolean ativo; "
                                                                + "}"));

                String xml = parser.parseClass();

                assertTrue(xml.contains("<classVarDec>\n"));
                assertTrue(xml.contains("<keyword> static </keyword>"));
                assertTrue(xml.contains("<keyword> field </keyword>"));
                assertTrue(xml.contains("<keyword> int </keyword>"));
                assertTrue(xml.contains("<keyword> boolean </keyword>"));

                assertEquals(
                                2,
                                xml.split("<classVarDec>", -1).length - 1);

                assertEquals(
                                2,
                                xml.split("</classVarDec>", -1).length - 1);
        }

        @Test
        void shouldParseClassTypeAndMultipleVariableNames() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Square { "
                                                                + "field Point superior, inferior; "
                                                                + "}"));

                String xml = parser.parseClass();

                assertTrue(xml.contains("<identifier> Point </identifier>"));
                assertTrue(xml.contains("<identifier> superior </identifier>"));
                assertTrue(xml.contains("<identifier> inferior </identifier>"));
                assertTrue(xml.contains("<symbol> , </symbol>"));
        }

        @Test
        void shouldRejectVoidAsClassVariableType() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { field void valor; }"));

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                parser::parseClass);

                assertTrue(
                                exception.getMessage().contains(
                                                "tipo int, char, boolean ou nome de classe"));

                assertEquals(1, exception.getLine());
                assertTrue(exception.getColumn() > 0);
        }

        @Test
        void shouldRejectClassDeclarationWithoutOpeningBrace() {
                JackParser parser = new JackParser(
                                new JackTokenizer("class Main"));

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                parser::parseClass);

                assertTrue(exception.getMessage().contains("LBRACE"));
                assertTrue(exception.getMessage().contains("fim do arquivo"));
        }

        @Test
        void shouldRejectClassVariableWithoutSemicolon() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { static int contador }"));

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                parser::parseClass);

                assertTrue(exception.getMessage().contains("SEMICOLON"));
                assertEquals(1, exception.getLine());
        }

        @Test
        void shouldParseFunctionWithEmptyParameterList() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { function void main() {} }"));

                String xml = parser.parseClass();

                assertTrue(xml.contains(
                                "<subroutineDec>\n"));

                assertTrue(xml.contains(
                                "<keyword> function </keyword>"));

                assertTrue(xml.contains(
                                "<keyword> void </keyword>"));

                assertTrue(xml.contains(
                                "<identifier> main </identifier>"));

                assertTrue(xml.contains(
                                "<parameterList>\n"
                                                + "    </parameterList>"));

                assertTrue(xml.contains(
                                "<subroutineBody>\n"));
        }

        @Test
        void shouldParseConstructorWithClassReturnType() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Square { constructor Square new() {} }"));

                String xml = parser.parseClass();

                assertTrue(xml.contains(
                                "<keyword> constructor </keyword>"));

                assertTrue(xml.contains(
                                "<identifier> Square </identifier>"));

                assertTrue(xml.contains(
                                "<identifier> new </identifier>"));

                assertEquals(
                                1,
                                xml.split("<subroutineDec>", -1).length - 1);
        }

        @Test
        void shouldParseMethodWithMultipleParameters() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { "
                                                                + "method void configurar("
                                                                + "int x, boolean ativo, Point ponto"
                                                                + ") {} "
                                                                + "}"));

                String xml = parser.parseClass();

                assertTrue(xml.contains(
                                "<keyword> method </keyword>"));

                assertTrue(xml.contains(
                                "<identifier> configurar </identifier>"));

                assertTrue(xml.contains(
                                "<keyword> int </keyword>"));

                assertTrue(xml.contains(
                                "<keyword> boolean </keyword>"));

                assertTrue(xml.contains(
                                "<identifier> Point </identifier>"));

                assertTrue(xml.contains(
                                "<identifier> ponto </identifier>"));

                assertEquals(
                                1,
                                xml.split("<parameterList>", -1).length - 1);

                assertEquals(
                                2,
                                xml.split("<symbol> , </symbol>", -1).length - 1);
        }

        @Test
        void shouldRejectVoidAsParameterType() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { function void executar(void valor) {} }"));

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                parser::parseClass);

                assertTrue(
                                exception.getMessage().contains("RPAREN"));

                assertEquals(1, exception.getLine());
                assertTrue(exception.getColumn() > 0);
        }

        @Test
        void shouldRejectSubroutineWithoutOpeningParenthesis() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { function void executar {} }"));

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                parser::parseClass);

                assertTrue(
                                exception.getMessage().contains("LPAREN"));
        }

        @Test
        void shouldRejectSubroutineWithoutClosingParenthesis() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { function void executar( {} }"));

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                parser::parseClass);

                assertTrue(
                                exception.getMessage().contains("RPAREN"));
        }

        @Test
        void shouldParseSubroutineBodyWithLocalVariables() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { "
                                                                + "function void executar() { "
                                                                + "var int x; "
                                                                + "} "
                                                                + "}"));

                String xml = parser.parseClass();

                assertTrue(xml.contains("<subroutineBody>"));
                assertTrue(xml.contains("<varDec>"));

                assertTrue(
                                xml.contains("<keyword> var </keyword>"));

                assertTrue(
                                xml.contains("<keyword> int </keyword>"));

                assertTrue(
                                xml.contains("<identifier> x </identifier>"));

                assertTrue(
                                xml.contains("</varDec>"));
        }

        @Test
        void shouldParseMultipleLocalVariableDeclarations() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { "
                                                                + "function void executar() { "
                                                                + "var int x, y; "
                                                                + "var boolean ativo; "
                                                                + "var Point ponto; "
                                                                + "} "
                                                                + "}"));

                String xml = parser.parseClass();

                assertEquals(
                                3,
                                xml.split("<varDec>", -1).length - 1);

                assertEquals(
                                3,
                                xml.split("</varDec>", -1).length - 1);

                assertTrue(
                                xml.contains("<identifier> x </identifier>"));

                assertTrue(
                                xml.contains("<identifier> y </identifier>"));

                assertTrue(
                                xml.contains("<identifier> ativo </identifier>"));

                assertTrue(
                                xml.contains("<identifier> ponto </identifier>"));

                assertTrue(
                                xml.contains("<identifier> Point </identifier>"));
        }

        @Test
        void shouldRejectVoidAsLocalVariableType() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { "
                                                                + "function void executar() { "
                                                                + "var void valor; "
                                                                + "} "
                                                                + "}"));

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                parser::parseClass);

                assertTrue(
                                exception.getMessage().contains(
                                                "tipo int, char, boolean ou nome de classe"));

                assertEquals(1, exception.getLine());
                assertTrue(exception.getColumn() > 0);
        }

        @Test
        void shouldRejectLocalVariableWithoutSemicolon() {
                JackParser parser = new JackParser(
                                new JackTokenizer(
                                                "class Main { "
                                                                + "function void executar() { "
                                                                + "var int valor "
                                                                + "} "
                                                                + "}"));

                JackSyntaxException exception = assertThrows(
                                JackSyntaxException.class,
                                parser::parseClass);

                assertTrue(
                                exception.getMessage().contains("SEMICOLON"));

                assertEquals(1, exception.getLine());
                assertTrue(exception.getColumn() > 0);
        }
}