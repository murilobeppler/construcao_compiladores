package ast;

import java.util.List;

import visitor.Visitor;

/** Procedimento (retorno void): DeclTop -> 'void' ID FuncResto. */
public class ProcDecl extends Node implements TopDecl {
    public final String name;
    public final List<Param> params;
    public final Block body;

    public ProcDecl(String name, List<Param> params, Block body, int line, int col) {
        super(line, col);
        this.name = name;
        this.params = params;
        this.body = body;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitProcDecl(this);
    }
}
