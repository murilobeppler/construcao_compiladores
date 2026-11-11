/** Um erro léxico com a posição (linha/coluna) onde ele ocorre. */
public class LexicalError {
    public final int line;
    public final int col;
    public final String message;

    public LexicalError(int line, int col, String message) {
        this.line = line;
        this.col = col;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Erro léxico [linha " + line + ", coluna " + col + "]: " + message;
    }
}
