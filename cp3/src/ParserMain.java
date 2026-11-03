import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import visitor.SourcePrinter;
import visitor.TreePrinter;

public class ParserMain {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Uso: java ParserMain <arquivo-fonte> [--fonte]");
            return;
        }
        String fonte = new String(Files.readAllBytes(Path.of(args[0])), "UTF-8");
        boolean reimprime = args.length > 1 && args[1].equals("--fonte");

        ParseResult r = new Parser(new Scanner(fonte)).parse();

        if (r.aceito) {
            System.out.print(reimprime ? SourcePrinter.print(r.program) : TreePrinter.print(r.program));
            System.out.println("ACEITO");
            return;
        }

        // junta erros léxicos e sintáticos e ordena por (linha, coluna)
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
        System.out.println("REJEITADO (" + todos.size() + " erro(s)) — nenhuma AST produzida");
    }
}
