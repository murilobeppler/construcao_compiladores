package ast;

import java.util.List;

import symbols.Symbol;
import visitor.Visitor;

/** Chamada de função: Primary -> ID PrimaryIdRest, PrimaryIdRest -> '(' Args ')'. O name é um uso. */
public class Call extends Expr {
    public final String name;
    public final List<Expr> args;
    /** Decoração da identificação (CP4): o símbolo da subrotina chamada; null se não foi declarada. */
    public Symbol decl;

    public Call(String name, List<Expr> args, int line, int col) {
        super(line, col);
        this.name = name;
        this.args = args;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitCall(this);
    }
}
