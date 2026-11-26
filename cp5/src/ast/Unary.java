package ast;

import visitor.Visitor;

/** Operação unária: Unary -> '!' Unary | '-' Unary. */
public class Unary extends Expr {
    public final UnOp op;
    public final Expr operand;

    public Unary(UnOp op, Expr operand, int line, int col) {
        super(line, col);
        this.op = op;
        this.operand = operand;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitUnary(this);
    }
}
