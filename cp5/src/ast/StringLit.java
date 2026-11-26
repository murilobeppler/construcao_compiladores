package ast;

import visitor.Visitor;

/** Literal string: Primary -> STRING_LIT. */
public class StringLit extends Expr {
    /** Conteúdo entre as aspas, com os escapes como no fonte. */
    public final String valor;

    public StringLit(String valor, int line, int col) {
        super(line, col);
        this.valor = valor;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitStringLit(this);
    }
}
