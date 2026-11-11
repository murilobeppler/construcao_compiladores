package ast;

import visitor.Visitor;

/** Uso de variável numa expressão: Primary -> ID PrimaryIdRest, PrimaryIdRest -> ε. (Futuro: campo para o símbolo.) */
public class Id extends Expr {
    public final String name;

    public Id(String name, int line, int col) {
        super(line, col);
        this.name = name;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitId(this);
    }
}
