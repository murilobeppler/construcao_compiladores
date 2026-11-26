import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import ast.*;
import tipos.Verificador;
import visitor.TreePrinter;

/**
 * CP5 — testes do verificador sobre programas.
 *
 * 1. Para cada test/programas/X.txt, compara a saída do verificador (identificação + tipos,
 *    formato de Verificador.formata) com test/esperado/X.txt, escrito à mão a partir das regras.
 * 2. Com erro, o programa é rejeitado (não segue para a geração de código); só com avisos, aceito.
 * 3. O tipo fica gravado em cada expressão (o que a geração de código vai ler).
 * 4. Robustez: milhares de programas aleatórios bem formados terminam sem exceção.
 *
 * Rodar da pasta cp5:  java -cp out TipagemTest
 */
public class TipagemTest {

    private static int ok = 0;
    private static int falhas = 0;

    private static String ler(String caminho) {
        try {
            return new String(Files.readAllBytes(Path.of(caminho)), "UTF-8").replace("\r\n", "\n").trim();
        } catch (IOException e) {
            return "<não consegui ler " + caminho + ">";
        }
    }

    private static void check(String nome, Object esperado, Object obtido) {
        if (esperado == null ? obtido == null : esperado.equals(obtido)) {
            ok++;
            System.out.println("[OK]     " + nome);
        } else {
            falhas++;
            System.out.println("[FALHA]  " + nome);
            System.out.println("           esperado:\n" + esperado);
            System.out.println("           obtido:\n" + obtido);
        }
    }

    private static Program parse(String fonte) {
        ParseResult r = new Parser(new Scanner(fonte)).parse();
        if (!r.aceito) {
            throw new IllegalStateException("erro sintático: " + r.syntaxErrors + " " + r.lexicalErrors);
        }
        return r.program;
    }

    // ---------- 1 e 2. pares programa / esperado ----------

    static void testProgramas() {
        File[] arquivos = new File("test/programas").listFiles((d, n) -> n.endsWith(".txt"));
        if (arquivos == null || arquivos.length == 0) {
            check("há programas em test/programas", "sim", "não");
            return;
        }
        Arrays.sort(arquivos);
        for (File f : arquivos) {
            String nome = f.getName();
            String esperado = ler("test/esperado/" + nome);
            String obtido;
            boolean aceito;
            try {
                Verificador v = Verificador.verifica(parse(ler(f.getPath())));
                obtido = v.formata();
                aceito = v.ok();
            } catch (RuntimeException e) {
                obtido = "<exceção: " + e + ">";
                aceito = false;
            }
            check(nome, esperado, obtido);
            // só avisos (ou nada) => aceito; qualquer erro => rejeitado
            boolean deveAceitar = Arrays.stream(esperado.split("\n"))
                    .allMatch(l -> l.equals("sem erros") || l.contains(": aviso: "));
            check(nome + " -> " + (deveAceitar ? "aceito (segue para a geração)" : "rejeitado (não segue)"),
                    deveAceitar, aceito);
        }
    }

    // ---------- 3. tipo gravado em cada expressão ----------

    static void testTiposGravados() {
        Program p = parse("void f(int i, double d) {\n    double r = i * 2 + d;\n    bool b = !(r > i);\n}\n");
        Verificador.verifica(p);
        String esperado = String.join("\n",
                "Program",
                "  ProcDecl(f)",
                "    Param(int i)",
                "    Param(double d)",
                "    Block",
                "      VarDecl(double r)",
                "        Bin(+) : double",
                "          Bin(*) : int",
                "            Id(i) : int",
                "            Int(2) : int",
                "          Id(d) : double",
                "      VarDecl(bool b)",
                "        Un(!) : bool",
                "          Bin(>) : bool",
                "            Id(r) : double",
                "            Id(i) : int");
        check("tipo gravado em cada expressão (int * int = int; int + double = double)",
                esperado, TreePrinter.printComTipos(p).trim());
    }

    // ---------- 4. robustez ----------

    private static final String[] FOLHAS = {"1", "2.5", "true", "'c'", "\"s\"", "i", "d", "b", "c", "s",
        "f(1)", "f(i, d)", "p()", "f", "x", "g(b)"};
    private static final String[] BIN = {"+", "-", "*", "/", "%", "<", "<=", ">", ">=", "==", "!=", "&&", "||"};

    private static String expr(Random r, int prof) {
        if (prof == 0 || r.nextInt(3) == 0) {
            return FOLHAS[r.nextInt(FOLHAS.length)];
        }
        switch (r.nextInt(3)) {
            case 0: return "(" + expr(r, prof - 1) + " " + BIN[r.nextInt(BIN.length)] + " " + expr(r, prof - 1) + ")";
            case 1: return (r.nextBoolean() ? "!" : "-") + expr(r, prof - 1);
            default: return "f(" + expr(r, prof - 1) + ", " + expr(r, prof - 1) + ")";
        }
    }

    static void testRobustez() {
        final int N = 3000;
        final int[] feitos = {0};
        final String[] problema = {null};
        Thread t = new Thread(() -> {
            Random r = new Random(5);
            String[] tipos = {"int", "double", "bool", "char", "string"};
            for (int k = 0; k < N; k++) {
                String fonte = "int f(int a, double b) { return " + expr(r, 3) + "; }\n"
                        + "void p() { }\n"
                        + "void g(bool q) { if (" + expr(r, 3) + ") { return " + expr(r, 2) + "; } }\n"
                        + "void principal() {\n"
                        + "    int i = 1; double d = 2.0; bool b = true; char c = 'c'; string s = \"s\";\n"
                        + "    " + tipos[r.nextInt(5)] + " v = " + expr(r, 4) + ";\n"
                        + "    while (" + expr(r, 3) + ") { i = " + expr(r, 3) + "; }\n"
                        + "    p(" + expr(r, 2) + ");\n"
                        + "}\n";
                try {
                    Verificador.verifica(parse(fonte)).formata();
                } catch (RuntimeException e) {
                    problema[0] = e + "\n" + fonte;
                    return;
                }
                feitos[0]++;
            }
        });
        t.setDaemon(true);
        t.start();
        try {
            t.join(60000);
        } catch (InterruptedException e) {
            // segue
        }
        check("robustez: " + N + " programas aleatórios verificados sem exceção e sem travar",
                N + "", problema[0] == null ? feitos[0] + "" : problema[0]);
    }

    public static void main(String[] args) {
        System.out.println("=== Testes do verificador de tipos (CP5) ===");
        testProgramas();
        testTiposGravados();
        testRobustez();
        System.out.println("=== Resumo Tipagem: " + ok + " ok, " + falhas + " falhas ===");
        if (falhas > 0) {
            System.exit(1);
        }
    }
}
