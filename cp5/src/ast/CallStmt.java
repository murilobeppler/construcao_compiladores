package ast;

import visitor.Visitor;

/** Chamada usada como comando: Stmt -> ID StmtId, StmtId -> '(' Args ')' ';'. */
public class CallStmt extends Stmt {
    public final Call call;

    public CallStmt(Call call, int line, int col) {
        super(line, col);
        this.call = call;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitCallStmt(this);
    }
}
