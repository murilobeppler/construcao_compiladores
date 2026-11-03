package ast;

import visitor.Visitor;

/** Literal char: Primary -> CHAR_LIT. */
public class CharLit extends Expr {
    /** Conteúdo entre as aspas simples, com o escape como no fonte (ex.: a, \n). */
    public final String valor;

    public CharLit(String valor, int line, int col) {
        super(line, col);
        this.valor = valor;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitCharLit(this);
    }
}
