package ast;

import visitor.Visitor;

/** Literal int: Primary -> INT_LIT. */
public class IntLit extends Expr {
    /** Lexema do literal; a conversão para número é feita pela semântica. */
    public final String lexeme;

    public IntLit(String lexeme, int line, int col) {
        super(line, col);
        this.lexeme = lexeme;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitIntLit(this);
    }
}
