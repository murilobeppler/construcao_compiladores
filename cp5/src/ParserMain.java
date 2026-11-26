import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import tipos.Verificador;
import visitor.ResolutionPrinter;
import visitor.SourcePrinter;
import visitor.TreePrinter;

public class ParserMain {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Uso: java ParserMain <arquivo-fonte> [--fonte | --arvore | --resolucao | --tipos]");
            return;
        }
        String fonte = Files.readString(Path.of(args[0]));
        boolean reimprime = args.length > 1 && args[1].equals("--fonte");
        boolean soArvore = args.length > 1 && args[1].equals("--arvore");
        boolean resolucao = args.length > 1 && args[1].equals("--resolucao");
        boolean arvoreTipos = args.length > 1 && args[1].equals("--tipos");

        ParseResult r = new Parser(new Scanner(fonte)).parse();

        if (!r.aceito) {
            List<Object[]> todos = new ArrayList<>();
            for (LexicalError e : r.lexicalErrors) {
                todos.add(new Object[]{e.line, e.col, e.toString()});
            }
            for (SyntaxError e : r.syntaxErrors) {
                todos.add(new Object[]{e.line, e.col, e.toString()});
            }
            todos.sort((a, b) -> {
                int c = Integer.compare((Integer) a[0], (Integer) b[0]);
                return c != 0 ? c : Integer.compare((Integer) a[1], (Integer) b[1]);
            });
            for (Object[] e : todos) {
                System.out.println(e[2]);
            }
            System.out.println("REJEITADO (" + todos.size() + " erro(s) léxicos/sintáticos) — sem AST");
            return;
        }

        if (soArvore) {
            System.out.print(TreePrinter.print(r.program));
            return;
        }
        if (reimprime) {
            System.out.print(SourcePrinter.print(r.program));
            return;
        }

        // Análise contextual: identificação (CP4) + tipos (CP5), numa única rodada
        Verificador v = Verificador.verifica(r.program);

        if (resolucao) {
            System.out.println(ResolutionPrinter.print(r.program));
        }
        if (arvoreTipos) {
            System.out.print(TreePrinter.printComTipos(r.program));
        }

        System.out.println(v.formata());
        if (!v.ok()) {
            System.out.println("REJEITADO (" + v.erros.size() + " erro(s)) — não segue para a geração de código");
        } else {
            System.out.println("ACEITO (" + v.avisos.size() + " aviso(s))");
        }
    }
}
