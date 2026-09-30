package compiler;

public enum TokenType {
    // Categorias básicas
    IDENTIFIER, NUMBER, STRING,
    
    // Palavras reservadas
    IF, WHILE, INT, RETURN,
    
    // Operadores
    ASSIGN,       // =
    EQUALS,       // ==
    LESS_THAN,    // <
    LESS_EQUAL,   // <=
    NOT_EQUAL,    // !=
    PLUS,         // +
    AND,          // &&
    
    // Outros
    EOF,          // Fim de arquivo
    ERROR         // Token de erro léxico
}