package ast;

import visitor.Visitor;

/** Literal double: Primary -> DOUBLE_LIT. */
public class DoubleLit extends Expr {
    public final String lexeme;

    public DoubleLit(String lexeme, int line, int col) {
        super(line, col);
        this.lexeme = lexeme;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitDoubleLit(this);
    }
}
