import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import ast.Program;
import symbols.ContextualError;
import visitor.IdentificationVisitor;
import visitor.ResolutionPrinter;

public class IdentificationTest {

    private static int ok = 0;
    private static int falhas = 0;

    private static String ler(String caminho) {
        try {
            return new String(Files.readAllBytes(Path.of(caminho)), "UTF-8").replace("\r\n", "\n").trim();
        } catch (IOException e) {
            return "<erro ao ler " + caminho + ">";
        }
    }

    private static void check(String nome, Object esperado, Object obtido) {
        if ((esperado == null && obtido == null) || (esperado != null && esperado.equals(obtido))) {
            ok++;
            System.out.println("[OK]     " + nome);
        } else {
            falhas++;
            System.out.println("[FALHA]  " + nome);
            System.out.println("           esperado:\n" + esperado);
            System.out.println("           obtido:\n" + obtido);
        }
    }

    private static Program parse(String caminho) {
        String fonte = ler(caminho);
        ParseResult r = new Parser(new Scanner(fonte)).parse();
        if (!r.aceito) {
            throw new RuntimeException("Erro sintático inesperado em " + caminho + ": " + r.syntaxErrors);
        }
        return r.program;
    }

    private static String formataErros(List<ContextualError> erros) {
        return erros.stream().map(ContextualError::toString).collect(Collectors.joining("\n")).trim();
    }

    public static void main(String[] args) {
        System.out.println("=== Testes da Passada de Identificação (CP4) ===");

        testProgramasObrigatorios();
        testDecisoesDeProjeto();
        testCasosEspeciais();
        testProgramaDesenho();

        System.out.println("=== Resumo Identificação: " + ok + " ok, " + falhas + " falhas ===");
        if (falhas > 0) {
            System.exit(1);
        }
    }

    static void testProgramasObrigatorios() {
        // 1. Válido com sombreamento: todo uso resolve à declaração certa
        Program p1 = parse("test/programas/prog1-valido-sombreamento.txt");
        List<ContextualError> e1 = IdentificationVisitor.run(p1);
        check("prog1-valido-sombreamento: 0 erros", "", formataErros(e1));
        check("prog1-valido-sombreamento: resolução confere com esperado",
                ler("test/esperado/prog1-valido-sombreamento.txt"),
                ResolutionPrinter.print(p1).trim());

        // 2. Erro de redeclaração
        Program p2 = parse("test/programas/prog2-erro-redeclaracao.txt");
        List<ContextualError> e2 = IdentificationVisitor.run(p2);
        check("prog2-erro-redeclaracao: erros conferem com esperado",
                ler("test/esperado/prog2-erro-redeclaracao.txt"),
                formataErros(e2));

        // 3. Nome não declarado
        Program p3 = parse("test/programas/prog3-erro-nao-declarado.txt");
        List<ContextualError> e3 = IdentificationVisitor.run(p3);
        check("prog3-erro-nao-declarado: erros conferem com esperado",
                ler("test/esperado/prog3-erro-nao-declarado.txt"),
                formataErros(e3));
    }

    static void testDecisoesDeProjeto() {
        // Decisão 1: Sombreamento em escopos aninhados é válido
        Program d1 = parse("test/programas/decisao1-sombreamento.txt");
        List<ContextualError> ed1 = IdentificationVisitor.run(d1);
        check("decisao 1 (sombreamento permitido): 0 erros", "", formataErros(ed1));

        // Decisão 2: Declaração antes do uso (uso prévio gera erro)
        Program d2 = parse("test/programas/decisao2-decl-antes-uso.txt");
        List<ContextualError> ed2 = IdentificationVisitor.run(d2);
        check("decisao 2 (declarar antes de usar): erros conferem",
                ler("test/esperado/decisao2-decl-antes-uso.txt"),
                formataErros(ed2));

        // Decisão 3: Funções mutuamente recursivas são válidas via passada prévia
        Program d3 = parse("test/programas/decisao3-recursao-mutua.txt");
        List<ContextualError> ed3 = IdentificationVisitor.run(d3);
        check("decisao 3 (recursão mútua permitida): 0 erros", "", formataErros(ed3));

        // Decisão 4: Parâmetros e corpo compartilham mesmo escopo (redeclaração)
        Program d4 = parse("test/programas/decisao4-param-corpo.txt");
        List<ContextualError> ed4 = IdentificationVisitor.run(d4);
        check("decisao 4 (parâmetro e variável local mesmo escopo): erro de redeclaração",
                ler("test/esperado/decisao4-param-corpo.txt"),
                formataErros(ed4));

        // Decisão 5: Espaço de nomes único entre variáveis e funções
        Program d5 = parse("test/programas/decisao5-espaco-nomes.txt");
        List<ContextualError> ed5 = IdentificationVisitor.run(d5);
        check("decisao 5 (espaço de nomes único): colisão de variável e função gera erro",
                ler("test/esperado/decisao5-espaco-nomes.txt"),
                formataErros(ed5));
    }

    static void testCasosEspeciais() {
        // Pelo menos 3 erros em uma única rodada na ordem do texto
        Program pTres = parse("test/programas/prog-tres-erros.txt");
        List<ContextualError> eTres = IdentificationVisitor.run(pTres);
        check("pelo menos 3 erros reportados em ordem",
                ler("test/esperado/prog-tres-erros.txt"),
                formataErros(eTres));

        // Procedimento recursivo
        Program pRec = parse("test/programas/prog-proc-recursivo.txt");
        List<ContextualError> eRec = IdentificationVisitor.run(pRec);
        check("procedimento recursivo: 0 erros", "", formataErros(eRec));

        // Uso após fechamento do bloco (o nome interno não pode ser visto fora)
        Program pPos = parse("test/programas/prog-uso-pos-bloco.txt");
        List<ContextualError> ePos = IdentificationVisitor.run(pPos);
        check("uso de identificador após fechamento do bloco gera 'não declarado'",
                ler("test/esperado/prog-uso-pos-bloco.txt"),
                formataErros(ePos));
    }

    static void testProgramaDesenho() {
        // Programa do escopo-design.md
        Program pDes = parse("test/programas/prog-desenho.txt");
        List<ContextualError> eDes = IdentificationVisitor.run(pDes);
        check("programa do desenho: erro esperado na linha 9 (y após fechamento)",
                ler("test/esperado/prog-desenho.txt"),
                formataErros(eDes));
        check("programa do desenho: resoluções conferem exatamente com o desenho manual",
                ler("test/esperado/prog-desenho.resolucao.txt"),
                ResolutionPrinter.print(pDes).trim());
    }
}

