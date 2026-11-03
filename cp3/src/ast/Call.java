package ast;

import java.util.List;

import visitor.Visitor;

/** Chamada de função: Primary -> ID PrimaryIdRest, PrimaryIdRest -> '(' Args ')'. (Futuro: campo para o símbolo.) */
public class Call extends Expr {
    public final String name;
    public final List<Expr> args;

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
