
package br.edu.ufma.jackcompiler;

import java.util.Objects;

/**
 * Motor de compilação Jack para geração de código da máquina virtual.
 *
 * <p>O motor reutiliza o {@link JackTokenizer}, produz código VM em vez de XML,
 * e resolve os identificadores pela {@link SymbolTable}. Implementa expressões,
 * comandos, subrotinas, objetos, arrays e strings literais.</p>
 */
public final class JackCompilationEngine {

    private final JackTokenizer tokenizer;
    private final VMWriter vmWriter;
    private final SymbolTable symbolTable;

    private Token currentToken;
    private String className;
    private TokenType currentSubroutineKind;
    private int labelCounter;

    public JackCompilationEngine(JackTokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(
                tokenizer,
                "O analisador léxico não pode ser nulo.");

        this.vmWriter = new VMWriter();
        this.symbolTable = new SymbolTable();
        this.currentToken = tokenizer.nextToken();
        this.labelCounter = 0;
    }

    /** Analisa uma classe Jack completa e retorna o código VM gerado. */
    public String compileClass() {
        consume(TokenType.CLASS);
        className = consume(TokenType.IDENT).getLexeme();
        consume(TokenType.LBRACE);

        while (check(TokenType.STATIC) || check(TokenType.FIELD)) {
            compileClassVarDec();
        }

        while (isSubroutineStart(currentToken.getType())) {
            compileSubroutine();
        }

        consume(TokenType.RBRACE);
        consume(TokenType.EOF);

        return vmWriter.toCode();
    }

    private void compileClassVarDec() {
        Token declaration = advance();

        SymbolTable.Kind kind = declaration.getType() == TokenType.STATIC
                ? SymbolTable.Kind.STATIC
                : SymbolTable.Kind.FIELD;

        String type = compileType();

        defineIdentifier(consume(TokenType.IDENT), type, kind);

        while (match(TokenType.COMMA)) {
            defineIdentifier(consume(TokenType.IDENT), type, kind);
        }

        consume(TokenType.SEMICOLON);
    }

    private void compileSubroutine() {
        currentSubroutineKind = advance().getType();

        if (check(TokenType.VOID)) {
            advance();
        } else {
            compileType();
        }

        String subroutineName = consume(TokenType.IDENT).getLexeme();

        symbolTable.startSubroutine();

        if (currentSubroutineKind == TokenType.METHOD) {
            symbolTable.define(
                    "this",
                    className,
                    SymbolTable.Kind.ARG);
        }

        consume(TokenType.LPAREN);
        compileParameterList();
        consume(TokenType.RPAREN);

        consume(TokenType.LBRACE);

        while (check(TokenType.VAR)) {
            compileVarDec();
        }

        vmWriter.writeFunction(
                className + "." + subroutineName,
                symbolTable.varCount(SymbolTable.Kind.VAR));

        if (currentSubroutineKind == TokenType.METHOD) {
            vmWriter.writePush("argument", 0);
            vmWriter.writePop("pointer", 0);

        } else if (currentSubroutineKind == TokenType.CONSTRUCTOR) {
            vmWriter.writePush(
                    "constant",
                    symbolTable.varCount(SymbolTable.Kind.FIELD));

            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop("pointer", 0);
        }

        compileStatements();
        consume(TokenType.RBRACE);
    }

    private void compileParameterList() {
        if (!isTypeStart(currentToken.getType())) {
            return;
        }

        do {
            String type = compileType();
            Token name = consume(TokenType.IDENT);

            defineIdentifier(name, type, SymbolTable.Kind.ARG);

        } while (match(TokenType.COMMA));
    }

    private void compileVarDec() {
        consume(TokenType.VAR);

        String type = compileType();

        defineIdentifier(
                consume(TokenType.IDENT),
                type,
                SymbolTable.Kind.VAR);

        while (match(TokenType.COMMA)) {
            defineIdentifier(
                    consume(TokenType.IDENT),
                    type,
                    SymbolTable.Kind.VAR);
        }

        consume(TokenType.SEMICOLON);
    }

    private void compileStatements() {
        while (isStatementStart(currentToken.getType())) {
            switch (currentToken.getType()) {
                case LET -> compileLet();
                case IF -> compileIf();
                case WHILE -> compileWhile();
                case DO -> compileDo();
                case RETURN -> compileReturn();

                default -> throw new JackSyntaxException(
                        currentToken,
                        "comando Jack válido");
            }
        }
    }

    private void compileLet() {
        consume(TokenType.LET);

        Token variable = consume(TokenType.IDENT);
        SymbolTable.Symbol symbol = resolveIdentifier(variable);

        if (match(TokenType.LBRACKET)) {
            // Calcula e preserva o endereço antes do lado direito.
            // Isso é importante quando a expressão da direita também acessa arrays.
            vmWriter.writePush(segmentOf(symbol), symbol.index());

            compileExpression();
            consume(TokenType.RBRACKET);

            vmWriter.writeArithmetic("add");

            consume(TokenType.EQ);
            compileExpression();
            consume(TokenType.SEMICOLON);

            vmWriter.writePop("temp", 0);
            vmWriter.writePop("pointer", 1);
            vmWriter.writePush("temp", 0);
            vmWriter.writePop("that", 0);

            return;
        }

        consume(TokenType.EQ);
        compileExpression();
        consume(TokenType.SEMICOLON);

        vmWriter.writePop(segmentOf(symbol), symbol.index());
    }

    private void compileIf() {
        consume(TokenType.IF);
        consume(TokenType.LPAREN);

        compileExpression();

        consume(TokenType.RPAREN);

        int label = labelCounter++;

        String trueLabel = "IF_TRUE" + label;
        String falseLabel = "IF_FALSE" + label;
        String endLabel = "IF_END" + label;

        vmWriter.writeIf(trueLabel);
        vmWriter.writeGoto(falseLabel);
        vmWriter.writeLabel(trueLabel);

        consume(TokenType.LBRACE);
        compileStatements();
        consume(TokenType.RBRACE);

        if (match(TokenType.ELSE)) {
            vmWriter.writeGoto(endLabel);
            vmWriter.writeLabel(falseLabel);

            consume(TokenType.LBRACE);
            compileStatements();
            consume(TokenType.RBRACE);

            vmWriter.writeLabel(endLabel);

        } else {
            vmWriter.writeLabel(falseLabel);
        }
    }

    private void compileWhile() {
        consume(TokenType.WHILE);

        int label = labelCounter++;

        String expressionLabel = "WHILE_EXP" + label;
        String endLabel = "WHILE_END" + label;

        vmWriter.writeLabel(expressionLabel);

        consume(TokenType.LPAREN);

        compileExpression();

        consume(TokenType.RPAREN);

        vmWriter.writeArithmetic("not");
        vmWriter.writeIf(endLabel);

        consume(TokenType.LBRACE);
        compileStatements();
        consume(TokenType.RBRACE);

        vmWriter.writeGoto(expressionLabel);
        vmWriter.writeLabel(endLabel);
    }

    private void compileDo() {
        consume(TokenType.DO);

        Token firstIdentifier = consume(TokenType.IDENT);

        compileSubroutineCall(firstIdentifier);

        consume(TokenType.SEMICOLON);

        vmWriter.writePop("temp", 0);
    }

    private void compileReturn() {
        consume(TokenType.RETURN);

        if (isTermStart(currentToken.getType())) {
            compileExpression();
        } else {
            vmWriter.writePush("constant", 0);
        }

        consume(TokenType.SEMICOLON);

        vmWriter.writeReturn();
    }

    private void compileExpression() {
        compileTerm();

        while (isOperator(currentToken.getType())) {
            TokenType operator = advance().getType();

            compileTerm();

            writeBinaryOperator(operator);
        }
    }

    private void compileTerm() {
        switch (currentToken.getType()) {
            case NUMBER -> {
                int value = Integer.parseInt(advance().getLexeme());

                vmWriter.writePush("constant", value);
            }

            case STRING -> compileStringConstant(advance());

            case TRUE -> {
                advance();

                vmWriter.writePush("constant", 1);
                vmWriter.writeArithmetic("neg");
            }

            case FALSE, NULL -> {
                advance();

                vmWriter.writePush("constant", 0);
            }

            case THIS -> {
                advance();

                vmWriter.writePush("pointer", 0);
            }

            case LPAREN -> {
                advance();

                compileExpression();

                consume(TokenType.RPAREN);
            }

            case MINUS, NOT -> {
                TokenType unaryOperator = advance().getType();

                compileTerm();

                vmWriter.writeArithmetic(
                        unaryOperator == TokenType.MINUS
                                ? "neg"
                                : "not");
            }

            case IDENT -> compileIdentifierTerm();

            default -> throw new JackSyntaxException(
                    currentToken,
                    "termo válido em expressão");
        }
    }

    private void compileIdentifierTerm() {
        Token identifier = consume(TokenType.IDENT);

        if (match(TokenType.LBRACKET)) {
            SymbolTable.Symbol array = resolveIdentifier(identifier);

            vmWriter.writePush(segmentOf(array), array.index());

            compileExpression();

            consume(TokenType.RBRACKET);

            vmWriter.writeArithmetic("add");
            vmWriter.writePop("pointer", 1);
            vmWriter.writePush("that", 0);

            return;
        }

        if (check(TokenType.LPAREN) || check(TokenType.DOT)) {
            compileSubroutineCall(identifier);
            return;
        }

        SymbolTable.Symbol symbol = resolveIdentifier(identifier);

        vmWriter.writePush(segmentOf(symbol), symbol.index());
    }

    private void compileSubroutineCall(Token firstIdentifier) {
        String targetClass = className;
        String subroutineName = firstIdentifier.getLexeme();

        int argumentCount = 0;

        if (match(TokenType.DOT)) {
            Token methodName = consume(TokenType.IDENT);

            subroutineName = methodName.getLexeme();

            SymbolTable.Symbol receiver = symbolTable
                    .resolve(firstIdentifier.getLexeme())
                    .orElse(null);

            if (receiver != null) {
                // O objeto é o primeiro argumento implícito do método.
                vmWriter.writePush(
                        segmentOf(receiver),
                        receiver.index());

                targetClass = receiver.type();
                argumentCount++;

            } else {
                // Prefixo que não é variável representa o nome de uma classe.
                targetClass = firstIdentifier.getLexeme();
            }

        } else {
            // Chamada sem prefixo: método da instância atual.
            vmWriter.writePush("pointer", 0);
            argumentCount++;
        }

        consume(TokenType.LPAREN);

        argumentCount += compileExpressionList();

        consume(TokenType.RPAREN);

        vmWriter.writeCall(
                targetClass + "." + subroutineName,
                argumentCount);
    }

    private void compileStringConstant(Token stringToken) {
        String value = stringToken.getLexeme();

        vmWriter.writePush("constant", value.length());
        vmWriter.writeCall("String.new", 1);

        for (int index = 0; index < value.length(); index++) {
            int character = value.charAt(index);

            if (character > 32767) {
                throw new JackCompilationException(
                        stringToken,
                        "Caractere fora da faixa aceita pela VM: "
                                + character);
            }

            vmWriter.writePush("constant", character);
            vmWriter.writeCall("String.appendChar", 2);
        }
    }

    private int compileExpressionList() {
        int expressionCount = 0;

        if (isTermStart(currentToken.getType())) {
            compileExpression();
            expressionCount++;

            while (match(TokenType.COMMA)) {
                compileExpression();
                expressionCount++;
            }
        }

        return expressionCount;
    }

    private String compileType() {
        if (isTypeStart(currentToken.getType())) {
            return advance().getLexeme();
        }

        throw new JackSyntaxException(
                currentToken,
                "tipo int, char, boolean ou nome de classe");
    }

    private void defineIdentifier(
            Token identifier,
            String type,
            SymbolTable.Kind kind) {

        try {
            symbolTable.define(identifier.getLexeme(), type, kind);

        } catch (IllegalArgumentException exception) {
            throw new JackCompilationException(
                    identifier,
                    exception.getMessage());
        }
    }

    private SymbolTable.Symbol resolveIdentifier(Token identifier) {
        return symbolTable.resolve(identifier.getLexeme())
                .orElseThrow(() -> new JackCompilationException(
                        identifier,
                        "Identificador não declarado: "
                                + identifier.getLexeme()));
    }

    private String segmentOf(SymbolTable.Symbol symbol) {
        return switch (symbol.kind()) {
            case STATIC -> "static";
            case FIELD -> "this";
            case ARG -> "argument";
            case VAR -> "local";
        };
    }

    private void writeBinaryOperator(TokenType operator) {
        switch (operator) {
            case PLUS -> vmWriter.writeArithmetic("add");
            case MINUS -> vmWriter.writeArithmetic("sub");

            case ASTERISK ->
                    vmWriter.writeCall("Math.multiply", 2);

            case SLASH ->
                    vmWriter.writeCall("Math.divide", 2);

            case AND -> vmWriter.writeArithmetic("and");
            case OR -> vmWriter.writeArithmetic("or");
            case LT -> vmWriter.writeArithmetic("lt");
            case GT -> vmWriter.writeArithmetic("gt");
            case EQ -> vmWriter.writeArithmetic("eq");

            default -> throw new IllegalArgumentException(
                    "Operador binário não suportado: " + operator);
        }
    }

    private Token advance() {
        Token token = currentToken;

        if (currentToken.getType() != TokenType.EOF) {
            currentToken = tokenizer.nextToken();
        }

        return token;
    }

    private Token consume(TokenType expected) {
        if (currentToken.getType() != expected) {
            throw new JackSyntaxException(currentToken, expected);
        }

        return advance();
    }

    private boolean match(TokenType expected) {
        if (!check(expected)) {
            return false;
        }

        advance();

        return true;
    }

    private boolean check(TokenType type) {
        return currentToken.getType() == type;
    }

    private boolean isSubroutineStart(TokenType type) {
        return type == TokenType.CONSTRUCTOR
                || type == TokenType.FUNCTION
                || type == TokenType.METHOD;
    }

    private boolean isTypeStart(TokenType type) {
        return type == TokenType.INT
                || type == TokenType.CHAR
                || type == TokenType.BOOLEAN
                || type == TokenType.IDENT;
    }

    private boolean isStatementStart(TokenType type) {
        return type == TokenType.LET
                || type == TokenType.IF
                || type == TokenType.WHILE
                || type == TokenType.DO
                || type == TokenType.RETURN;
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
