package ast;

import symbols.Symbol;
import visitor.Visitor;

/** Atribuição: Stmt -> ID StmtId, StmtId -> '=' Expr ';'. O name é um uso (o alvo). */
public class Assign extends Stmt {
    public final String name;
    public final Expr value;
    /** Decoração da identificação (CP4): o símbolo do alvo; null se o nome não foi declarado. */
    public Symbol decl;

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
