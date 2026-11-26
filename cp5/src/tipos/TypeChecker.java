package tipos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ast.*;
import symbols.Categoria;
import symbols.ContextualError;
import symbols.Symbol;
import visitor.Visitor;

/**
 * Verificador de tipos (CP5). Roda depois da passada de identificação, sobre a AST decorada:
 * cada Id, Assign e Call já aponta para o Symbol da sua declaração em decl.
 *
 * - Expressões: o visit devolve o tipo e também o grava em Expr.tipo.
 * - Comandos: o visit devolve null; confere condição, atribuição, return e chamadas.
 * - Não declara nomes, não abre nem fecha escopos: a assinatura das sub-rotinas vem do Symbol.
 * - Sem cascata: uma expressão com erro tem tipo ERRO, compatível com tudo e sem nova mensagem.
 * - Acumula todas as mensagens (não para no primeiro erro).
 * - Avisos: uso de variável local que pode não ter valor (análise conservadora).
 *
 * As regras de operadores e de compatibilidade estão em RegrasDeTipos; aqui ficam as regras
 * de comandos, chamadas e identificadores. Cada visit indica, em comentário, a regra do papel.
 *
 * Não usa instanceof nem switch sobre o tipo do nó.
 */
public class TypeChecker implements Visitor<Tipo> {

    private final List<ContextualError> erros = new ArrayList<>();
    private final List<ContextualError> avisos = new ArrayList<>();

    // ---------- contexto ----------

    /** Sub-rotina corrente (para o return): nome e tipo de retorno (VOID em procedimento). */
    private String rotinaNome;
    private Tipo rotinaRetorno;
    private boolean rotinaEhProcedimento;
    /** Laços abertos. A linguagem não tem break; o contador fica para quando tiver. */
    private int lacosAbertos = 0;

    // ---------- análise de "variável sem valor" (avisos) ----------

    /** Variáveis locais que, com certeza, já receberam valor no ponto atual. */
    private Set<Symbol> comValor = new HashSet<>();
    /** VarDecl com inicializador: têm valor desde a declaração. */
    private final Set<Node> declaradasComValor = new HashSet<>();
    /** Para avisar uma vez só por variável. */
    private final Set<Symbol> jaAvisadas = new HashSet<>();

    // =====================================================================
    // Ponto de entrada e resultados
    // =====================================================================

    /** Verifica o programa (já identificado) e devolve o verificador com as mensagens. */
    public static TypeChecker run(Program prog) {
        TypeChecker tc = new TypeChecker();
        if (prog != null) {
            prog.accept(tc);
        }
        return tc;
    }

    public List<ContextualError> getErros() {
        return ordena(erros);
    }

    public List<ContextualError> getAvisos() {
        return ordena(avisos);
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }

    private static List<ContextualError> ordena(List<ContextualError> l) {
        List<ContextualError> c = new ArrayList<>(l);
        c.sort((a, b) -> {
            int x = Integer.compare(a.line, b.line);
            return x != 0 ? x : Integer.compare(a.col, b.col);
        });
        return Collections.unmodifiableList(c);
    }

    private void erro(Node n, String msg) {
        erros.add(new ContextualError(n.line, n.col, msg));
    }

    private void aviso(Node n, String msg) {
        avisos.add(new ContextualError(n.line, n.col, "aviso: " + msg));
    }

    // =====================================================================
    // Utilitários
    // =====================================================================

    /** Tipo de uma expressão (visita e grava); ERRO se o nó faltar. */
    private Tipo tipoDe(Expr e) {
        if (e == null) {
            return Tipo.ERRO;
        }
        Tipo t = e.accept(this);
        if (t == null) {
            t = Tipo.ERRO;
        }
        e.tipo = t;
        return t;
    }

    private void visita(Stmt s) {
        if (s != null) {
            s.accept(this);
        }
    }

    private static Tipo tipoDeclarado(TypeNode t) {
        return t == null || t.tipo == null ? Tipo.ERRO : t.tipo;
    }

    private static boolean ehRotina(Symbol s) {
        return s.kind == Categoria.FUNCAO || s.kind == Categoria.PROCEDIMENTO;
    }

    /** Só variáveis locais entram na análise de "sem valor" (decisão em tipos-design.md). */
    private static boolean rastreada(Symbol s) {
        return s != null && s.kind == Categoria.VARIAVEL && s.level >= 1;
    }

    private void recebeuValor(Symbol s) {
        if (rastreada(s)) {
            comValor.add(s);
        }
    }

    // =====================================================================
    // Programa e declarações
    // =====================================================================

    @Override
    public Tipo visitProgram(Program n) {
        if (n.decls != null) {
            for (TopDecl d : n.decls) {
                if (d != null) {
                    d.accept(this);
                }
            }
        }
        return null;
    }

    // [DECL-INIT]  e : T'   T' <= T   =>  T x = e;  ok
    @Override
    public Tipo visitVarDecl(VarDecl n) {
        Tipo declarado = tipoDeclarado(n.type);
        if (n.init != null) {
            Tipo t = tipoDe(n.init);
            if (!RegrasDeTipos.atribuivel(declarado, t)) {
                erro(n.init, "inicialização de '" + n.name + "': esperado " + declarado + ", veio " + t);
            }
            // o nó não guarda o seu Symbol (é o Symbol que aponta para o nó, Symbol.node);
            // por isso a variável inicializada é marcada pelo próprio nó (ver usoDeVariavel)
            declaradasComValor.add(n);
        }
        return null;
    }


    @Override
    public Tipo visitFuncDecl(FuncDecl n) {
        entraRotina(n.name, tipoDeclarado(n.returnType), false);
        visita(n.body);
        // [RETORNO-CAMINHOS] (decisão 9): função precisa terminar com return em todo caminho
        if (rotinaRetorno != Tipo.ERRO && !SempreRetorna.de(n.body)) {
            erro(n, "função '" + n.name + "' pode terminar sem 'return'");
        }
        saiRotina();
        return null;
    }

    @Override
    public Tipo visitProcDecl(ProcDecl n) {
        entraRotina(n.name, Tipo.VOID, true);
        visita(n.body);
        saiRotina();
        return null;
    }

    private void entraRotina(String nome, Tipo retorno, boolean procedimento) {
        rotinaNome = nome;
        rotinaRetorno = retorno;
        rotinaEhProcedimento = procedimento;
        comValor = new HashSet<>();
        declaradasComValor.clear();
        // parâmetros sempre têm valor (o chamador fornece) e não são rastreados
    }

    private void saiRotina() {
        rotinaNome = null;
        rotinaRetorno = null;
        rotinaEhProcedimento = false;
    }

    @Override
    public Tipo visitParam(Param n) {
        return null;
    }

    @Override
    public Tipo visitType(TypeNode n) {
        return null;
    }

    // =====================================================================
    // Comandos
    // =====================================================================

    @Override
    public Tipo visitBlock(Block n) {
        if (n.stmts != null) {
            for (Stmt s : n.stmts) {
                visita(s);
            }
        }
        return null;
    }

    // [ATRIB]  x : T   e : T'   T' <= T   x é variável ou parâmetro  =>  x = e;  ok
    @Override
    public Tipo visitAssign(Assign n) {
        Tipo t = tipoDe(n.value);
        Symbol s = n.decl;
        if (s == null) {
            return null;                    // não declarado: já reportado na identificação
        }
        if (ehRotina(s)) {
            erro(n, "'" + n.name + "' é " + s.kind.descricao() + ", não pode receber atribuição");
            return null;
        }
        if (!RegrasDeTipos.atribuivel(s.type, t)) {
            erro(n.value, "atribuição a '" + n.name + "': esperado " + s.type + ", veio " + t);
        }
        recebeuValor(s);
        return null;
    }

    // [CHAMADA-CMD]  como [CHAMADA], mas aceita procedimento; o valor de função é descartado
    @Override
    public Tipo visitCallStmt(CallStmt n) {
        if (n.call != null) {
            n.call.tipo = verificaChamada(n.call, true);
        }
        return null;
    }

    // [IF]  e : bool   B1 ok   B2 ok  =>  if (e) B1 else B2  ok
    @Override
    public Tipo visitIf(If n) {
        Tipo c = tipoDe(n.cond);
        if (c != Tipo.BOOL && c != Tipo.ERRO) {
            erro(n.cond, "condição de 'if' deve ser bool, mas é " + c);
        }
        Set<Symbol> antes = new HashSet<>(comValor);
        visita(n.thenBlock);
        Set<Symbol> depoisEntao = comValor;
        comValor = new HashSet<>(antes);
        visita(n.elseBranch);
        Set<Symbol> depoisSenao = comValor;
        // um ramo que sempre retorna não chega ao comando seguinte
        if (SempreRetorna.de(n.thenBlock)) {
            comValor = depoisSenao;
        } else if (n.elseBranch != null && SempreRetorna.de(n.elseBranch)) {
            comValor = depoisEntao;
        } else {
            depoisEntao.retainAll(depoisSenao);
            comValor = depoisEntao;
        }
        return null;
    }

    // [WHILE]  e : bool   B ok  =>  while (e) B  ok
    @Override
    public Tipo visitWhile(While n) {
        Tipo c = tipoDe(n.cond);
        if (c != Tipo.BOOL && c != Tipo.ERRO) {
            erro(n.cond, "condição de 'while' deve ser bool, mas é " + c);
        }
        Set<Symbol> antes = new HashSet<>(comValor);
        lacosAbertos++;
        visita(n.body);
        lacosAbertos--;
        comValor = antes;                   // o corpo pode não executar
        return null;
    }

    // [RETURN]       dentro de função T:  e : T'   T' <= T  =>  return e;  ok
    // [RETURN-VOID]  dentro de procedimento:              =>  return;    ok
    @Override
    public Tipo visitReturn(Return n) {
        Tipo t = n.value == null ? null : tipoDe(n.value);
        if (rotinaNome == null) {
            return null;                    // a gramática não permite return fora de sub-rotina
        }
        if (rotinaEhProcedimento) {
            if (n.value != null) {
                erro(n, "'return' com valor no procedimento '" + rotinaNome + "'");
            }
        } else if (n.value == null) {
            if (rotinaRetorno != Tipo.ERRO) {
                erro(n, "'return' sem valor na função '" + rotinaNome + "', que devolve " + rotinaRetorno);
            }
        } else if (!RegrasDeTipos.atribuivel(rotinaRetorno, t)) {
            erro(n.value, "'return' de '" + rotinaNome + "': esperado " + rotinaRetorno + ", veio " + t);
        }
        return null;
    }

    // =====================================================================
    // Expressões
    // =====================================================================

    // [LIT-INT]  n literal, n <= 32767  =>  n : int
    @Override
    public Tipo visitIntLit(IntLit n) {
        if (n.lexeme == null || new java.math.BigInteger(n.lexeme).compareTo(
                java.math.BigInteger.valueOf(RegrasDeTipos.MAX_INT)) > 0) {
            erro(n, "literal " + n.lexeme + " fora da faixa de int (máximo " + RegrasDeTipos.MAX_INT + ")");
        }
        return Tipo.INT;                    // o tipo é int mesmo assim: não gera cascata
    }

    // [LIT-DOUBLE] [LIT-BOOL] [LIT-CHAR] [LIT-STRING]
    @Override public Tipo visitDoubleLit(DoubleLit n) { return Tipo.DOUBLE; }
    @Override public Tipo visitBoolLit(BoolLit n) { return Tipo.BOOL; }
    @Override public Tipo visitCharLit(CharLit n) { return Tipo.CHAR; }
    @Override public Tipo visitStringLit(StringLit n) { return Tipo.STRING; }

    // [ID]  x declarado com tipo T, x é variável ou parâmetro  =>  x : T
    @Override
    public Tipo visitId(Id n) {
        Symbol s = n.decl;
        if (s == null) {
            return Tipo.ERRO;               // não declarado: já reportado na identificação
        }
        if (ehRotina(s)) {
            // decisão 11: nome de sub-rotina sem parênteses não é valor
            erro(n, "'" + n.name + "' é " + s.kind.descricao() + ": nome sem parênteses não é valor (use '"
                    + n.name + "(...)')");
            return Tipo.ERRO;
        }
        usoDeVariavel(n, s);
        return s.type == null ? Tipo.ERRO : s.type;
    }

    /** Aviso conservador: variável local lida sem ter recebido valor em todo caminho até aqui. */
    private void usoDeVariavel(Id n, Symbol s) {
        if (!rastreada(s) || comValor.contains(s) || declaradasComValor.contains(s.node)) {
            return;
        }
        if (jaAvisadas.add(s)) {
            aviso(n, "'" + n.name + "' pode ser usada sem valor");
        }
    }

    // [ARIT-*] [MOD] [REL-*] [IGUAL*] [LOG]: ver RegrasDeTipos.binario
    @Override
    public Tipo visitBin(Bin n) {
        Tipo a = tipoDe(n.left);
        Tipo b = tipoDe(n.right);
        Tipo r = RegrasDeTipos.binario(n.op, a, b);
        if (r == null) {
            erro(n, "operador '" + n.op.simbolo() + "' não se aplica a " + a + " e " + b);
            return Tipo.ERRO;
        }
        return r;
    }

    // [NOT] [NEG]: ver RegrasDeTipos.unario
    @Override
    public Tipo visitUnary(Unary n) {
        Tipo t = tipoDe(n.operand);
        Tipo r = RegrasDeTipos.unario(n.op, t);
        if (r == null) {
            erro(n, "operador '" + n.op.simbolo() + "' não se aplica a " + t);
            return Tipo.ERRO;
        }
        return r;
    }

    // [CHAMADA]  f : (T1,...,Tn) -> T,  T != void,  ei : Ti',  Ti' <= Ti  =>  f(e1,...,en) : T
    @Override
    public Tipo visitCall(Call n) {
        return verificaChamada(n, false);
    }

    /**
     * Confere uma chamada. comoComando: true em CallStmt (procedimento permitido, valor de
     * função descartado — decisão 6); false dentro de expressão (precisa devolver valor).
     */
    private Tipo verificaChamada(Call n, boolean comoComando) {
        List<Tipo> args = new ArrayList<>();
        if (n.args != null) {
            for (Expr a : n.args) {
                args.add(tipoDe(a));        // os argumentos são tipados sempre
            }
        }
        Symbol s = n.decl;
        if (s == null) {
            return Tipo.ERRO;               // não declarado: já reportado na identificação
        }
        if (!ehRotina(s)) {
            erro(n, "'" + n.name + "' é " + s.kind.descricao() + ", não pode ser chamado");
            return Tipo.ERRO;
        }
        if (!s.variadico) {
            List<Tipo> params = s.paramTypes == null ? new ArrayList<>() : s.paramTypes;
            if (params.size() != args.size()) {
                erro(n, "'" + n.name + "' espera " + params.size() + " argumento(s), recebeu " + args.size());
            } else {
                for (int i = 0; i < params.size(); i++) {
                    if (!RegrasDeTipos.atribuivel(params.get(i), args.get(i))) {
                        erro(n.args.get(i), "argumento " + (i + 1) + " de '" + n.name + "': esperado "
                                + params.get(i) + ", veio " + args.get(i));
                    }
                }
            }
        }
        if (s.kind == Categoria.PROCEDIMENTO) {
            if (!comoComando) {
                erro(n, "procedimento '" + n.name + "' usado em expressão (não devolve valor)");
                return Tipo.ERRO;
            }
            return Tipo.VOID;
        }
        return s.type == null ? Tipo.ERRO : s.type;
    }
}
