package br.edu.ufma.jackcompiler;

import java.util.Objects;

public final class JackParser {

    private final JackTokenizer tokenizer;
    private Token currentToken;

    public JackParser(JackTokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(
                tokenizer,
                "O analisador léxico não pode ser nulo.");

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
}