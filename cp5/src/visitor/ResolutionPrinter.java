package visitor;

import java.util.ArrayList;
import java.util.List;

import ast.*;

/**
 * Imprime a resolução de todos os identificadores de uso da AST na ordem do texto:
 *   "linha:col <nome> -> x@5"   (ou "?")
 *
 * Útil para conferir a saída da passada de identificação contra o desenho do ambiente.
 */
public class ResolutionPrinter implements Visitor<Void> {

    private final List<String> resolucoes = new ArrayList<>();

    public static List<String> resolucoesDe(Node raiz) {
        ResolutionPrinter p = new ResolutionPrinter();
        if (raiz != null) {
            raiz.accept(p);
        }
        return p.resolucoes;
    }

    public static String print(Node raiz) {
        return String.join("\n", resolucoesDe(raiz));
    }

    private void registra(int line, int col, String nome, symbols.Symbol s) {
        String destino = s == null ? "?" : s.toString();
        resolucoes.add(line + ":" + col + " " + nome + " -> " + destino);
    }

    // ---------- Programa e declarações ----------

    @Override
    public Void visitProgram(Program n) {
        if (n.decls != null) {
            for (TopDecl d : n.decls) {
                if (d != null) {
                    d.accept(this);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl n) {
        if (n.init != null) {
            n.init.accept(this);
        }
        return null;
    }

    @Override
    public Void visitFuncDecl(FuncDecl n) {
        if (n.body != null) {
            n.body.accept(this);
        }
        return null;
    }

    @Override
    public Void visitProcDecl(ProcDecl n) {
        if (n.body != null) {
            n.body.accept(this);
        }
        return null;
    }

    @Override
    public Void visitParam(Param n) {
        return null;
    }

    @Override
    public Void visitType(TypeNode n) {
        return null;
    }

    // ---------- Comandos ----------

    @Override
    public Void visitBlock(Block n) {
        if (n.stmts != null) {
            for (Stmt s : n.stmts) {
                if (s != null) {
                    s.accept(this);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitAssign(Assign n) {
        registra(n.line, n.col, n.name, n.decl);
        if (n.value != null) {
            n.value.accept(this);
        }
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt n) {
        if (n.call != null) {
            n.call.accept(this);
        }
        return null;
    }

    @Override
    public Void visitIf(If n) {
        if (n.cond != null) {
            n.cond.accept(this);
        }
        if (n.thenBlock != null) {
            n.thenBlock.accept(this);
        }
        if (n.elseBranch != null) {
            n.elseBranch.accept(this);
        }
        return null;
    }

    @Override
    public Void visitWhile(While n) {
        if (n.cond != null) {
            n.cond.accept(this);
        }
        if (n.body != null) {
            n.body.accept(this);
        }
        return null;
    }

    @Override
    public Void visitReturn(Return n) {
        if (n.value != null) {
            n.value.accept(this);
        }
        return null;
    }

    // ---------- Expressões ----------

    @Override
    public Void visitIntLit(IntLit n) { return null; }
    @Override
    public Void visitDoubleLit(DoubleLit n) { return null; }
    @Override
    public Void visitBoolLit(BoolLit n) { return null; }
    @Override
    public Void visitCharLit(CharLit n) { return null; }
    @Override
    public Void visitStringLit(StringLit n) { return null; }

    @Override
    public Void visitId(Id n) {
        registra(n.line, n.col, n.name, n.decl);
        return null;
    }

    @Override
    public Void visitBin(Bin n) {
        if (n.left != null) {
            n.left.accept(this);
        }
        if (n.right != null) {
            n.right.accept(this);
        }
        return null;
    }

    @Override
    public Void visitUnary(Unary n) {
        if (n.operand != null) {
            n.operand.accept(this);
        }
        return null;
    }

    @Override
    public Void visitCall(Call n) {
        registra(n.line, n.col, n.name, n.decl);
        if (n.args != null) {
            for (Expr a : n.args) {
                if (a != null) {
                    a.accept(this);
                }
            }
        }
        return null;
    }
}

