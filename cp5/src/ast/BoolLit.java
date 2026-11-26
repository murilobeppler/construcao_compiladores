package ast;

import visitor.Visitor;

/** Literal bool: Primary -> 'true' | 'false'. */
public class BoolLit extends Expr {
    public final boolean value;

    public BoolLit(boolean value, int line, int col) {
        super(line, col);
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitBoolLit(this);
    }
}
