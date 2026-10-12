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
                () -> new JackTokenizer(null));
    }

    @Test
    void shouldRecognizeAllKeywords() {
        String source = "class constructor function method field "
                + "static var int char boolean void true false null "
                + "this let do if else while return";

        TokenType[] expected = {
                TokenType.CLASS,
                TokenType.CONSTRUCTOR,
                TokenType.FUNCTION,
                TokenType.METHOD,
                TokenType.FIELD,
                TokenType.STATIC,
                TokenType.VAR,
                TokenType.INT,
                TokenType.CHAR,
                TokenType.BOOLEAN,
                TokenType.VOID,
                TokenType.TRUE,
                TokenType.FALSE,
                TokenType.NULL,
                TokenType.THIS,
                TokenType.LET,
                TokenType.DO,
                TokenType.IF,
                TokenType.ELSE,
                TokenType.WHILE,
                TokenType.RETURN
        };

        JackTokenizer tokenizer = new JackTokenizer(source);

        for (TokenType expectedType : expected) {
            Token token = tokenizer.nextToken();

            assertEquals(expectedType, token.getType());
        }

        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldRecognizeIdentifiers() {
        JackTokenizer tokenizer = new JackTokenizer(
                "Main myVariable count2 value_2");

        String[] expectedLexemes = {
                "Main",
                "myVariable",
                "count2",
                "value_2"
        };

        for (String expectedLexeme : expectedLexemes) {
            Token token = tokenizer.nextToken();

            assertEquals(TokenType.IDENT, token.getType());
            assertEquals(expectedLexeme, token.getLexeme());
        }

        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldDistinguishKeywordsFromIdentifiers() {
        JackTokenizer tokenizer = new JackTokenizer(
                "class className Class returnValue");

        assertEquals(TokenType.CLASS, tokenizer.nextToken().getType());

        Token className = tokenizer.nextToken();
        assertEquals(TokenType.IDENT, className.getType());
        assertEquals("className", className.getLexeme());

        Token capitalizedClass = tokenizer.nextToken();
        assertEquals(TokenType.IDENT, capitalizedClass.getType());
        assertEquals("Class", capitalizedClass.getLexeme());

        Token returnValue = tokenizer.nextToken();
        assertEquals(TokenType.IDENT, returnValue.getType());
        assertEquals("returnValue", returnValue.getLexeme());
    }

    @Test
    void shouldPreserveStartingLineOfToken() {
        JackTokenizer tokenizer = new JackTokenizer(
                "class\nMain\nreturn");

        Token first = tokenizer.nextToken();
        Token second = tokenizer.nextToken();
        Token third = tokenizer.nextToken();

        assertEquals(1, first.getLine());
        assertEquals(2, second.getLine());
        assertEquals(3, third.getLine());
    }

    @Test
    void shouldRecognizeAllSymbols() {
        String source = "{}()[].,;+-*/&|<>=~";

        TokenType[] expectedTypes = {
                TokenType.LBRACE,
                TokenType.RBRACE,
                TokenType.LPAREN,
                TokenType.RPAREN,
                TokenType.LBRACKET,
                TokenType.RBRACKET,
                TokenType.DOT,
                TokenType.COMMA,
                TokenType.SEMICOLON,
                TokenType.PLUS,
                TokenType.MINUS,
                TokenType.ASTERISK,
                TokenType.SLASH,
                TokenType.AND,
                TokenType.OR,
                TokenType.LT,
                TokenType.GT,
                TokenType.EQ,
                TokenType.NOT
        };

        JackTokenizer tokenizer = new JackTokenizer(source);

        for (int i = 0; i < expectedTypes.length; i++) {
            Token token = tokenizer.nextToken();

            assertEquals(expectedTypes[i], token.getType());
            assertEquals(
                    String.valueOf(source.charAt(i)),
                    token.getLexeme());
        }

        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldRecognizeIntegerConstants() {
        JackTokenizer tokenizer = new JackTokenizer(
                "0 7 123 32767");

        String[] expectedLexemes = {
                "0",
                "7",
                "123",
                "32767"
        };

        for (String expectedLexeme : expectedLexemes) {
            Token token = tokenizer.nextToken();

            assertEquals(TokenType.NUMBER, token.getType());
            assertEquals(expectedLexeme, token.getLexeme());
        }

        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldRecognizeMinusSeparatelyFromInteger() {
        JackTokenizer tokenizer = new JackTokenizer("-12");

        Token minus = tokenizer.nextToken();

        assertEquals(TokenType.MINUS, minus.getType());
        assertEquals("-", minus.getLexeme());

        Token number = tokenizer.nextToken();

        assertEquals(TokenType.NUMBER, number.getType());
        assertEquals("12", number.getLexeme());
    }

    @Test
    void shouldRejectIntegerAboveMaximum() {
        JackTokenizer tokenizer = new JackTokenizer("32768");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                tokenizer::nextToken);

        assertTrue(exception.getMessage().contains("32768"));
    }

    @Test
    void shouldRejectVeryLargeIntegerWithoutOverflow() {
        JackTokenizer tokenizer = new JackTokenizer(
                "999999999999999999999999999999999999");

        assertThrows(
                IllegalArgumentException.class,
                tokenizer::nextToken);
    }

    @Test
    void shouldRecognizeStringConstants() {
        JackTokenizer tokenizer = new JackTokenizer(
                "\"Hello, Jack!\"");

        Token token = tokenizer.nextToken();

        assertEquals(TokenType.STRING, token.getType());
        assertEquals("Hello, Jack!", token.getLexeme());
        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldRecognizeEmptyString() {
        JackTokenizer tokenizer = new JackTokenizer("\"\"");

        Token token = tokenizer.nextToken();

        assertEquals(TokenType.STRING, token.getType());
        assertEquals("", token.getLexeme());
    }

    @Test
    void shouldPreserveSymbolsInsideString() {
        JackTokenizer tokenizer = new JackTokenizer(
                "\"a < b & c > d\"");

        Token token = tokenizer.nextToken();

        assertEquals(TokenType.STRING, token.getType());
        assertEquals("a < b & c > d", token.getLexeme());
    }

    @Test
    void shouldRecordStartingLineOfString() {
        JackTokenizer tokenizer = new JackTokenizer(
                "class\n\"Hello\"");

        tokenizer.nextToken();

        Token token = tokenizer.nextToken();

        assertEquals(TokenType.STRING, token.getType());
        assertEquals("Hello", token.getLexeme());
        assertEquals(2, token.getLine());
    }

    @Test
    void shouldRejectUnterminatedString() {
        JackTokenizer tokenizer = new JackTokenizer(
                "\"Hello, Jack!");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                tokenizer::nextToken);

        assertTrue(
                exception.getMessage().contains("não terminada"));
    }

    @Test
    void shouldSkipWhitespaceAndLineComments() {
        JackTokenizer tokenizer = new JackTokenizer(
                " \t\r\n// comentário\r\nclass // outro\nMain");

        Token keyword = tokenizer.nextToken();
        assertEquals(TokenType.CLASS, keyword.getType());
        assertEquals(3, keyword.getLine());

        Token identifier = tokenizer.nextToken();
        assertEquals(TokenType.IDENT, identifier.getType());
        assertEquals("Main", identifier.getLexeme());
        assertEquals(4, identifier.getLine());
        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldSkipLineCommentAtEndOfInput() {
        JackTokenizer tokenizer = new JackTokenizer("// sem quebra de linha");

        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldSkipBlockAndDocumentationCommentsBetweenTokens() {
        JackTokenizer tokenizer = new JackTokenizer(
                "class/* bloco */Main/** documentação */{}");

        assertEquals(TokenType.CLASS, tokenizer.nextToken().getType());
        assertEquals("Main", tokenizer.nextToken().getLexeme());
        assertEquals(TokenType.LBRACE, tokenizer.nextToken().getType());
        assertEquals(TokenType.RBRACE, tokenizer.nextToken().getType());
        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldCountLinesInsideBlockComment() {
        JackTokenizer tokenizer = new JackTokenizer(
                "/* primeira\nsegunda\n*/return");

        Token token = tokenizer.nextToken();
        assertEquals(TokenType.RETURN, token.getType());
        assertEquals(3, token.getLine());
    }

    @Test
    void shouldSkipConsecutiveCommentsAtEndOfInput() {
        JackTokenizer tokenizer = new JackTokenizer(
                "/**//* bloco *//** documentação */ \t// fim");

        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldPreserveDivisionAroundComments() {
        JackTokenizer tokenizer = new JackTokenizer("12/3/* bloco *//2/");

        assertEquals("12", tokenizer.nextToken().getLexeme());
        assertEquals(TokenType.SLASH, tokenizer.nextToken().getType());
        assertEquals("3", tokenizer.nextToken().getLexeme());
        assertEquals(TokenType.SLASH, tokenizer.nextToken().getType());
        assertEquals("2", tokenizer.nextToken().getLexeme());
        assertEquals(TokenType.SLASH, tokenizer.nextToken().getType());
        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldPreserveCommentMarkersInsideStrings() {
        JackTokenizer tokenizer = new JackTokenizer(
                "\"// não é comentário /* nem isto */\"");

        Token token = tokenizer.nextToken();
        assertEquals(TokenType.STRING, token.getType());
        assertEquals("// não é comentário /* nem isto */", token.getLexeme());
        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldRejectUnterminatedBlockCommentsWithStartingLine() {
        String[] sources = {"\n/*", "\n/** documentação", "\n/* texto\n*"};

        for (String source : sources) {
            JackTokenizer tokenizer = new JackTokenizer(source);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    tokenizer::nextToken);

            assertEquals("Comentário de bloco não terminado na linha 2",
                    exception.getMessage());
        }
    }

    @Test
    void shouldRecognizeIdentifiersStartingWithUnderscore() {
        JackTokenizer tokenizer = new JackTokenizer("_ _value _count2");

        for (String expected : new String[]{"_", "_value", "_count2"}) {
            Token token = tokenizer.nextToken();
            assertEquals(TokenType.IDENT, token.getType());
            assertEquals(expected, token.getLexeme());
        }
    }

    @Test
    void shouldRejectLineBreaksInsideStrings() {
        for (String separator : new String[]{"\n", "\r", "\r\n"}) {
            JackTokenizer tokenizer = new JackTokenizer("\"Hello" + separator + "Jack\"");
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class, tokenizer::nextToken);
            assertTrue(exception.getMessage().contains("linha 1"));
        }
    }

    @Test
    void shouldCountUnixWindowsAndMacLineEndings() {
        for (String separator : new String[]{"\n", "\r", "\r\n"}) {
            JackTokenizer tokenizer = new JackTokenizer(
                    "// comentário" + separator + "/* bloco" + separator + "*/class");
            Token token = tokenizer.nextToken();
            assertEquals(TokenType.CLASS, token.getType());
            assertEquals(3, token.getLine());
        }
    }

    @Test
    void shouldRejectUnexpectedCharacters() {
        for (String source : new String[]{"@", "é", "١"}) {
            JackTokenizer tokenizer = new JackTokenizer(source);
            assertThrows(IllegalArgumentException.class, tokenizer::nextToken);
        }
    }

    @Test
    void shouldPreserveUnicodeAndBackslashesInsideStrings() {
        JackTokenizer tokenizer = new JackTokenizer("\"Olá \\n\"");
        assertEquals("Olá \\n", tokenizer.nextToken().getLexeme());
        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }

    @Test
    void shouldIgnoreQuotesAndLineCommentMarkersInsideBlockComment() {
        JackTokenizer tokenizer = new JackTokenizer(
                "/* \" // conteúdo */return");

        assertEquals(TokenType.RETURN, tokenizer.nextToken().getType());
        assertEquals(TokenType.EOF, tokenizer.nextToken().getType());
    }
}
