package compiler;

public class Main {
    public static void main(String[] args) {
        // Código fonte de teste com espaços, comentários, quebras e ERROS intencionais
        String codigoFonte = 
            "int x = 10; \n" +
            "/* comentário \n" +
            " de bloco */\n" +
            "if (x <= 20) { \n" +
            "   String nome = \"teste;\n" + // Erro: aspa não fechada até o eof (simulado)
            "   @ \n" +                     // Erro: caractere inválido
            "}";

        System.out.println("Iniciando Análise Léxica...\n");
        
        Scanner scanner = new Scanner(codigoFonte);
        Token token;

        do {
            token = scanner.nextToken();
            
            // Ignoramos a impressão do token de ERROR no terminal principal 
            // pois o Scanner já emitiu o aviso no System.err
            if (token.type != TokenType.ERROR) {
                System.out.println(token);
            }
            
        } while (token.type != TokenType.EOF);
        
        System.out.println("\nAnálise Léxica Finalizada.");
    }
}