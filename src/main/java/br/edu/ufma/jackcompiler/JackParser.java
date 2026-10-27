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

        parseStatements();

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

    private void parseStatements() {
        xmlWriter.openTag("statements");

        while (currentToken.getType() == TokenType.LET
                || currentToken.getType() == TokenType.IF
                || currentToken.getType() == TokenType.WHILE) {

            switch (currentToken.getType()) {
                case LET:
                    parseLetStatement();
                    break;

                case IF:
                    parseIfStatement();
                    break;

                case WHILE:
                    parseWhileStatement();
                    break;

                default:
                    throw new JackSyntaxException(
                            currentToken,
                            "comando válido");
            }
        }

        xmlWriter.closeTag("statements");
    }

    private void parseLetStatement() {
        xmlWriter.openTag("letStatement");

        consumeAndWrite(TokenType.LET);

        consumeAndWrite(TokenType.IDENT);

        if (currentToken.getType() == TokenType.LBRACKET) {
            consumeAndWrite(TokenType.LBRACKET);

            parseExpression();

            consumeAndWrite(TokenType.RBRACKET);
        }

        consumeAndWrite(TokenType.EQ);

        parseExpression();

        consumeAndWrite(TokenType.SEMICOLON);

        xmlWriter.closeTag("letStatement");
    }

    private void parseIfStatement() {
        xmlWriter.openTag("ifStatement");

        consumeAndWrite(TokenType.IF);

        consumeAndWrite(TokenType.LPAREN);

        parseExpression();

        consumeAndWrite(TokenType.RPAREN);

        consumeAndWrite(TokenType.LBRACE);

        parseStatements();

        consumeAndWrite(TokenType.RBRACE);

        if (currentToken.getType() == TokenType.ELSE) {
            consumeAndWrite(TokenType.ELSE);

            consumeAndWrite(TokenType.LBRACE);

            parseStatements();

            consumeAndWrite(TokenType.RBRACE);
        }

        xmlWriter.closeTag("ifStatement");
    }

    // whileStatement: 'while' '(' expression ')' '{' statements '}'
    private void parseWhileStatement() {
        xmlWriter.openTag("whileStatement");

        consumeAndWrite(TokenType.WHILE);
        consumeAndWrite(TokenType.LPAREN);

        parseExpression();

        consumeAndWrite(TokenType.RPAREN);
        consumeAndWrite(TokenType.LBRACE);

        parseStatements();

        consumeAndWrite(TokenType.RBRACE);

        xmlWriter.closeTag("whileStatement");
    }

    private void parseExpression() {
        xmlWriter.openTag("expression");

        parseTerm();

        while (isOperator(currentToken.getType())) {
            consumeAndWrite(currentToken.getType());

            parseTerm();
        }

        xmlWriter.closeTag("expression");
    }

    private boolean isOperator(TokenType type) {
        return type == TokenType.PLUS
                || type == TokenType.MINUS
                || type == TokenType.ASTERISK
                || type == TokenType.SLASH
                || type == TokenType.AND
                || type == TokenType.OR
                || type == TokenType.LT
                || type == TokenType.GT
                || type == TokenType.EQ;
    }

    private void parseTerm() {
        xmlWriter.openTag("term");

        TokenType type = currentToken.getType();

        switch (type) {
            case NUMBER:
            case STRING:
            case TRUE:
            case FALSE:
            case NULL:
            case THIS:
                consumeAndWrite(type);
                break;

            case LPAREN:
                consumeAndWrite(TokenType.LPAREN);

                parseExpression();

                consumeAndWrite(TokenType.RPAREN);
                break;

            case MINUS:
            case NOT:
                consumeAndWrite(type);

                parseTerm();
                break;

            case IDENT:
                parseIdentifierTerm();
                break;

            default:
                throw new JackSyntaxException(
                        currentToken,
                        "termo válido: constante, variável, chamada "
                                + "de subrotina, expressão entre "
                                + "parênteses ou operador unário");
        }

        xmlWriter.closeTag("term");
    }

    private void parseIdentifierTerm() {
        consumeAndWrite(TokenType.IDENT);

        if (currentToken.getType() == TokenType.LBRACKET) {
            consumeAndWrite(TokenType.LBRACKET);

            parseExpression();

            consumeAndWrite(TokenType.RBRACKET);

        } else if (currentToken.getType() == TokenType.LPAREN) {
            consumeAndWrite(TokenType.LPAREN);

            parseExpressionList();

            consumeAndWrite(TokenType.RPAREN);

        } else if (currentToken.getType() == TokenType.DOT) {
            consumeAndWrite(TokenType.DOT);

            consumeAndWrite(TokenType.IDENT);

            consumeAndWrite(TokenType.LPAREN);

            parseExpressionList();

            consumeAndWrite(TokenType.RPAREN);
        }
    }

    private void parseExpressionList() {
        xmlWriter.openTag("expressionList");

        if (isTermStart(currentToken.getType())) {
            parseExpression();

            while (currentToken.getType() == TokenType.COMMA) {
                consumeAndWrite(TokenType.COMMA);

                parseExpression();
            }
        }

        xmlWriter.closeTag("expressionList");
    }

    private boolean isTermStart(TokenType type) {
        return type == TokenType.NUMBER
                || type == TokenType.STRING
                || type == TokenType.TRUE
                || type == TokenType.FALSE
                || type == TokenType.NULL
                || type == TokenType.THIS
                || type == TokenType.IDENT
                || type == TokenType.LPAREN
                || type == TokenType.MINUS
                || type == TokenType.NOT;
    }
}
