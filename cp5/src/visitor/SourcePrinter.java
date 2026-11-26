package visitor;

import java.util.List;

import ast.*;

public class SourcePrinter implements Visitor<String> {

    private static final String IND = "    ";
    private int nivel = 0;

    public static String print(Node raiz) {
        return raiz.accept(new SourcePrinter());
    }

    private String ind() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < nivel; i++) {
            s.append(IND);
        }
        return s.toString();
    }

    private String lista(List<? extends Node> nos) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < nos.size(); i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append(nos.get(i).accept(this));
        }
        return s.toString();
    }

    // ---------- programa e declarações ----------

    @Override
    public String visitProgram(Program n) {
        StringBuilder s = new StringBuilder();
        for (TopDecl d : n.decls) {
            s.append(d.accept(this)).append('\n');
        }
        return s.toString();
    }

    @Override
    public String visitVarDecl(VarDecl n) {
        String s = n.type.accept(this) + " " + n.name;
        if (n.init != null) {
            s += " = " + n.init.accept(this);
        }
        return s + ";";
    }

    @Override
    public String visitFuncDecl(FuncDecl n) {
        return n.returnType.accept(this) + " " + n.name + "(" + lista(n.params) + ") "
                + n.body.accept(this);
    }

    @Override
    public String visitProcDecl(ProcDecl n) {
        return "void " + n.name + "(" + lista(n.params) + ") " + n.body.accept(this);
    }

    @Override
    public String visitParam(Param n) {
        return n.type.accept(this) + " " + n.name;
    }

    @Override
    public String visitType(TypeNode n) {
        return n.tipo.lexema();
    }

    // ---------- comandos ----------
    // Cada comando devolve o seu texto sem a indentação da primeira linha;
    // quem coloca a indentação é o Block que o contém.

    @Override
    public String visitBlock(Block n) {
        StringBuilder s = new StringBuilder("{\n");
        nivel++;
        for (Stmt st : n.stmts) {
            s.append(ind()).append(st.accept(this)).append('\n');
        }
        nivel--;
        s.append(ind()).append('}');
        return s.toString();
    }

    @Override
    public String visitAssign(Assign n) {
        return n.name + " = " + n.value.accept(this) + ";";
    }

    @Override
    public String visitCallStmt(CallStmt n) {
        return n.call.accept(this) + ";";
    }

    @Override
    public String visitIf(If n) {
        String s = "if (" + n.cond.accept(this) + ") " + n.thenBlock.accept(this);
        if (n.elseBranch != null) {
            // o ramo else é um Block ("else { ... }") ou outro If ("else if ...");
            // quem decide como ele se imprime é o próprio nó (double dispatch)
            s += " else " + n.elseBranch.accept(this);
        }
        return s;
    }

    @Override
    public String visitWhile(While n) {
        return "while (" + n.cond.accept(this) + ") " + n.body.accept(this);
    }

    @Override
    public String visitReturn(Return n) {
        return n.value == null ? "return;" : "return " + n.value.accept(this) + ";";
    }

    // ---------- expressões ----------

    @Override
    public String visitIntLit(IntLit n) {
        return n.lexeme;
    }

    @Override
    public String visitDoubleLit(DoubleLit n) {
        return n.lexeme;
    }

    @Override
    public String visitBoolLit(BoolLit n) {
        return n.value ? "true" : "false";
    }

    @Override
    public String visitCharLit(CharLit n) {
        return "'" + n.valor + "'";
    }

    @Override
    public String visitStringLit(StringLit n) {
        return "\"" + n.valor + "\"";
    }

    @Override
    public String visitId(Id n) {
        return n.name;
    }

    @Override
    public String visitBin(Bin n) {
        return "(" + n.left.accept(this) + " " + n.op.simbolo() + " " + n.right.accept(this) + ")";
    }

    @Override
    public String visitUnary(Unary n) {
        return "(" + n.op.simbolo() + n.operand.accept(this) + ")";
    }

    @Override
    public String visitCall(Call n) {
        return n.name + "(" + lista(n.args) + ")";
    }
}
