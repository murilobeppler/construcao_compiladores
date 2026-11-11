package ast;

import visitor.Visitor;

/** Seleção: Stmt -> 'if' '(' Expr ')' Bloco ElseOpt (e ElseTail -> 'if' ... no else if). */
public class If extends Stmt {
    public final Expr cond;
    public final Block thenBlock;
    /** null (sem else), um Block (else { }) ou outro If (else if). */
    public final Stmt elseBranch;

    public If(Expr cond, Block thenBlock, Stmt elseBranch, int line, int col) {
        super(line, col);
        this.cond = cond;
        this.thenBlock = thenBlock;
        this.elseBranch = elseBranch;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitIf(this);
    }
}
