package br.edu.ufma.jackcompiler;

public enum TokenType {

    // Símbolos
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    LBRACKET,
    RBRACKET,
    COMMA,
    SEMICOLON,
    DOT,
    PLUS,
    MINUS,
    ASTERISK,
    SLASH,
    AND,
    OR,
    NOT,
    LT,
    GT,
    EQ,

    //Constantes e identificadores
    NUMBER,
    IDENT,
    STRING,

    // Palavras reservadas do Jack
    CLASS,
    CONSTRUCTOR,
    FUNCTION,
    METHOD,
    FIELD,
    STATIC,
    VAR,
    INT,
    CHAR,
    BOOLEAN,
    VOID,
    TRUE,
    FALSE,
    NULL,
    THIS,
    LET,
    DO,
    IF,
    ELSE,
    WHILE,
    RETURN,

    // Fim
    EOF
}