import java.util.ArrayList;
import java.util.List;

/**
 * Suíte de testes do scanner.
 * Saída: uma linha [OK]/[FALHA] por teste e um resumo no final.
 * Código de saída 1 se algum teste falhar.
 */
public class ScannerTest {

    private static int ok = 0;
    private static int falhas = 0;

    // ---------- utilitários ----------

    /** Tokeniza tudo e devolve os tokens (sem o EOF). */
    private static List<Token> tokens(String src, List<LexicalError> errosOut) {
        Scanner s = new Scanner(src);
        List<Token> lista = new ArrayList<>();
        Token t;
        while ((t = s.nextToken()).type != TokenType.EOF) {
            lista.add(t);
        }
        if (errosOut != null) {
            errosOut.addAll(s.getErrors());
        }
        return lista;
    }

    /** Ex.: "KEYWORD:int IDENTIFIER:x OPERATOR:=" */
    private static String dump(List<Token> ts) {
        StringBuilder sb = new StringBuilder();
        for (Token t : ts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(t.type).append(':').append(t.lexeme);
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

    private static String pos(LexicalError e) {
        return e.line + ":" + e.col;
    }

    // ---------- casos válidos: uma bateria por categoria ----------

    static void testIdentificador() {
        check("identificador: total x1 _aux contaItens",
                "IDENTIFIER:total IDENTIFIER:x1 IDENTIFIER:_aux IDENTIFIER:contaItens",
                dump(tokens("total x1 _aux contaItens", null)));
    }

    static void testPalavraReservada() {
        check("palavras reservadas: if while int return true",
                "KEYWORD:if KEYWORD:while KEYWORD:int KEYWORD:return KEYWORD:true",
                dump(tokens("if while int return true", null)));
        check("maximal munch: 'iff' e 'ifx' são identificadores",
                "IDENTIFIER:iff IDENTIFIER:ifx",
                dump(tokens("iff ifx", null)));
        check("case-sensitive: 'While' e 'INT' são identificadores",
                "IDENTIFIER:While IDENTIFIER:INT",
                dump(tokens("While INT", null)));
    }

    static void testString() {
        check("string simples",
                "STRING_LIT:\"ok\" STRING_LIT:\"linha 1\"",
                dump(tokens("\"ok\" \"linha 1\"", null)));
        check("string com escape \\\"",
                "STRING_LIT:\"diz \\\"oi\\\"\"",
                dump(tokens("\"diz \\\"oi\\\"\"", null)));
        check("string vazia",
                "STRING_LIT:\"\"",
                dump(tokens("\"\"", null)));
    }

    static void testOperadores() {
        check("operadores de 1 e 2 caracteres",
                "OPERATOR:= OPERATOR:== OPERATOR:< OPERATOR:<= OPERATOR:> OPERATOR:>= OPERATOR:!= "
                        + "OPERATOR:+ OPERATOR:- OPERATOR:* OPERATOR:/ OPERATOR:% "
                        + "OPERATOR:&& OPERATOR:|| OPERATOR:!",
                dump(tokens("= == < <= > >= != + - * / % && || !", null)));
        check("maximal munch: 'a<=b' e 'a===b'",
                "IDENTIFIER:a OPERATOR:<= IDENTIFIER:b IDENTIFIER:a OPERATOR:== OPERATOR:= IDENTIFIER:b",
                dump(tokens("a<=b a===b", null)));
    }

    static void testNumeros() {
        check("literais numéricos: 10 3.14 0",
                "INT_LIT:10 DOUBLE_LIT:3.14 INT_LIT:0",
                dump(tokens("10 3.14 0", null)));
    }

    static void testChar() {
        check("literais char: 'a' '\\n'",
                "CHAR_LIT:'a' CHAR_LIT:'\\n'",
                dump(tokens("'a' '\\n'", null)));
    }

    static void testDelimitadores() {
        check("delimitadores",
                "DELIMITER:( DELIMITER:) DELIMITER:{ DELIMITER:} DELIMITER:[ DELIMITER:] DELIMITER:, DELIMITER:;",
                dump(tokens("(){}[],;", null)));
    }

    static void testComentarios() {
        check("comentário de linha e de bloco são descartados",
                "IDENTIFIER:a IDENTIFIER:b IDENTIFIER:c",
                dump(tokens("a // resto da linha\nb /* bloco\n de várias linhas ** */ c", null)));
        check("'/' sozinho é divisão",
                "IDENTIFIER:a OPERATOR:/ IDENTIFIER:b",
                dump(tokens("a / b", null)));
    }

    // ---------- casos de erro ----------

    static void testErroStringEOF() {
        List<LexicalError> erros = new ArrayList<>();
        List<Token> ts = tokens("\"abc", erros);
        check("erro: string não fechada até EOF (nenhum token, 1 erro)",
                "0 tokens, 1 erro em 1:1", ts.size() + " tokens, " + erros.size() + " erro em " + pos(erros.get(0)));
    }

    static void testErroStringFimDeLinha() {
        List<LexicalError> erros = new ArrayList<>();
        List<Token> ts = tokens("x = \"abc\ny = 1", erros);
        check("erro: string não fechada até fim de linha (erro em 1:5)",
                "1 erro em 1:5", erros.size() + " erro em " + pos(erros.get(0)));
        check("recuperação: tokenização continua na linha seguinte",
                "IDENTIFIER:x OPERATOR:= IDENTIFIER:y OPERATOR:= INT_LIT:1", dump(ts));
        check("recuperação: 'y' está na linha 2, coluna 1",
                "2:1", ts.get(2).line + ":" + ts.get(2).col);
    }

    static void testErroCaractereInvalido() {
        List<LexicalError> erros = new ArrayList<>();
        List<Token> ts = tokens("a @ b", erros);
        check("erro: caractere fora do alfabeto '@' (erro em 1:3)",
                "1 erro em 1:3", erros.size() + " erro em " + pos(erros.get(0)));
        check("recuperação: 'a' e 'b' continuam sendo reconhecidos",
                "IDENTIFIER:a IDENTIFIER:b", dump(ts));
    }

    static void testErroComentarioEOF() {
        List<LexicalError> erros = new ArrayList<>();
        List<Token> ts = tokens("x /* nunca fecha", erros);
        check("erro: comentário de bloco não fechado até EOF (erro em 1:3)",
                "1 erro em 1:3", erros.size() + " erro em " + pos(erros.get(0)));
        check("comentário não fechado: token anterior preservado",
                "IDENTIFIER:x", dump(ts));
    }

    static void testErrosDiversos() {
        List<LexicalError> erros = new ArrayList<>();
        List<Token> ts = tokens("a & b | c", erros);
        check("erro: '&' e '|' isolados (2 erros, tokens seguem)",
                "2 erros; IDENTIFIER:a IDENTIFIER:b IDENTIFIER:c",
                erros.size() + " erros; " + dump(ts));

        erros = new ArrayList<>();
        ts = tokens("\"a\\qb\" x", erros);
        check("erro: escape inválido em string (string descartada, 'x' segue)",
                "1 erro em 1:3; IDENTIFIER:x", erros.size() + " erro em " + pos(erros.get(0)) + "; " + dump(ts));

        erros = new ArrayList<>();
        ts = tokens("'' 'ab' y", erros);
        check("erro: char vazio e char com mais de um caractere",
                "2 erros; IDENTIFIER:y", erros.size() + " erros; " + dump(ts));

        erros = new ArrayList<>();
        ts = tokens("3.", erros);
        check("'3.' vira INT_LIT:3 e o '.' isolado é erro em 1:2",
                "INT_LIT:3; 1 erro em 1:2", dump(ts) + "; " + erros.size() + " erro em " + pos(erros.get(0)));
    }

    // ---------- trecho realista ----------

    static void testTrechoRealista() {
        String fonte = "int total = 10; // contador\n"
                + "/* bloco\n"
                + "   de comentario */ while (total >= 3.5 && !done) { total = total - 1; }\n"
                + "string s = \"ok\";\n";
        List<LexicalError> erros = new ArrayList<>();
        List<Token> ts = tokens(fonte, erros);

        check("trecho realista: sequência completa de tokens",
                "KEYWORD:int IDENTIFIER:total OPERATOR:= INT_LIT:10 DELIMITER:; "
                        + "KEYWORD:while DELIMITER:( IDENTIFIER:total OPERATOR:>= DOUBLE_LIT:3.5 OPERATOR:&& "
                        + "OPERATOR:! IDENTIFIER:done DELIMITER:) DELIMITER:{ IDENTIFIER:total OPERATOR:= "
                        + "IDENTIFIER:total OPERATOR:- INT_LIT:1 DELIMITER:; DELIMITER:} "
                        + "KEYWORD:string IDENTIFIER:s OPERATOR:= STRING_LIT:\"ok\" DELIMITER:;",
                dump(ts));
        check("trecho realista: nenhum erro léxico", 0, erros.size());

        Token wh = ts.get(5);
        check("trecho realista: 'while' na posição 3:21 (depois de comentários de linha e de bloco)",
                "3:21", wh.line + ":" + wh.col);
    }

    public static void main(String[] args) {
        System.out.println("=== Testes do scanner ===");
        testIdentificador();
        testPalavraReservada();
        testString();
        testOperadores();
        testNumeros();
        testChar();
        testDelimitadores();
        testComentarios();
        testErroStringEOF();
        testErroStringFimDeLinha();
        testErroCaractereInvalido();
        testErroComentarioEOF();
        testErrosDiversos();
        testTrechoRealista();
        System.out.println("=== Resumo: " + ok + " ok, " + falhas + " falhas ===");
        if (falhas > 0) {
            System.exit(1);
        }
    }
}
