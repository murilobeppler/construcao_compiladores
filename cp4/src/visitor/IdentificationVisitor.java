package visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ast.*;
import symbols.Categoria;
import symbols.ContextualError;
import symbols.PilhaDeEscopos;
import symbols.Symbol;
import symbols.SymbolTable;

/**
 * Passada de identificação (Aula 11 e Checkpoint 4).
 *
 * Abre e fecha escopos, declara os nomes na tabela e decora a AST: cada identificador de
 * uso (Id, Assign, Call) recebe o Symbol da sua declaração em decl.
 *
 * Decisões de escopo adotadas (ver escopo-design.md, seção 4):
 * 1. Sombreamento: permitido entre escopos diferentes; no mesmo escopo é redeclaração (erro).
 * 2. Ordem: variáveis exigem declaração antes do uso (a inicialização avalia antes de o
 *    nome entrar na tabela); funções e procedimentos sofrem uma passada prévia de
 *    cabeçalhos, podendo ser chamadas antes do texto da declaração.
 * 3. Recursão mútua: permitida entre funções e procedimentos (consequência da passada prévia).
 * 4. Parâmetros e corpo: compartilham o mesmo escopo (um parâmetro e uma variável local
 *    de mesmo nome no bloco mais externo da função configuram redeclaração).
 * 5. Espaço de nomes único: variáveis e funções dividem a mesma tabela de símbolos.
 *
 * Nunca usa instanceof nem switch sobre o tipo do nó.
 * Em nenhuma entrada válida ou inválida lança NullPointerException ou entra em laço infinito.
 */
public class IdentificationVisitor implements Visitor<Void> {

    private final SymbolTable table;
    private final List<ContextualError> errors = new ArrayList<>();

    public IdentificationVisitor() {
        this(new PilhaDeEscopos());
    }

    public IdentificationVisitor(SymbolTable table) {
        this.table = table;
        instalaAmbienteInicial();
    }

    /**
     * Símbolos predefinidos da linguagem.
     * 'escreve' é um procedimento predefinido (usado nos testes e programas do grupo).
     */
    private void instalaAmbienteInicial() {
        table.declare(new Symbol("escreve", Categoria.PROCEDIMENTO, null, table.level()));
    }

    /** Devolve a lista de erros acumulados, ordenada por linha e coluna. */
    public List<ContextualError> getErrors() {
        List<ContextualError> copia = new ArrayList<>(errors);
        copia.sort((a, b) -> {
            int c = Integer.compare(a.line, b.line);
            return c != 0 ? c : Integer.compare(a.col, b.col);
        });
        return Collections.unmodifiableList(copia);
    }

    public boolean temErros() {
        return !errors.isEmpty();
    }

    /** Ponto de entrada: decora a AST e devolve a lista de erros contextuais. */
    public static List<ContextualError> run(Program prog) {
        IdentificationVisitor v = new IdentificationVisitor();
        if (prog != null) {
            prog.accept(v);
        }
        return v.getErrors();
    }

    // ---------- Passada prévia de funções/procedimentos ----------

    /**
     * Passada prévia: declara os nomes das funções e procedimentos no escopo global
     * antes de analisar os corpos e as variáveis, viabilizando recursão mútua e
     * chamadas textualmente anteriores à definição. Usa double dispatch (sem instanceof).
     */
    private class PassadaPreviaSubrotinas extends VisitorVazio {
        @Override
        public Void visitFuncDecl(FuncDecl n) {
            declaraNomeGlobal(n.name, Categoria.FUNCAO, n);
            return null;
        }

        @Override
        public Void visitProcDecl(ProcDecl n) {
            declaraNomeGlobal(n.name, Categoria.PROCEDIMENTO, n);
            return null;
        }

        private void declaraNomeGlobal(String nome, Categoria cat, Node n) {
            Symbol s = new Symbol(nome, cat, n, table.level());
            if (!table.declare(s)) {
                Symbol anterior = table.lookupLocal(nome);
                reportaRedeclaracao(n.line, n.col, nome, anterior);
            }
        }
    }

    // ---------- Helpers ----------

    private void reportaRedeclaracao(int line, int col, String nome, Symbol anterior) {
        String onde = anterior == null ? "anterior"
                : (anterior.isPredefinido() ? "predefinido" : "linha " + anterior.line());
        errors.add(new ContextualError(line, col,
                "'" + nome + "' já declarado neste escopo (" + onde + ")"));
    }

    private void reportaNaoDeclarado(int line, int col, String nome) {
        errors.add(new ContextualError(line, col, "'" + nome + "' não declarado"));
    }

    // ---------- Programa e declarações ----------

    @Override
    public Void visitProgram(Program n) {
        if (n.decls == null) {
            return null;
        }
        // 1. Passada prévia: coleta cabeçalhos de funções e procedimentos
        PassadaPreviaSubrotinas previa = new PassadaPreviaSubrotinas();
        for (TopDecl d : n.decls) {
            if (d != null) {
                d.accept(previa);
            }
        }
        // 2. Passada principal: variáveis globais e corpos de sub-rotinas
        for (TopDecl d : n.decls) {
            if (d != null) {
                d.accept(this);
            }
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl n) {
        // Declarar antes de usar: a inicialização é avaliada antes de declarar a variável.
        // Assim, "int x = x;" usa o x do escopo externo (ou erro se não houver).
        if (n.init != null) {
            n.init.accept(this);
        }
        Symbol s = new Symbol(n.name, Categoria.VARIAVEL, n, table.level());
        if (!table.declare(s)) {
            Symbol anterior = table.lookupLocal(n.name);
            reportaRedeclaracao(n.line, n.col, n.name, anterior);
        }
        return null;
    }

    @Override
    public Void visitFuncDecl(FuncDecl n) {
        // O nome da função já foi declarado no escopo global pela passada prévia.
        // Abre o escopo que parâmetros e corpo mais externo compartilham (Decisão 4).
        table.openScope();
        try {
            if (n.params != null) {
                for (Param p : n.params) {
                    if (p != null) {
                        p.accept(this);
                    }
                }
            }
            if (n.body != null && n.body.stmts != null) {
                for (Stmt s : n.body.stmts) {
                    if (s != null) {
                        s.accept(this);
                    }
                }
            }
        } finally {
            table.closeScope();
        }
        return null;
    }

    @Override
    public Void visitProcDecl(ProcDecl n) {
        table.openScope();
        try {
            if (n.params != null) {
                for (Param p : n.params) {
                    if (p != null) {
                        p.accept(this);
                    }
                }
            }
            if (n.body != null && n.body.stmts != null) {
                for (Stmt s : n.body.stmts) {
                    if (s != null) {
                        s.accept(this);
                    }
                }
            }
        } finally {
            table.closeScope();
        }
        return null;
    }

    @Override
    public Void visitParam(Param n) {
        Symbol s = new Symbol(n.name, Categoria.PARAMETRO, n, table.level());
        if (!table.declare(s)) {
            Symbol anterior = table.lookupLocal(n.name);
            reportaRedeclaracao(n.line, n.col, n.name, anterior);
        }
        return null;
    }

    @Override
    public Void visitType(TypeNode n) {
        return null;
    }

    // ---------- Comandos ----------

    @Override
    public Void visitBlock(Block n) {
        table.openScope();
        try {
            if (n.stmts != null) {
                for (Stmt s : n.stmts) {
                    if (s != null) {
                        s.accept(this);
                    }
                }
            }
        } finally {
            table.closeScope();
        }
        return null;
    }

    @Override
    public Void visitAssign(Assign n) {
        Symbol s = table.lookup(n.name);
        if (s != null) {
            n.decl = s;
        } else {
            n.decl = null;
            reportaNaoDeclarado(n.line, n.col, n.name);
        }
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
    public Void visitIntLit(IntLit n) {
        return null;
    }

    @Override
    public Void visitDoubleLit(DoubleLit n) {
        return null;
    }

    @Override
    public Void visitBoolLit(BoolLit n) {
        return null;
    }

    @Override
    public Void visitCharLit(CharLit n) {
        return null;
    }

    @Override
    public Void visitStringLit(StringLit n) {
        return null;
    }

    @Override
    public Void visitId(Id n) {
        Symbol s = table.lookup(n.name);
        if (s != null) {
            n.decl = s;
        } else {
            n.decl = null;
            reportaNaoDeclarado(n.line, n.col, n.name);
        }
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
        Symbol s = table.lookup(n.name);
        if (s != null) {
            n.decl = s;
        } else {
            n.decl = null;
            reportaNaoDeclarado(n.line, n.col, n.name);
        }
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

