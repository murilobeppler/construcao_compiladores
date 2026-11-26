package ast;

import visitor.Visitor;

/**
 * Marca os nós que podem aparecer no nível superior do programa:
 * VarDecl (variável global), FuncDecl e ProcDecl.
 */
public interface TopDecl {
    <T> T accept(Visitor<T> v);
}
