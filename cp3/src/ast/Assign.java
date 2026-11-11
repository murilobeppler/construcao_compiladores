package ast;

import visitor.Visitor;

/** Atribuição: Stmt -> ID StmtId, StmtId -> '=' Expr ';'. (Futuro: campo para o símbolo.) */
public class Assign extends Stmt {
    public final String name;
    public final Expr value;

    public Assign(String name, Expr value, int line, int col) {
        super(line, col);
        this.name = name;
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitAssign(this);
    }
}
