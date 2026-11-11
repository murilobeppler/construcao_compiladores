package ast;

import visitor.Visitor;

/** Retorno: Stmt -> 'return' ReturnOpt ';'. */
public class Return extends Stmt {
    /** null em 'return;'. */
    public final Expr value;

    public Return(Expr value, int line, int col) {
        super(line, col);
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitReturn(this);
    }
}
