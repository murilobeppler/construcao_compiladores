package ast;

import java.util.List;

import visitor.Visitor;

/** Programa: lista ordenada de declarações de nível superior. Programa -> DeclsTop EOF */
public class Program extends Node {
    public final List<TopDecl> decls;

    public Program(List<TopDecl> decls, int line, int col) {
        super(line, col);
        this.decls = decls;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitProgram(this);
    }
}
