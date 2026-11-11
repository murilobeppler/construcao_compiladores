package ast;

import visitor.Visitor;

/** Uso de um tipo básico numa declaração (Tipo -> 'int' | ... | 'string'). */
public class TypeNode extends Node {
    public final Tipo tipo;

    public TypeNode(Tipo tipo, int line, int col) {
        super(line, col);
        this.tipo = tipo;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitType(this);
    }
}
