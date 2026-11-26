import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ast.*;
import tipos.RegrasDeTipos;
import tipos.TypeChecker;

/**
 * CP5 — as regras de tipos, isoladas: sem scanner e sem parser.
 *
 * 1. A tabela de operadores do papel (tipos-a-mao.pdf), transcrita em TABELA abaixo. Para
 *    cada operador e CADA par de tipos básicos (5 x 5), o teste confere: se o par está na
 *    tabela, a regra aceita e o resultado é o da tabela; se não está, a regra recusa (null).
 *    Assim toda linha da tabela é testada aceita, e toda combinação fora dela, recusada.
 * 2. Unários e compatibilidade de atribuição, do mesmo jeito.
 * 3. [ERRO]: qualquer operando ERRO dá ERRO, sem recusa (sem cascata).
 * 4. Árvores montadas à mão (sem parser) passando pelo TypeChecker: o tipo fica gravado
 *    em cada nó e o erro sai com a mensagem documentada.
 */
public class RegrasTest {

    private static int ok = 0;
    private static int falhas = 0;

    private static final Tipo[] BASICOS = {Tipo.INT, Tipo.DOUBLE, Tipo.BOOL, Tipo.CHAR, Tipo.STRING};

    private static final Tipo I = Tipo.INT, D = Tipo.DOUBLE, B = Tipo.BOOL, C = Tipo.CHAR;

    /** Uma linha da tabela de operadores: operadores, tipos dos operandos, tipo do resultado. */
    private static Object[] linha(String ops, Tipo a, Tipo b, Tipo r) {
        return new Object[]{ops, a, b, r};
    }

    /** A tabela de operadores, igual à do papel. O que não está aqui é recusado. */
    private static final Object[][] TABELA = {
        // aritméticos: [ARIT-INT] e [ARIT-DOUBLE] (int -> double)
        linha("+ - * /", I, I, I),
        linha("+ - * /", I, D, D),
        linha("+ - * /", D, I, D),
        linha("+ - * /", D, D, D),
        // resto: [MOD]
        linha("%", I, I, I),
        // relacionais: [REL-NUM] e [REL-CHAR]
        linha("< <= > >=", I, I, B),
        linha("< <= > >=", I, D, B),
        linha("< <= > >=", D, I, B),
        linha("< <= > >=", D, D, B),
        linha("< <= > >=", C, C, B),
        // igualdade: [IGUAL-NUM] e [IGUAL]
        linha("== !=", I, I, B),
        linha("== !=", I, D, B),
        linha("== !=", D, I, B),
        linha("== !=", D, D, B),
        linha("== !=", B, B, B),
        linha("== !=", C, C, B),
        // lógicos: [LOG]
        linha("&& ||", B, B, B),
    };

    private static void check(String nome, Object esperado, Object obtido) {
        if (esperado == null ? obtido == null : esperado.equals(obtido)) {
            ok++;
        } else {
            falhas++;
            System.out.println("[FALHA]  " + nome + "  esperado: " + esperado + "  obtido: " + obtido);
        }
    }

    private static Tipo esperadoNaTabela(BinOp op, Tipo a, Tipo b) {
        for (Object[] l : TABELA) {
            List<String> ops = Arrays.asList(((String) l[0]).split(" "));
            if (ops.contains(op.simbolo()) && l[1] == a && l[2] == b) {
                return (Tipo) l[3];
            }
        }
        return null;
    }

    static void testTabelaBinarios() {
        int antes = ok;
        int aceitas = 0;
        for (BinOp op : BinOp.values()) {
            for (Tipo a : BASICOS) {
                for (Tipo b : BASICOS) {
                    Tipo esp = esperadoNaTabela(op, a, b);
                    if (esp != null) {
                        aceitas++;
                    }
                    check(a + " " + op.simbolo() + " " + b, esp, RegrasDeTipos.binario(op, a, b));
                }
            }
        }
        int total = BinOp.values().length * BASICOS.length * BASICOS.length;
        System.out.println("[" + (ok - antes == total ? "OK" : "FALHA") + "]     tabela de operadores binários: "
                + total + " combinações (13 operadores x 25 pares), " + aceitas + " aceitas e "
                + (total - aceitas) + " recusadas conforme a tabela");
    }

    static void testUnarios() {
        int antes = ok;
        for (Tipo t : BASICOS) {
            check("!" + t, t == B ? B : null, RegrasDeTipos.unario(UnOp.NOT, t));
            check("-" + t, (t == I || t == D) ? t : null, RegrasDeTipos.unario(UnOp.NEG, t));
        }
        System.out.println("[" + (ok - antes == 10 ? "OK" : "FALHA")
                + "]     unários: !bool = bool, -int = int, -double = double; os outros 7 recusados");
    }

    static void testCompatibilidade() {
        int antes = ok;
        for (Tipo destino : BASICOS) {
            for (Tipo origem : BASICOS) {
                boolean esp = destino == origem || (destino == D && origem == I);
                check(origem + " -> " + destino, esp, RegrasDeTipos.atribuivel(destino, origem));
            }
        }
        System.out.println("[" + (ok - antes == 25 ? "OK" : "FALHA")
                + "]     compatibilidade (atribuição, argumento, return): T <= T e int <= double; "
                + "double <= int recusado");
    }

    static void testErroSemCascata() {
        int antes = ok;
        for (BinOp op : BinOp.values()) {
            for (Tipo t : BASICOS) {
                check("ERRO " + op.simbolo() + " " + t, Tipo.ERRO, RegrasDeTipos.binario(op, Tipo.ERRO, t));
                check(t + " " + op.simbolo() + " ERRO", Tipo.ERRO, RegrasDeTipos.binario(op, t, Tipo.ERRO));
            }
        }
        check("!ERRO", Tipo.ERRO, RegrasDeTipos.unario(UnOp.NOT, Tipo.ERRO));
        check("int <= ERRO", true, RegrasDeTipos.atribuivel(I, Tipo.ERRO));
        check("ERRO <= string", true, RegrasDeTipos.atribuivel(Tipo.ERRO, Tipo.STRING));
        check("void não é valor", false, RegrasDeTipos.atribuivel(I, Tipo.VOID));
        System.out.println("[" + (ok - antes == 134 ? "OK" : "FALHA")
                + "]     [ERRO]: operando ERRO dá ERRO, nunca recusa (sem cascata)");
    }

    /** Árvores construídas à mão, sem parser, passando pelo verificador. */
    static void testArvoresAMao() {
        int antes = ok;

        // int x = 1 + 2.5;  (global, linha 1)
        IntLit um = new IntLit("1", 1, 9);
        DoubleLit dois = new DoubleLit("2.5", 1, 13);
        Bin soma = new Bin(BinOp.ADD, um, dois, 1, 9);
        VarDecl x = new VarDecl(new TypeNode(Tipo.DOUBLE, 1, 1), "x", soma, 1, 1);
        // bool b = (1 + true) == 3;  (linha 2): um único erro, no '+'
        Bin ruim = new Bin(BinOp.ADD, new IntLit("1", 2, 11), new BoolLit(true, 2, 15), 2, 11);
        Bin igual = new Bin(BinOp.EQ, ruim, new IntLit("3", 2, 24), 2, 10);
        VarDecl b = new VarDecl(new TypeNode(Tipo.BOOL, 2, 1), "b", igual, 2, 1);
        Program p = new Program(new ArrayList<>(Arrays.asList(x, b)), 1, 1);

        TypeChecker tc = TypeChecker.run(p);
        check("tipo gravado: 1 : int", I, um.tipo);
        check("tipo gravado: 2.5 : double", D, dois.tipo);
        check("tipo gravado: 1 + 2.5 : double", D, soma.tipo);
        check("tipo gravado: 1 + true : <erro>", Tipo.ERRO, ruim.tipo);
        check("tipo gravado: (1 + true) == 3 : <erro> (sobe sem mensagem)", Tipo.ERRO, igual.tipo);
        check("um único erro, com a mensagem documentada",
                "[linha 2: operador '+' não se aplica a int e bool]", tc.getErros().toString());
        System.out.println("[" + (ok - antes == 6 ? "OK" : "FALHA")
                + "]     árvores montadas à mão: tipo gravado em cada nó; erro único sem cascata");
    }

    public static void main(String[] args) {
        System.out.println("=== Testes das regras de tipos, isoladas (CP5) ===");
        testTabelaBinarios();
        testUnarios();
        testCompatibilidade();
        testErroSemCascata();
        testArvoresAMao();
        System.out.println("=== Resumo Regras: " + ok + " asserções ok, " + falhas + " falhas ===");
        if (falhas > 0) {
            System.exit(1);
        }
    }
}
