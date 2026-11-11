import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParserTest {

    private static int ok = 0;
    private static int falhas = 0;

    // ---------- utilitários ----------

    static class Resultado {
        boolean aceito;
        List<SyntaxError> sintaticos;
        List<LexicalError> lexicos;
    }

    private static Resultado analisa(String fonte) {
        Parser p = new Parser(new Scanner(fonte));
        Resultado r = new Resultado();
        r.aceito = p.parse();
        r.sintaticos = p.getErrors();
        r.lexicos = p.getLexicalErrors();
        return r;
    }

    /** Roda a análise numa thread com limite de tempo: null = travou ou lançou exceção. */
    private static Resultado analisaComTimeout(String fonte, long ms) {
        final Resultado[] caixa = new Resultado[1];
        Thread t = new Thread(() -> caixa[0] = analisa(fonte));
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((th, ex) -> { });
        t.start();
        try {
            t.join(ms);
        } catch (InterruptedException e) {
            return null;
        }
        return t.isAlive() ? null : caixa[0];
    }

    private static String posicoes(List<SyntaxError> erros) {
        StringBuilder sb = new StringBuilder();
        for (SyntaxError e : erros) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.line).append(':').append(e.col);
        }
        return sb.toString();
    }

    private static void check(String nome, Object esperado, Object obtido) {
        if (esperado.equals(obtido)) {
            ok++;
            System.out.println("[OK]     " + nome);
        } else {
            falhas++;
            System.out.println("[FALHA]  " + nome);
            System.out.println("           esperado: " + esperado);
            System.out.println("           obtido:   " + obtido);
        }
    }

    private static void mostraErros(Resultado r) {
        for (SyntaxError e : r.sintaticos) {
            System.out.println("           > " + e);
        }
    }


    static void testValidos() {
        String tipos = ""
                + "int a = 1;\n"
                + "double b = 2.5;\n"
                + "bool ok = true;\n"
                + "char c = 'x';\n"
                + "string s = \"texto\";\n"
                + "int semInicial;\n";
        Resultado r = analisa(tipos);
        check("válido 1: declarações globais dos 5 tipos básicos (com e sem inicialização)",
                "aceito, 0 erros", (r.aceito ? "aceito" : "rejeitado") + ", " + r.sintaticos.size() + " erros");

        String controle = ""
                + "int maior(int a, int b) {\n"
                + "    if (a > b) { return a; } else { return b; }\n"
                + "}\n"
                + "void conta(int n) {\n"
                + "    int i = 0;\n"
                + "    while (i < n && !(i == 5)) {\n"
                + "        i = i + 1;\n"
                + "        if (i % 2 == 0) { escreve(\"par\"); }\n"
                + "        else if (i > 7) { escreve(\"grande\"); }\n"
                + "        else { escreve(\"impar\"); }\n"
                + "    }\n"
                + "    return;\n"
                + "}\n";
        r = analisa(controle);
        check("válido 2: if / else if / else, while, return com e sem valor",
                "aceito, 0 erros", (r.aceito ? "aceito" : "rejeitado") + ", " + r.sintaticos.size() + " erros");

        String funcoes = ""
                + "int fat(int n) {\n"
                + "    if (n <= 1) { return 1; }\n"
                + "    return n * fat(n - 1);\n"
                + "}\n"
                + "double media(int a, int b) { return (a + b) / 2.0; }\n"
                + "void principal() {\n"
                + "    int r = fat(5);\n"
                + "    double m = media(r, -3);\n"
                + "    bool b = !(r >= 10) || m != 0.5 && true;\n"
                + "    { int x = 1; r = r + x; }\n"
                + "    escreve(\"fim\", r);\n"
                + "    escreve();\n"
                + "}\n";
        r = analisa(funcoes);
        check("válido 3: funções, parâmetros, chamadas (comando e expressão), recursão, bloco aninhado",
                "aceito, 0 erros", (r.aceito ? "aceito" : "rejeitado") + ", " + r.sintaticos.size() + " erros");

        r = analisa("");
        check("válido 4: programa vazio é aceito (DeclsTop → ε com lookahead EOF)",
                "aceito", r.aceito ? "aceito" : "rejeitado");

        r = analisa("// só comentário\n/* e bloco */\n");
        check("válido 5: só comentários também é aceito", "aceito", r.aceito ? "aceito" : "rejeitado");
    }

    static void testInvalidos() {
        Resultado r = analisa("int x = 5\nint y = 6;");
        check("inválido 1: falta ';' depois da expressão (erro em 2:1, 1 erro)",
                "rejeitado; 2:1", (r.aceito ? "aceito" : "rejeitado") + "; " + posicoes(r.sintaticos));

        r = analisa("void f() { if (x > 1 { return; } }");
        check("inválido 2: falta ')' no if (primeiro erro em 1:22)",
                "rejeitado; 1:22", (r.aceito ? "aceito" : "rejeitado") + "; " + posicoes(r.sintaticos).split(",")[0]);

        r = analisa("int x = ;");
        check("inválido 3: expressão ausente (erro em 1:9, 1 erro)",
                "rejeitado; 1:9", (r.aceito ? "aceito" : "rejeitado") + "; " + posicoes(r.sintaticos));

        r = analisa("void f() { else { } }");
        check("inválido 4: 'else' sem 'if' (erro em 1:12, 1 erro, o bloco seguinte é reaproveitado)",
                "rejeitado; 1:12", (r.aceito ? "aceito" : "rejeitado") + "; " + posicoes(r.sintaticos));

        r = analisa("x = 3;");
        check("inválido 5: comando fora de função (erro em 1:1, 1 erro)",
                "rejeitado; 1:1", (r.aceito ? "aceito" : "rejeitado") + "; " + posicoes(r.sintaticos));

        r = analisa("void f() { int x = 1;");
        check("inválido 6: bloco não fechado até EOF (erro em 1:22, 1 erro)",
                "rejeitado; 1:22", (r.aceito ? "aceito" : "rejeitado") + "; " + posicoes(r.sintaticos));

        r = analisa("int g(int a, ) { return 1; }");
        check("inválido 7: parâmetro faltando depois da vírgula (erro em 1:14, 1 erro)",
                "rejeitado; 1:14", (r.aceito ? "aceito" : "rejeitado") + "; " + posicoes(r.sintaticos));

        r = analisa("int x = 5 @;");
        check("erro léxico também rejeita: '@' (0 erros sintáticos, 1 léxico)",
                "rejeitado; 0 sint; 1 lex",
                (r.aceito ? "aceito" : "rejeitado") + "; " + r.sintaticos.size() + " sint; " + r.lexicos.size() + " lex");
    }

    static void testMultiplosErros() {
        String fonte = ""
                + "int x = ;\n"                       // 1
                + "void f(int a) {\n"                 // 2
                + "    y = 3 +;\n"                    // 3
                + "    while (a < ) {\n"              // 4
                + "        a = a - 1;\n"              // 5
                + "    }\n"                           // 6
                + "    return a\n"                    // 7
                + "}\n"                               // 8
                + "int g(int a, ) { return 1; }\n";   // 9
        Resultado r = analisa(fonte);
        System.out.println("           traço dos erros (uma única rodada de análise):");
        mostraErros(r);
        check("múltiplos erros: 5 erros detectados, cada um na posição certa, sem parar no primeiro",
                "rejeitado; 1:9, 3:12, 4:16, 8:1, 9:14",
                (r.aceito ? "aceito" : "rejeitado") + "; " + posicoes(r.sintaticos));

        String depois = fonte + "int ok = 1;\nint h() { return ok; }\n";
        Resultado r2 = analisa(depois);
        check("recuperação: declarações válidas depois dos erros não geram erro novo",
                posicoes(r.sintaticos), posicoes(r2.sintaticos));

        r = analisa("x = 3;\nint a = 1;\ny = 4;\nint b = 2;");
        check("múltiplos erros no nível superior: 2 erros (1:1 e 3:1), declarações válidas entre eles preservadas",
                "1:1, 3:1", posicoes(r.sintaticos));
    }

    static void testNaoTrava() {
        String[] entradas = {
            "}", "}}}}", "((((", "))))", "{", "int", "void", "if", "else else else", "while (",
            "return", ";;;;", "int int int", "void f(", "void f() {", "void f() { if (", "f(", "= = =",
            "int x = (((", "int x = 1 + + + ;", "\"abc", "/* nunca fecha", "'", "@@@", "void f() { while }"
        };
        boolean todos = true;
        String travou = "";
        for (String e : entradas) {
            Resultado r = analisaComTimeout(e, 2000);
            if (r == null) {
                todos = false;
                travou += " [" + e + "]";
            }
        }
        check("entradas patológicas (" + entradas.length + "): todas terminam em < 2s sem exceção",
                "sim", todos ? "sim" : "não:" + travou);

        final String[] vocab = {
            "int", "void", "if", "else", "while", "return", "true", "x", "f", "5", "3.5", "\"s\"", "'c'",
            "=", "==", "+", "-", "*", "(", ")", "{", "}", ",", ";", "!", "&&", "<"
        };
        final int[] processados = {0};
        Thread t = new Thread(() -> {
            Random rnd = new Random(42);
            for (int i = 0; i < 5000; i++) {
                int n = 1 + rnd.nextInt(40);
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < n; k++) {
                    sb.append(vocab[rnd.nextInt(vocab.length)]).append(' ');
                }
                analisa(sb.toString());
                processados[0]++;
            }
        });
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((th, ex) -> { });
        t.start();
        try {
            t.join(30000);
        } catch (InterruptedException e) {
            // segue
        }
        check("fuzz: 5000 sequências aleatórias de tokens analisadas sem travar nem lançar exceção",
                5000, processados[0]);
    }

    public static void main(String[] args) {
        System.out.println("=== Testes do parser ===");
        testValidos();
        testInvalidos();
        testMultiplosErros();
        testNaoTrava();
        System.out.println("=== Resumo: " + ok + " ok, " + falhas + " falhas ===");
        if (falhas > 0) {
            System.exit(1);
        }
    }
}
