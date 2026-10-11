package br.edu.ufma.jackcompiler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class JackTokenizer {

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("class", TokenType.CLASS);
        KEYWORDS.put("constructor", TokenType.CONSTRUCTOR);
        KEYWORDS.put("function", TokenType.FUNCTION);
        KEYWORDS.put("method", TokenType.METHOD);
        KEYWORDS.put("field", TokenType.FIELD);
        KEYWORDS.put("static", TokenType.STATIC);
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("char", TokenType.CHAR);
        KEYWORDS.put("boolean", TokenType.BOOLEAN);
        KEYWORDS.put("void", TokenType.VOID);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("null", TokenType.NULL);
        KEYWORDS.put("this", TokenType.THIS);
        KEYWORDS.put("let", TokenType.LET);
        KEYWORDS.put("do", TokenType.DO);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("return", TokenType.RETURN);
    }

    private final String input;
    private int current;
    private int line;

    public JackTokenizer(String input) {
        this.input = Objects.requireNonNull(
                input,
                "O código-fonte não pode ser nulo.");

        this.current = 0;
        this.line = 1;
    }

    public Token nextToken() {
        skipWhitespace();

        if (isAtEnd()) {
            return new Token(TokenType.EOF, "EOF", line);
        }

        char character = peek();

        if (Character.isLetter(character)) {
            return identifier();
        }

        if (isDigit(character)) {
            return integerConstant();
        }

        return symbol();
    }

    private Token symbol() {
        int startLine = line;
        char character = peek();

        TokenType type = switch (character) {
            case '(' -> TokenType.LPAREN;
            case ')' -> TokenType.RPAREN;
            case '{' -> TokenType.LBRACE;
            case '}' -> TokenType.RBRACE;
            case '[' -> TokenType.LBRACKET;
            case ']' -> TokenType.RBRACKET;
            case ',' -> TokenType.COMMA;
            case ';' -> TokenType.SEMICOLON;
            case '.' -> TokenType.DOT;
            case '+' -> TokenType.PLUS;
            case '-' -> TokenType.MINUS;
            case '*' -> TokenType.ASTERISK;
            case '/' -> TokenType.SLASH;
            case '&' -> TokenType.AND;
            case '|' -> TokenType.OR;
            case '~' -> TokenType.NOT;
            case '<' -> TokenType.LT;
            case '>' -> TokenType.GT;
            case '=' -> TokenType.EQ;

            default -> throw new IllegalArgumentException(
                    "Caractere inesperado na linha "
                            + line + ": '" + character + "'");
        };

        advance();

        return new Token(type, String.valueOf(character), startLine);
    }

    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(peek())) {
            advance();
        }
    }

    private Token identifier() {
        int start = current;
        int startLine = line;

        while (!isAtEnd() && isIdentifierPart(peek())) {
            advance();
        }

        String lexeme = input.substring(start, current);

        TokenType type = KEYWORDS.getOrDefault(
                lexeme,
                TokenType.IDENT);

        return new Token(type, lexeme, startLine);
    }

    private Token integerConstant() {
        int start = current;
        int startLine = line;
        int value = 0;
        boolean outOfRange = false;

        while (!isAtEnd() && isDigit(peek())) {
            int digit = advance() - '0';

            if (!outOfRange) {
                if (value > (32767 - digit) / 10) {
                    outOfRange = true;
                } else {
                    value = value * 10 + digit;
                }
            }
        }

        String lexeme = input.substring(start, current);

        if (outOfRange) {
            throw new IllegalArgumentException(
                    "Constante inteira fora do intervalo permitido "
                            + "(0 a 32767) na linha "
                            + startLine + ": " + lexeme);
        }

        return new Token(TokenType.NUMBER, lexeme, startLine);
    }

    private boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private boolean isIdentifierPart(char character) {
        return Character.isLetterOrDigit(character)
                || character == '_';
    }

    boolean isAtEnd() {
        return current >= input.length();
    }

    char peek() {
        if (isAtEnd()) {
            return '\0';
        }

        return input.charAt(current);
    }

    char peekNext() {
        int next = current + 1;

        if (next >= input.length()) {
            return '\0';
        }

        return input.charAt(next);
    }

    char advance() {
        if (isAtEnd()) {
            return '\0';
        }

        char character = input.charAt(current);
        current++;

        if (character == '\n') {
            line++;
        }

        return character;
    }

    int currentLine() {
        return line;
    }
}
