import java.nio.file.Files;
import java.nio.file.Path;

/** Uso: java Main [arquivo-fonte]. Sem argumento, usa um exemplo embutido. */
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

/** ESTE MAIN É PARA TESTE, para usar basta descomentar este e comentar o de cima
 NA PASTA "EXEMPLOS" TEM UM ARQUIVO TESTE.TXT */
/**
public class Main {
    public static void main(String[] args) throws Exception {
        String fonte = Files.readString(Path.of("exemplos/teste.txt"));

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
 */