import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ast.Expr;
import ast.Program;
import ast.VarDecl;
import visitor.SourcePrinter;
import visitor.TreePrinter;

public class AstTest {

    private static int ok = 0;
    private static int falhas = 0;

    private static final String[] PROGRAMAS = {"prog1-tipos", "prog2-controle", "prog3-funcoes"};

    // ---------- utilitários ----------

    private static String ler(String caminho) {
        try {
            return new String(Files.readAllBytes(Path.of(caminho)), "UTF-8").replace("\r\n", "\n");
        } catch (IOException e) {
            return "<não consegui ler " + caminho + ">";
        }
    }

    private static ParseResult analisa(String fonte) {
        return new Parser(new Scanner(fonte)).parse();
    }

    /** Árvore da expressão e, lida como inicialização de "int x = e;". */
    private static String arvoreDe(String expr) {
        ParseResult r = analisa("int x = " + expr + ";");
        if (!r.aceito) {
            return "<rejeitado: " + r.syntaxErrors + ">";
        }
        Expr e = ((VarDecl) r.program.decls.get(0)).init;
        return TreePrinter.print(e);
    }

    private static void check(String nome, Object esperado, Object obtido) {
        if (esperado.equals(obtido)) {
            ok++;
            System.out.println("[OK]     " + nome);
        } else {
            falhas++;
            System.out.println("[FALHA]  " + nome);
            System.out.println("           esperado:\n" + esperado);
            System.out.println("           obtido:\n" + obtido);
        }
    }

    

    static void testArvoresEsperadas() {
        for (String nome : PROGRAMAS) {
            ParseResult r = analisa(ler("test/regressao-cp3/programas/" + nome + ".txt"));
            String obtido = r.aceito ? TreePrinter.print(r.program) : "<rejeitado: " + r.syntaxErrors + ">";
            check("árvore de " + nome + " = test/regressao-cp3/esperado/" + nome + ".txt",
                    ler("test/regressao-cp3/esperado/" + nome + ".txt"), obtido);
        }
    }

    

    static void testPrecedencia() {
        check("precedência: a + b * c  =>  a + (b * c)",
                ler("test/regressao-cp3/esperado/prec-soma-mult.txt"), arvoreDe("a + b * c"));
        check("associatividade à esquerda: a - b - c  =>  (a - b) - c",
                ler("test/regressao-cp3/esperado/assoc-sub-sub.txt"), arvoreDe("a - b - c"));
        check("parênteses mudam a árvore: a - (b - c)",
                ler("test/regressao-cp3/esperado/paren-muda-arvore.txt"), arvoreDe("a - (b - c)"));
        check("as três árvores acima são diferentes entre si",
                3, new java.util.HashSet<>(java.util.Arrays.asList(
                        arvoreDe("a + b * c"), arvoreDe("a - b - c"), arvoreDe("a - (b - c)"))).size());
        check("exemplo do enunciado: a - b - c * d",
                ler("test/regressao-cp3/esperado/enunciado-exemplo.txt"), arvoreDe("a - b - c * d"));
    }

    

    static void testParentesesRedundantes() {
        check("((a)) vira só Id(a), sem nó de parênteses",
                ler("test/regressao-cp3/esperado/paren-redundante.txt"), arvoreDe("((a))"));
        check("((a)) produz a mesma árvore que a", arvoreDe("a"), arvoreDe("((a))"));
        check("((a + (b))) produz a mesma árvore que a + b", arvoreDe("a + b"), arvoreDe("((a + (b)))"));
    }

    

    static void testPosicao() {
        ParseResult r = analisa("void p() {\n    x = a - b * c;\n}");
        // Assign começa no 'x' (2:5); a Bin(-) começa no 'a' (2:9), seu primeiro token
        ast.ProcDecl p = (ast.ProcDecl) r.program.decls.get(0);
        ast.Assign a = (ast.Assign) p.body.stmts.get(0);
        check("posição: ProcDecl em 1:1, Assign em 2:5, Bin(-) no primeiro token (2:9), Bin(*) em 2:13",
                "1:1 2:5 2:9 2:13",
                p.line + ":" + p.col + " " + a.line + ":" + a.col + " " + a.value.line + ":" + a.value.col
                        + " " + ((ast.Bin) a.value).right.line + ":" + ((ast.Bin) a.value).right.col);
    }

    

    static void testIdaEVolta() {
        String[][] casos = {
            {"prog1-tipos", ler("test/regressao-cp3/programas/prog1-tipos.txt")},
            {"prog2-controle", ler("test/regressao-cp3/programas/prog2-controle.txt")},
            {"prog3-funcoes", ler("test/regressao-cp3/programas/prog3-funcoes.txt")},
            {"exemplos/valido.txt", ler("exemplos/valido.txt")},
            {"programa vazio", ""},
            {"só comentários", "// só comentário\n/* e bloco */\n"},
            {"unários e else if encadeado",
                "void f() { int y = - -x; bool b = !!(a < b) == false;\n"
                + "if (a) { } else if (b) { } else if (c) { x = 1; } else { }\n"
                + "while (!(x >= 10)) { x = x + 1; } }"}
        };
        for (String[] c : casos) {
            ParseResult r1 = analisa(c[1]);
            if (!r1.aceito) {
                check("ida e volta: " + c[0] + " (original aceito)", "aceito", "rejeitado " + r1.syntaxErrors);
                continue;
            }
            String texto1 = SourcePrinter.print(r1.program);
            ParseResult r2 = analisa(texto1);
            if (!r2.aceito) {
                check("ida e volta: " + c[0] + " (reimpresso aceito)", "aceito",
                        "rejeitado " + r2.syntaxErrors + "\n" + texto1);
                continue;
            }
            String texto2 = SourcePrinter.print(r2.program);
            boolean mesmoTexto = texto1.equals(texto2);
            boolean mesmaArvore = TreePrinter.print(r1.program).equals(TreePrinter.print(r2.program));
            check("ida e volta: " + c[0] + " (texto2 == texto1 e mesma árvore)",
                    "true true", mesmoTexto + " " + mesmaArvore);
        }
    }

   
    static void mostraReimpressao() {
        Program p = analisa(ler("test/regressao-cp3/programas/prog2-controle.txt")).program;
        System.out.println("           reimpressão canônica de prog2-controle:");
        for (String l : SourcePrinter.print(p).split("\n")) {
            System.out.println("           | " + l);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Testes da AST (CP3) ===");
        testArvoresEsperadas();
        testPrecedencia();
        testParentesesRedundantes();
        testPosicao();
        testIdaEVolta();
        mostraReimpressao();
        System.out.println("=== Resumo: " + ok + " ok, " + falhas + " falhas ===");
        if (falhas > 0) {
            System.exit(1);
        }
    }
}
