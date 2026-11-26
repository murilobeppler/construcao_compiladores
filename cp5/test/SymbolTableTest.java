import symbols.Categoria;
import symbols.PilhaDeEscopos;
import symbols.Symbol;
import symbols.SymbolTable;

public class SymbolTableTest {

    private static int ok = 0;
    private static int falhas = 0;

    private static void check(String nome, Object esperado, Object obtido) {
        if ((esperado == null && obtido == null) || (esperado != null && esperado.equals(obtido))) {
            ok++;
            System.out.println("[OK]     " + nome);
        } else {
            falhas++;
            System.out.println("[FALHA]  " + nome);
            System.out.println("           esperado: " + esperado);
            System.out.println("           obtido:   " + obtido);
        }
    }

    private static void assertTrue(String nome, boolean cond) {
        check(nome, true, cond);
    }

    private static void assertFalse(String nome, boolean cond) {
        check(nome, false, cond);
    }

    public static void main(String[] args) {
        System.out.println("=== Testes da SymbolTable (PilhaDeEscopos) ===");

        testDeclararEBuscar();
        testSombreamentoERestauracao();
        testRedeclaracao();
        testLookupLocalNaoEnxergaExterno();
        testLookupInexistente();
        testNivel();
        testCloseScopeSemEscopo();

        System.out.println("=== Resumo SymbolTable: " + ok + " ok, " + falhas + " falhas ===");
        if (falhas > 0) {
            System.exit(1);
        }
    }

    static void testDeclararEBuscar() {
        SymbolTable table = new PilhaDeEscopos();
        Symbol s = new Symbol("x", Categoria.VARIAVEL, null, table.level());
        assertTrue("declarar 'x' no nível 0", table.declare(s));
        check("lookup acha 'x'", s, table.lookup("x"));
        check("lookupLocal acha 'x'", s, table.lookupLocal("x"));
    }

    static void testSombreamentoERestauracao() {
        SymbolTable table = new PilhaDeEscopos();
        Symbol x0 = new Symbol("x", Categoria.VARIAVEL, null, 0);
        table.declare(x0);

        table.openScope(); // nível 1
        Symbol x1 = new Symbol("x", Categoria.VARIAVEL, null, 1);
        assertTrue("declarar 'x' no nível 1 (sombreamento)", table.declare(x1));
        check("lookup acha 'x' interno (nível 1)", x1, table.lookup("x"));
        check("lookupLocal acha 'x' interno (nível 1)", x1, table.lookupLocal("x"));

        table.closeScope(); // volta ao nível 0
        check("após closeScope, lookup restaura 'x' do nível 0", x0, table.lookup("x"));
        check("lookupLocal acha 'x' do nível 0", x0, table.lookupLocal("x"));
    }

    static void testRedeclaracao() {
        SymbolTable table = new PilhaDeEscopos();
        Symbol s1 = new Symbol("a", Categoria.VARIAVEL, null, 0);
        Symbol s2 = new Symbol("a", Categoria.FUNCAO, null, 0);

        assertTrue("primeira declaração de 'a' aceita", table.declare(s1));
        assertFalse("segunda declaração de 'a' no mesmo escopo devolve false", table.declare(s2));
        check("o primeiro símbolo permanece inalterado", s1, table.lookup("a"));
    }

    static void testLookupLocalNaoEnxergaExterno() {
        SymbolTable table = new PilhaDeEscopos();
        Symbol ext = new Symbol("globalVar", Categoria.VARIAVEL, null, 0);
        table.declare(ext);

        table.openScope(); // nível 1
        check("lookup enxerga escopo externo", ext, table.lookup("globalVar"));
        check("lookupLocal NÃO enxerga escopo externo (devolve null)", null, table.lookupLocal("globalVar"));
    }

    static void testLookupInexistente() {
        SymbolTable table = new PilhaDeEscopos();
        table.openScope();
        check("lookup de nome inexistente devolve null sem lançar exceção", null, table.lookup("fantasma"));
        check("lookupLocal de nome inexistente devolve null sem lançar exceção", null, table.lookupLocal("fantasma"));
        check("lookup de null devolve null", null, table.lookup(null));
        check("lookupLocal de null devolve null", null, table.lookupLocal(null));
    }

    static void testNivel() {
        SymbolTable table = new PilhaDeEscopos();
        check("nível inicial é 0 (global)", 0, table.level());
        table.openScope();
        check("após 1 openScope, nível é 1", 1, table.level());
        table.openScope();
        check("após 2 openScopes, nível é 2", 2, table.level());
        table.closeScope();
        check("após closeScope, nível volta a 1", 1, table.level());
        table.closeScope();
        check("após fechar tudo, nível volta a 0", 0, table.level());
    }

    static void testCloseScopeSemEscopo() {
        SymbolTable table = new PilhaDeEscopos();
        boolean lancou = false;
        try {
            table.closeScope();
        } catch (IllegalStateException e) {
            lancou = true;
            assertTrue("mensagem de erro explicativa no closeScope indevido",
                    e.getMessage().contains("escopo global") || e.getMessage().contains("nível 0"));
        } catch (Exception e) {
            lancou = false;
        }
        assertTrue("closeScope no nível 0 lança IllegalStateException documentada", lancou);
    }
}

