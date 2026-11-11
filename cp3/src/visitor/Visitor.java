package visitor;

import ast.*;

public interface Visitor<T> {
    // programa e declarações
    T visitProgram(Program n);
    T visitVarDecl(VarDecl n);
    T visitFuncDecl(FuncDecl n);
    T visitProcDecl(ProcDecl n);
    T visitParam(Param n);
    T visitType(TypeNode n);

    // comandos
    T visitBlock(Block n);
    T visitAssign(Assign n);
    T visitCallStmt(CallStmt n);
    T visitIf(If n);
    T visitWhile(While n);
    T visitReturn(Return n);

    // expressões
    T visitIntLit(IntLit n);
    T visitDoubleLit(DoubleLit n);
    T visitBoolLit(BoolLit n);
    T visitCharLit(CharLit n);
    T visitStringLit(StringLit n);
    T visitId(Id n);
    T visitBin(Bin n);
    T visitUnary(Unary n);
    T visitCall(Call n);
}
