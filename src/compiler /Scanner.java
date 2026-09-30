package compiler;

import java.util.HashMap;
import java.util.Map;

public class Scanner {
    private final String source;
    private int current = 0;
    private int line = 1;
    private int column = 1;

    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.put("if", TokenType.IF);
        keywords.put("while", TokenType.WHILE);
        keywords.put("int", TokenType.INT);
        keywords.put("return", TokenType.RETURN);
    }

    public Scanner(String source) {
        this.source = source;
    }

    // --- MÉTODOS DE CONTROLE DO CURSOR ---

    public boolean hasNext() {
        return current < source.length();
    }

    public char peek() {
        if (!hasNext()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    public char advance() {
        char c = source.charAt(current);
        current++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    // --- MOTOR DO SCANNER (AFD) ---

    public Token nextToken() {
        skipWhitespaceAndComments();

        if (!hasNext()) {
            return new Token(TokenType.EOF, "", line, column);
        }

        int startLine = line;
        int startCol = column;
        char c = peek();

        // 1. Strings
        if (c == '"') return scanString(startLine, startCol);

        // 2. Literais Numéricos (simplificado para inteiros e decimais básicos)
        if (Character.isDigit(c)) return scanNumber(startLine, startCol);

        // 3. Identificadores e Palavras Reservadas
        if (Character.isLetter(c) || c == '_') return scanIdentifier(startLine, startCol);

        // 4. Operadores (Aplicando Maximal Munch)
        return scanOperator(startLine, startCol);
    }

    private Token scanString(int startLine, int startCol) {
        int startPos = current;
        advance(); // consome a aspa inicial '"'

        while (peek() != '"' && hasNext()) {
            advance();
        }

        // Tratamento de Erro: EOF no meio da string
        if (!hasNext()) {
            String lexeme = source.substring(startPos, current);
            reportError("String não fechada antes do fim do arquivo.", startLine, startCol);
            return new Token(TokenType.ERROR, lexeme, startLine, startCol);
        }

        advance(); // consome a aspa final '"'
        String lexeme = source.substring(startPos, current);
        return new Token(TokenType.STRING, lexeme, startLine, startCol);
    }

    private Token scanNumber(int startLine, int startCol) {
        int startPos = current;
        while (Character.isDigit(peek())) {
            advance();
        }

        // Parte decimal
        if (peek() == '.' && Character.isDigit(peekNext())) {
            advance(); // consome o ponto
            while (Character.isDigit(peek())) {
                advance();
            }
        }

        String lexeme = source.substring(startPos, current);
        return new Token(TokenType.NUMBER, lexeme, startLine, startCol);
    }

    private Token scanIdentifier(int startLine, int startCol) {
        int startPos = current;
        while (Character.isLetterOrDigit(peek()) || peek() == '_') {
            advance();
        }

        String lexeme = source.substring(startPos, current);
        TokenType type = keywords.getOrDefault(lexeme, TokenType.IDENTIFIER);
        return new Token(type, lexeme, startLine, startCol);
    }

    private Token scanOperator(int startLine, int startCol) {
        char c = advance();
        String lexeme = String.valueOf(c);

        switch (c) {
            case '+':
                return new Token(TokenType.PLUS, lexeme, startLine, startCol);
            case '=':
                if (peek() == '=') {
                    advance(); // Maximal munch: forma '=='
                    return new Token(TokenType.EQUALS, "==", startLine, startCol);
                }
                return new Token(TokenType.ASSIGN, lexeme, startLine, startCol);
            case '<':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.LESS_EQUAL, "<=", startLine, startCol);
                }
                return new Token(TokenType.LESS_THAN, lexeme, startLine, startCol);
            case '!':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.NOT_EQUAL, "!=", startLine, startCol);
                }
                break; // '!' sozinho é inválido nesta linguagem exemplo
            case '&':
                if (peek() == '&') {
                    advance();
                    return new Token(TokenType.AND, "&&", startLine, startCol);
                }
                break; // '&' sozinho é inválido
        }

        // Tratamento de Erro: Caractere inválido
        reportError("Caractere não reconhecido: '" + c + "'", startLine, startCol);
        // Retorna erro mas não trava, o analisador pode continuar a pedir nextToken()
        return new Token(TokenType.ERROR, lexeme, startLine, startCol); 
    }

    private void skipWhitespaceAndComments() {
        while (hasNext()) {
            char c = peek();
            if (c == ' ' || c == '\r' || c == '\t' || c == '\n') {
                advance();
            } else if (c == '/') {
                if (peekNext() == '/') {
                    // Comentário de linha: consome até o \n
                    while (peek() != '\n' && hasNext()) advance();
                } else if (peekNext() == '*') {
                    // Comentário de bloco
                    int startLine = line;
                    int startCol = column;
                    advance(); advance(); // consome '/*'
                    
                    boolean closed = false;
                    while (hasNext()) {
                        if (peek() == '*' && peekNext() == '/') {
                            advance(); advance(); // consome '*/'
                            closed = true;
                            break;
                        }
                        advance();
                    }
                    // Tratamento de Erro: EOF no comentário
                    if (!closed) {
                        reportError("Comentário de bloco não fechado.", startLine, startCol);
                    }
                } else {
                    break; // É um operador de divisão (não implementado aqui, mas pararia)
                }
            } else {
                break; // Não é espaço nem comentário, sai do loop
            }
        }
    }

    private void reportError(String message, int errLine, int errCol) {
        System.err.println("[Erro Léxico] Linha " + errLine + ", Coluna " + errCol + ": " + message);
    }
}