package visitor;

import ast.*;

/**
 * Visitor que não faz nada em nenhum nó (nem desce nos filhos).
 * Base para visitors que só se interessam por poucos tipos de nó, como a passada prévia
 * da identificação, que só olha FuncDecl e ProcDecl: assim a escolha é feita pelo
 * double dispatch, sem instanceof.
 */
public abstract class VisitorVazio implements Visitor<Void> {
    @Override public Void visitProgram(Program n) { return null; }
    @Override public Void visitVarDecl(VarDecl n) { return null; }
    @Override public Void visitFuncDecl(FuncDecl n) { return null; }
    @Override public Void visitProcDecl(ProcDecl n) { return null; }
    @Override public Void visitParam(Param n) { return null; }
    @Override public Void visitType(TypeNode n) { return null; }
    @Override public Void visitBlock(Block n) { return null; }
    @Override public Void visitAssign(Assign n) { return null; }
    @Override public Void visitCallStmt(CallStmt n) { return null; }
    @Override public Void visitIf(If n) { return null; }
    @Override public Void visitWhile(While n) { return null; }
    @Override public Void visitReturn(Return n) { return null; }
    @Override public Void visitIntLit(IntLit n) { return null; }
    @Override public Void visitDoubleLit(DoubleLit n) { return null; }
    @Override public Void visitBoolLit(BoolLit n) { return null; }
    @Override public Void visitCharLit(CharLit n) { return null; }
    @Override public Void visitStringLit(StringLit n) { return null; }
    @Override public Void visitId(Id n) { return null; }
    @Override public Void visitBin(Bin n) { return null; }
    @Override public Void visitUnary(Unary n) { return null; }
    @Override public Void visitCall(Call n) { return null; }
}

