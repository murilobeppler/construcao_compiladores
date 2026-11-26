package ast;

/**
 * Categoria sintática "expressão".
 *
 * CP5: tipo é a ÚNICA mudança na hierarquia de nós. É gravado pelo verificador de tipos
 * (visitor/TypeChecker) em toda expressão; null até lá. A geração de código lê este campo,
 * por exemplo para saber que um operando INT de uma operação DOUBLE precisa ser convertido.
 */
public abstract class Expr extends Node {
    /** Tipo calculado pelo TypeChecker (CP5); null antes da verificação. */
    public Tipo tipo;

    protected Expr(int line, int col) {
        super(line, col);
    }
}
