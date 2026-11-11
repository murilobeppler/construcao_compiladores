package symbols;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementação "pilha de mapas": um mapa nome -> símbolo por escopo aberto.
 * O índice 0 da pilha é o escopo global; o topo é o escopo mais interno.
 *
 * - openScope  empilha um mapa vazio                       O(1)
 * - closeScope desempilha o topo (os nomes somem juntos)   O(1)
 * - declare    consulta e grava só no topo                 O(1)
 * - lookup     procura do topo até o global                O(profundidade)
 *
 * closeScope no nível 0 (sem escopo aberto) lança IllegalStateException com mensagem
 * explicativa: o escopo global nunca é fechado. A passada de identificação sempre fecha
 * o que abriu (try/finally), então isso só acontece por erro de programação.
 */
public class PilhaDeEscopos implements SymbolTable {

    /** LinkedHashMap: a ordem de declaração se mantém, e toString() é determinístico. */
    private final List<Map<String, Symbol>> pilha = new ArrayList<>();

    public PilhaDeEscopos() {
        pilha.add(new LinkedHashMap<>());     // escopo global, nível 0
    }

    @Override
    public void openScope() {
        pilha.add(new LinkedHashMap<>());
    }

    @Override
    public void closeScope() {
        if (pilha.size() == 1) {
            throw new IllegalStateException(
                    "closeScope sem escopo aberto: o escopo global (nível 0) não pode ser fechado");
        }
        pilha.remove(pilha.size() - 1);
    }

    /** Devolve false (sem gravar) se s ou s.name for null, ou se o nome já existe no topo. */
    @Override
    public boolean declare(Symbol s) {
        if (s == null || s.name == null) {
            return false;
        }
        Map<String, Symbol> topo = topo();
        if (topo.containsKey(s.name)) {
            return false;                     // a primeira declaração permanece
        }
        topo.put(s.name, s);
        return true;
    }

    @Override
    public Symbol lookupLocal(String name) {
        if (name == null) {
            return null;
        }
        return topo().get(name);
    }

    @Override
    public Symbol lookup(String name) {
        if (name == null) {
            return null;
        }
        for (int i = pilha.size() - 1; i >= 0; i--) {
            Symbol s = pilha.get(i).get(name);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    @Override
    public int level() {
        return pilha.size() - 1;
    }

    private Map<String, Symbol> topo() {
        return pilha.get(pilha.size() - 1);
    }

    /**
     * O ambiente inteiro, do global ao topo, no formato do desenho:
     * "0{escreve@pre f@3 x@1} 1{n@3} 2{}".
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pilha.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(i).append('{');
            boolean primeiro = true;
            for (Symbol s : pilha.get(i).values()) {
                if (!primeiro) {
                    sb.append(' ');
                }
                sb.append(s);
                primeiro = false;
            }
            sb.append('}');
        }
        return sb.toString();
    }
}

