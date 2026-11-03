package visitor;

import java.util.List;

import ast.*;

public class TreePrinter implements Visitor<Void> {

    private final StringBuilder sb = new StringBuilder();
    private int nivel = 0;

    /** Atalho: imprime a árvore a partir de qualquer nó. */
    public static String print(Node raiz) {
        TreePrinter p = new TreePrinter();
        p.filho(raiz);
        return p.sb.toString();
    }

    /** Atalho para declarações de nível superior (interface TopDecl). */
    public static String print(TopDecl raiz) {
        TreePrinter p = new TreePrinter();
        p.filho(raiz);
        return p.sb.toString();
    }

    // ---------- utilitários ----------

    private void linha(String texto) {
        for (int i = 0; i < nivel; i++) {
            sb.append("  ");
        }
        sb.append(texto).append('\n');
    }

    /** Visita um filho um nível abaixo. Um filho nulo (árvore com erro) aparece como "?". */
    private void filho(Node n) {
        if (n == null) {
            linha("?");
        } else {
            n.accept(this);
        }
    }

    private void filho(TopDecl n) {
        if (n == null) {
            linha("?");
        } else {
            n.accept(this);
        }
    }

    private void abre(String texto) {
        linha(texto);
        nivel++;
    }

    private void fecha() {
        nivel--;
    }

    private static String tipo(TypeNode t) {
        return t == null || t.tipo == null ? "?" : t.tipo.lexema();
    }

    private void filhos(List<? extends Node> nos) {
        if (nos == null) {
            return;
        }
        for (Node n : nos) {
            filho(n);
        }
    }

    // ---------- programa e declarações ----------

    @Override
    public Void visitProgram(Program n) {
        abre("Program");
        if (n.decls != null) {
            for (TopDecl d : n.decls) {
                filho(d);
            }
        }
        fecha();
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl n) {
        abre("VarDecl(" + tipo(n.type) + " " + n.name + ")");
        if (n.init != null) {
            filho(n.init);
        }
        fecha();
        return null;
    }

    @Override
    public Void visitFuncDecl(FuncDecl n) {
        abre("FuncDecl(" + tipo(n.returnType) + " " + n.name + ")");
        filhos(n.params);
        filho(n.body);
        fecha();
        return null;
    }

    @Override
    public Void visitProcDecl(ProcDecl n) {
        abre("ProcDecl(" + n.name + ")");
        filhos(n.params);
        filho(n.body);
        fecha();
        return null;
    }

    @Override
    public Void visitParam(Param n) {
        linha("Param(" + tipo(n.type) + " " + n.name + ")");
        return null;
    }

    @Override
    public Void visitType(TypeNode n) {
        // nas declarações o tipo é impresso ao lado do nome; esta linha só
        // aparece se alguém imprimir um TypeNode sozinho
        linha("Type(" + tipo(n) + ")");
        return null;
    }

    // ---------- comandos ----------

    @Override
    public Void visitBlock(Block n) {
        abre("Block");
        filhos(n.stmts);
        fecha();
        return null;
    }

    @Override
    public Void visitAssign(Assign n) {
        abre("Assign(" + n.name + ")");
        filho(n.value);
        fecha();
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt n) {
        abre("CallStmt");
        filho(n.call);
        fecha();
        return null;
    }

    @Override
    public Void visitIf(If n) {
        abre("If");
        filho(n.cond);
        filho(n.thenBlock);
        if (n.elseBranch != null) {
            filho(n.elseBranch);
        }
        fecha();
        return null;
    }

    @Override
    public Void visitWhile(While n) {
        abre("While");
        filho(n.cond);
        filho(n.body);
        fecha();
        return null;
    }

    @Override
    public Void visitReturn(Return n) {
        abre("Return");
        if (n.value != null) {
            filho(n.value);
        }
        fecha();
        return null;
    }

    // ---------- expressões ----------

    @Override
    public Void visitIntLit(IntLit n) {
        linha("Int(" + n.lexeme + ")");
        return null;
    }

    @Override
    public Void visitDoubleLit(DoubleLit n) {
        linha("Double(" + n.lexeme + ")");
        return null;
    }

    @Override
    public Void visitBoolLit(BoolLit n) {
        linha("Bool(" + n.value + ")");
        return null;
    }

    @Override
    public Void visitCharLit(CharLit n) {
        linha("Char('" + n.valor + "')");
        return null;
    }

    @Override
    public Void visitStringLit(StringLit n) {
        linha("Str(\"" + n.valor + "\")");
        return null;
    }

    @Override
    public Void visitId(Id n) {
        linha("Id(" + n.name + ")");
        return null;
    }

    @Override
    public Void visitBin(Bin n) {
        abre("Bin(" + (n.op == null ? "?" : n.op.simbolo()) + ")");
        filho(n.left);
        filho(n.right);
        fecha();
        return null;
    }

    @Override
    public Void visitUnary(Unary n) {
        abre("Un(" + (n.op == null ? "?" : n.op.simbolo()) + ")");
        filho(n.operand);
        fecha();
        return null;
    }

    @Override
    public Void visitCall(Call n) {
        abre("Call(" + n.name + ")");
        filhos(n.args);
        fecha();
        return null;
    }
}
