package ast;

import visitor.Visitor;

/** Declaração de variável, global (DeclTop) ou local (Stmt). (Declaração: o Symbol aponta para este nó em Symbol.node.) */
public class VarDecl extends Stmt implements TopDecl {
    public final TypeNode type;
    public final String name;
    /** Inicialização; null quando não há '= Expr'. */
    public final Expr init;

    public VarDecl(TypeNode type, String name, Expr init, int line, int col) {
        super(line, col);
        this.type = type;
        this.name = name;
        this.init = init;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitVarDecl(this);
    }
}
