import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Uso: java Main [arquivo-fonte]. Sem argumento, usa um exemplo embutido.
 * Exemplo com arquivo: java -cp out Main exemplos/teste.txt
 */
public class Main {
    public static void main(String[] args) throws Exception {
        String fonte;
        if (args.length > 0) {
            fonte = Files.readString(Path.of(args[0]));
        } else {
            fonte = "int total = 10; // contador\n"
                    + "while (total >= 3.5 && !done) { total = total - 1; }\n"
                    + "string s = \"ok\";\n";
        }

        Scanner scanner = new Scanner(fonte);
        Token t;
        do {
            t = scanner.nextToken();
            System.out.println(t);
        } while (t.type != TokenType.EOF);

        for (LexicalError err : scanner.getErrors()) {
            System.out.println(err);
        }
    }
}
