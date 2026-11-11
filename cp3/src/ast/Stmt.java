package ast;

/** Categoria sintática "comando": tudo o que pode aparecer dentro de um Bloco. */
public abstract class Stmt extends Node {
    protected Stmt(int line, int col) {
        super(line, col);
    }
}
