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
}