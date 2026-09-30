package compiler;

public class Token {
    public final TokenType type;
    public final String lexeme;
    public final int line;
    public final int column;

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("Token(%-12s | '%s' | linha: %d, col: %d)", 
                             type, lexeme.replace("\n", "\\n"), line, column);
    }
}