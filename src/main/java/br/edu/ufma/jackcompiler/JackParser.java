package br.edu.ufma.jackcompiler;

import java.util.Objects;

public final class JackParser {

    private final JackTokenizer tokenizer;
    private final SyntaxXmlWriter xmlWriter;

    private Token currentToken;

    public JackParser(JackTokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(
                tokenizer,
                "O analisador léxico não pode ser nulo.");

        this.xmlWriter = new SyntaxXmlWriter();
        this.currentToken = this.tokenizer.nextToken();
    }

    Token advance() {
        Token token = currentToken;

        if (currentToken.getType() != TokenType.EOF) {
            currentToken = tokenizer.nextToken();
        }

        return token;
    }

    Token peek() {
        return currentToken;
    }

    boolean match(TokenType tokenType) {
        Objects.requireNonNull(
                tokenType,
                "O tipo do token não pode ser nulo.");

        if (currentToken.getType() != tokenType) {
            return false;
        }

        advance();

        return true;
    }

    Token consume(TokenType expectedType) {
        Objects.requireNonNull(
                expectedType,
                "O tipo esperado não pode ser nulo.");

        if (currentToken.getType() == expectedType) {
            return advance();
        }

        throw new JackSyntaxException(
                currentToken,
                expectedType);
    }

    private Token consumeAndWrite(TokenType expectedType) {
        Token token = consume(expectedType);

        xmlWriter.writeToken(token);

        return token;
    }

    public String parseClass() {
        xmlWriter.openTag("class");

        consumeAndWrite(TokenType.CLASS);
        consumeAndWrite(TokenType.IDENT);
        consumeAndWrite(TokenType.LBRACE);

        while (currentToken.getType() == TokenType.STATIC
                || currentToken.getType() == TokenType.FIELD) {

            parseClassVarDec();
        }

        while (isSubroutineStart(currentToken.getType())) {
            parseSubroutineDec();
        }

        consumeAndWrite(TokenType.RBRACE);

        xmlWriter.closeTag("class");

        consume(TokenType.EOF);

        return xmlWriter.toXml();
    }

    private void parseClassVarDec() {
        xmlWriter.openTag("classVarDec");

        if (currentToken.getType() == TokenType.STATIC) {
            consumeAndWrite(TokenType.STATIC);
        } else if (currentToken.getType() == TokenType.FIELD) {
            consumeAndWrite(TokenType.FIELD);
        } else {
            throw new JackSyntaxException(
                    currentToken,
                    "STATIC ou FIELD");
        }

        parseType();

        consumeAndWrite(TokenType.IDENT);

        while (currentToken.getType() == TokenType.COMMA) {
            consumeAndWrite(TokenType.COMMA);
            consumeAndWrite(TokenType.IDENT);
        }

        consumeAndWrite(TokenType.SEMICOLON);

        xmlWriter.closeTag("classVarDec");
    }

    private void parseType() {
        TokenType type = currentToken.getType();

        if (type == TokenType.INT
                || type == TokenType.CHAR
                || type == TokenType.BOOLEAN
                || type == TokenType.IDENT) {

            consumeAndWrite(type);

            return;
        }

        throw new JackSyntaxException(
                currentToken,
                "tipo int, char, boolean ou nome de classe");
    }

    private boolean isSubroutineStart(TokenType type) {
        return type == TokenType.CONSTRUCTOR
                || type == TokenType.FUNCTION
                || type == TokenType.METHOD;
    }

    private void parseSubroutineDec() {
        xmlWriter.openTag("subroutineDec");

        if (isSubroutineStart(currentToken.getType())) {
            consumeAndWrite(currentToken.getType());
        } else {
            throw new JackSyntaxException(
                    currentToken,
                    "CONSTRUCTOR, FUNCTION ou METHOD");
        }

        parseReturnType();

        consumeAndWrite(TokenType.IDENT);
        consumeAndWrite(TokenType.LPAREN);

        parseParameterList();

        consumeAndWrite(TokenType.RPAREN);

        parseSubroutineBody();

        xmlWriter.closeTag("subroutineDec");
    }

    private void parseReturnType() {
        if (currentToken.getType() == TokenType.VOID) {
            consumeAndWrite(TokenType.VOID);
        } else {
            parseType();
        }
    }

    private void parseParameterList() {
        xmlWriter.openTag("parameterList");

        if (isTypeStart(currentToken.getType())) {
            parseType();

            consumeAndWrite(TokenType.IDENT);

            while (currentToken.getType() == TokenType.COMMA) {
                consumeAndWrite(TokenType.COMMA);

                parseType();

                consumeAndWrite(TokenType.IDENT);
            }
        }

        xmlWriter.closeTag("parameterList");
    }

    private boolean isTypeStart(TokenType type) {
        return type == TokenType.INT
                || type == TokenType.CHAR
                || type == TokenType.BOOLEAN
                || type == TokenType.IDENT;
    }

    private void parseSubroutineBody() {
        xmlWriter.openTag("subroutineBody");

        consumeAndWrite(TokenType.LBRACE);

        while (currentToken.getType() == TokenType.VAR) {
            parseVarDec();
        }

        consumeAndWrite(TokenType.RBRACE);

        xmlWriter.closeTag("subroutineBody");
    }

    private void parseVarDec() {
        xmlWriter.openTag("varDec");

        consumeAndWrite(TokenType.VAR);

        parseType();

        consumeAndWrite(TokenType.IDENT);

        while (currentToken.getType() == TokenType.COMMA) {
            consumeAndWrite(TokenType.COMMA);
            consumeAndWrite(TokenType.IDENT);
        }

        consumeAndWrite(TokenType.SEMICOLON);

        xmlWriter.closeTag("varDec");
    }
}