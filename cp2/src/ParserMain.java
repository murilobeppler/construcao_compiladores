import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class ParserMain {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Uso: java ParserMain <arquivo-fonte>");
            return;
        }
        String fonte = Files.readString(Path.of(args[0]));

        Parser parser = new Parser(new Scanner(fonte));
        boolean aceito = parser.parse();

        // junta erros léxicos e sintáticos e ordena por (linha, coluna)
        List<Object[]> todos = new ArrayList<>();
        for (LexicalError e : parser.getLexicalErrors()) {
            todos.add(new Object[]{e.line, e.col, e.toString()});
        }
        for (SyntaxError e : parser.getErrors()) {
            todos.add(new Object[]{e.line, e.col, e.toString()});
        }
        todos.sort((a, b) -> {
            int c = Integer.compare((Integer) a[0], (Integer) b[0]);
            return c != 0 ? c : Integer.compare((Integer) a[1], (Integer) b[1]);
        });
        for (Object[] e : todos) {
            System.out.println(e[2]);
        }

        if (aceito) {
            System.out.println("ACEITO");
        } else {
            System.out.println("REJEITADO (" + todos.size() + " erro(s))");
        }
    }
}
