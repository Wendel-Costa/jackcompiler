package br.edu.ufma.jackcompiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JackTokenizerTest {

    @Test
    void shouldRecognizeEmptyInput() {
        JackTokenizer tokenizer = new JackTokenizer("");

        assertTrue(tokenizer.isAtEnd());
        assertEquals('\0', tokenizer.peek());
        assertEquals('\0', tokenizer.peekNext());
    }

    @Test
    void shouldReadCharactersInOrder() {
        JackTokenizer tokenizer = new JackTokenizer("Jack");

        assertEquals('J', tokenizer.peek());
        assertEquals('a', tokenizer.peekNext());

        assertEquals('J', tokenizer.advance());
        assertEquals('a', tokenizer.peek());

        assertEquals('a', tokenizer.advance());
        assertEquals('c', tokenizer.peek());
    }

    @Test
    void shouldCountLineBreaks() {
        JackTokenizer tokenizer = new JackTokenizer("a\nb\n");

        assertEquals(1, tokenizer.currentLine());

        assertEquals('a', tokenizer.advance());
        assertEquals(1, tokenizer.currentLine());

        assertEquals('\n', tokenizer.advance());
        assertEquals(2, tokenizer.currentLine());

        assertEquals('b', tokenizer.advance());
        assertEquals(2, tokenizer.currentLine());

        assertEquals('\n', tokenizer.advance());
        assertEquals(3, tokenizer.currentLine());
    }

    @Test
    void shouldRecognizeEndOfInput() {
        JackTokenizer tokenizer = new JackTokenizer("x");

        assertFalse(tokenizer.isAtEnd());

        assertEquals('x', tokenizer.advance());

        assertTrue(tokenizer.isAtEnd());
    }

    @Test
    void shouldNotAdvancePastEndOfInput() {
        JackTokenizer tokenizer = new JackTokenizer("x");

        tokenizer.advance();

        assertEquals('\0', tokenizer.advance());
        assertTrue(tokenizer.isAtEnd());
        assertEquals(1, tokenizer.currentLine());
    }

    @Test
    void shouldRejectNullInput() {
        assertThrows(
            NullPointerException.class,
            () -> new JackTokenizer(null)
        );
    }
}
