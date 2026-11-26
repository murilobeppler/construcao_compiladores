package ast;

import visitor.Visitor;

/** Repetição: Stmt -> 'while' '(' Expr ')' Bloco. */
public class While extends Stmt {
    public final Expr cond;
    public final Block body;

    public While(Expr cond, Block body, int line, int col) {
        super(line, col);
        this.cond = cond;
        this.body = body;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitWhile(this);
    }
}
