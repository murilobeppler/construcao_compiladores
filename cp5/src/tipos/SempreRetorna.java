package tipos;

import ast.*;
import visitor.Visitor;

/**
 * Análise de fluxo da decisão 9: "este comando termina, em todos os caminhos, com return?".
 * Conservadora (estrutural, sem avaliar condições):
 *
 *   return ...                       sempre retorna
 *   { c1 ... cn }                    sempre retorna se algum ci sempre retorna
 *   if (e) B1 else B2                sempre retorna se B1 e B2 sempre retornam
 *   if (e) B1   (sem else)           não (a condição pode ser falsa)
 *   while (e) B                      não (o laço pode não executar)
 *   demais comandos                  não
 *
 * Nunca usa instanceof: cada nó responde por double dispatch.
 */
public class SempreRetorna implements Visitor<Boolean> {

    public static boolean de(Stmt s) {
        if (s == null) {
            return false;
        }
        Boolean r = s.accept(new SempreRetorna());
        return r != null && r;
    }

    @Override public Boolean visitReturn(Return n) { return true; }

    @Override
    public Boolean visitBlock(Block n) {
        if (n.stmts != null) {
            for (Stmt s : n.stmts) {
                if (de(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Boolean visitIf(If n) {
        return n.elseBranch != null && de(n.thenBlock) && de(n.elseBranch);
    }

    @Override public Boolean visitWhile(While n) { return false; }
    @Override public Boolean visitVarDecl(VarDecl n) { return false; }
    @Override public Boolean visitAssign(Assign n) { return false; }
    @Override public Boolean visitCallStmt(CallStmt n) { return false; }

    // Os nós abaixo não são comandos; nunca são perguntados, mas o Visitor exige o método.
    @Override public Boolean visitProgram(Program n) { return false; }
    @Override public Boolean visitFuncDecl(FuncDecl n) { return false; }
    @Override public Boolean visitProcDecl(ProcDecl n) { return false; }
    @Override public Boolean visitParam(Param n) { return false; }
    @Override public Boolean visitType(TypeNode n) { return false; }
    @Override public Boolean visitIntLit(IntLit n) { return false; }
    @Override public Boolean visitDoubleLit(DoubleLit n) { return false; }
    @Override public Boolean visitBoolLit(BoolLit n) { return false; }
    @Override public Boolean visitCharLit(CharLit n) { return false; }
    @Override public Boolean visitStringLit(StringLit n) { return false; }
    @Override public Boolean visitId(Id n) { return false; }
    @Override public Boolean visitBin(Bin n) { return false; }
    @Override public Boolean visitUnary(Unary n) { return false; }
    @Override public Boolean visitCall(Call n) { return false; }
}
