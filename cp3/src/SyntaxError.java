/** Um erro sintático com a posição (linha/coluna) do token onde foi detectado. */
public class SyntaxError {
    public final int line;
    public final int col;
    public final String message;

    public SyntaxError(int line, int col, String message) {
        this.line = line;
        this.col = col;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Erro sintático [linha " + line + ", coluna " + col + "]: " + message;
    }
}
