package ast;

import java.util.List;

import visitor.Visitor;

/** Função (com tipo de retorno): DeclTop -> Tipo ID DeclTopResto, DeclTopResto -> FuncResto. */
public class FuncDecl extends Node implements TopDecl {
    public final TypeNode returnType;
    public final String name;
    public final List<Param> params;
    public final Block body;

    public FuncDecl(TypeNode returnType, String name, List<Param> params, Block body, int line, int col) {
        super(line, col);
        this.returnType = returnType;
        this.name = name;
        this.params = params;
        this.body = body;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitFuncDecl(this);
    }
}
