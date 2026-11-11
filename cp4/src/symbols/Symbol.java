package symbols;

import ast.Node;
import ast.Tipo;

/**
 * Um nome declarado. Os campos e a fase que pergunta por cada um estão em escopo-design.md (seção 2).
 *
 * Os quatro primeiros campos são preenchidos pela passada de identificação (CP4) e não mudam mais.
 * type e offset já existem, mas ficam vazios até as fases que os calculam (Aulas 12 a 19),
 * para que essas fases os preencham sem mudar a forma desta classe.
 */
public class Symbol {
    /** O identificador, como escrito no fonte. */
    public final String name;
    /** Variável, parâmetro, função ou procedimento. */
    public final Categoria kind;
    /**
     * O nó da declaração: VarDecl, Param, FuncDecl ou ProcDecl.
     * É null só nos símbolos predefinidos (ambiente inicial, ex.: escreve), que não têm nó.
     */
    public final Node node;
    /** Profundidade do escopo onde foi declarado (0 = global); igual a table.level() na criação. */
    public final int level;

    /** Reservado (CP5): tipo da variável/parâmetro ou tipo de retorno da função. null até lá. */
    public Tipo type;
    /** Reservado (geração de código): deslocamento no registro de ativação. -1 = ainda não alocado. */
    public int offset = -1;

    public Symbol(String name, Categoria kind, Node node, int level) {
        this.name = name;
        this.kind = kind;
        this.node = node;
        this.level = level;
    }

    /** Símbolo do ambiente inicial, sem nó de declaração no programa. */
    public boolean isPredefinido() {
        return node == null;
    }

    /** Linha da declaração (0 para predefinidos). */
    public int line() {
        return node == null ? 0 : node.line;
    }

    /** Formato do desenho: "x@5" (o x da linha 5) ou "escreve@pre" (predefinido). */
    @Override
    public String toString() {
        return name + "@" + (isPredefinido() ? "pre" : String.valueOf(line()));
    }
}

