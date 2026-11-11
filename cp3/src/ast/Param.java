package ast;

import visitor.Visitor;

/** Parâmetro formal: Param -> Tipo ID. (Futuro: campo para o símbolo.) */
public class Param extends Node {
    public final TypeNode type;
    public final String name;

    public Param(TypeNode type, String name, int line, int col) {
        super(line, col);
        this.type = type;
        this.name = name;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitParam(this);
    }
}
