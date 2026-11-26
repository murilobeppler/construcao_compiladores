package ast;

import visitor.Visitor;

/** Operação binária: gerada pelos XxxRest (OrRest, AndRest, EqRest, RelRest, AddRest, MulRest). */
public class Bin extends Expr {
    public final BinOp op;
    public final Expr left;
    public final Expr right;

    public Bin(BinOp op, Expr left, Expr right, int line, int col) {
        super(line, col);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitBin(this);
    }
}
