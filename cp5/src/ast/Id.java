package ast;

import symbols.Symbol;
import visitor.Visitor;

/** Uso de variável numa expressão: Primary -> ID PrimaryIdRest, PrimaryIdRest -> ε. */
public class Id extends Expr {
    public final String name;
    /** Decoração da identificação (CP4): o símbolo da declaração; null se o nome não foi declarado. */
    public Symbol decl;

    public Id(String name, int line, int col) {
        super(line, col);
        this.name = name;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitId(this);
    }
}
