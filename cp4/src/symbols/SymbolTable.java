package symbols;

/**
 * Tabela de símbolos da Aula 11 (assinaturas sem alteração).
 *
 * Contrato:
 * - declare não substitui um símbolo existente: devolve false e a primeira declaração permanece;
 * - lookup e lookupLocal devolvem null quando não acham (nunca lançam exceção);
 * - o escopo global (nível 0) já existe quando a tabela é criada e não pode ser fechado;
 *   o comportamento de closeScope nesse caso é documentado em cada implementação.
 */
public interface SymbolTable {
    void openScope();                 // entra em um bloco
    void closeScope();                // sai do bloco
    boolean declare(Symbol s);        // false: já existe neste escopo
    Symbol lookupLocal(String name);  // só no escopo do topo
    Symbol lookup(String name);       // do mais interno ao mais externo
    int level();                      // profundidade atual (0 = global)
}

