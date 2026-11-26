package ast;

import java.util.List;

import visitor.Visitor;

/** Bloco: Bloco -> '{' ListaStmt '}'. */
public class Block extends Stmt {
    public final List<Stmt> stmts;

    public Block(List<Stmt> stmts, int line, int col) {
        super(line, col);
        this.stmts = stmts;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitBlock(this);
    }
}
