package symbols;

/**
 * Um erro da análise contextual (identificação). A coluna não é impressa: ela só serve
 * para ordenar os erros na ordem do texto quando dois caem na mesma linha.
 *
 * Formato (determinístico):
 *   linha 2: 'a' já declarado neste escopo (linha 1)
 *   linha 4: 'b' não declarado
 */
public class ContextualError {
    public final int line;
    public final int col;
    public final String message;

    public ContextualError(int line, int col, String message) {
        this.line = line;
        this.col = col;
        this.message = message;
    }

    @Override
    public String toString() {
        return "linha " + line + ": " + message;
    }
}

