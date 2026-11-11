import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Analisador léxico, sem JFlex e sem regex de biblioteca.
 *
 * Cada categoria de token é um método scanXxx() que roda um AFD: uma variável
 * de estado (enum Estado) dentro de um while(true)/switch, onde cada 'case' é
 * um estado do desenho em automatos/. Os métodos devolvem o Token ou null
 * quando ocorre erro léxico (o erro já foi registrado; nextToken() continua).
 *
 * Maximal munch: cada AFD só termina quando o próximo caractere não permite
 * continuar o token (lookahead com peek(), sem consumir).
 */
public class Scanner {

    private static final Set<String> KEYWORDS = Set.of(
            "int", "double", "bool", "char", "string", "void",
            "if", "else", "while", "return", "true", "false");

    private final String src;
    private int pos = 0;      // cursor no texto
    private int line = 1;     // linha atual (começa em 1)
    private int col = 1;      // coluna atual (começa em 1)
    private final List<LexicalError> errors = new ArrayList<>();

    public Scanner(String src) {
        this.src = src;
    }

    // ------------------------------------------------------------------
    // Cursor: hasNext / peek / peekNext / advance
    // ------------------------------------------------------------------

    public boolean hasNext() {
        return pos < src.length();
    }

    /** Caractere atual, sem consumir. Só chamar se hasNext(). */
    private char peek() {
        return src.charAt(pos);
    }

    /** Caractere seguinte ao atual, sem consumir. '\0' se não existir. */
    private char peekNext() {
        return (pos + 1 < src.length()) ? src.charAt(pos + 1) : '\0';
    }

    /** Consome o caractere atual e atualiza linha/coluna. */
    private char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    /** Se o caractere atual for 'esperado', consome e devolve true. */
    private boolean match(char esperado) {
        if (hasNext() && peek() == esperado) {
            advance();
            return true;
        }
        return false;
    }

    public List<LexicalError> getErrors() {
        return errors;
    }

    private void report(int l, int c, String msg) {
        errors.add(new LexicalError(l, c, msg));
    }

    // ------------------------------------------------------------------
    // Classes de caracteres (só ASCII, conforme a especificação)
    // ------------------------------------------------------------------

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isEscape(char c, char aspa) {
        return c == aspa || c == '\\' || c == 'n' || c == 't';
    }

    private static String descreve(char c) {
        if (c < 32 || c == 127) {
            return String.format("0x%02X", (int) c);
        }
        return "'" + c + "'";
    }

    // ------------------------------------------------------------------
    // nextToken: ponto de entrada
    // ------------------------------------------------------------------

    /**
     * Devolve o próximo token válido. Se encontrar erro léxico, registra o
     * erro (getErrors) e segue tokenizando (recuperação). No fim, devolve EOF.
     */
    public Token nextToken() {
        while (true) {
            skipWhitespaceAndComments();
            if (!hasNext()) {
                return new Token(TokenType.EOF, "", line, col);
            }
            int startLine = line;
            int startCol = col;
            char c = peek();

            Token t;
            if (isLetter(c) || c == '_') {
                t = scanIdentifier(startLine, startCol);
            } else if (isDigit(c)) {
                t = scanNumber(startLine, startCol);
            } else if (c == '"') {
                t = scanString(startLine, startCol);
            } else if (c == '\'') {
                t = scanChar(startLine, startCol);
            } else {
                t = scanOperatorOrDelimiter(startLine, startCol);
            }

            if (t != null) {
                return t;
            }
            // t == null: houve erro (já registrado). Volta ao início do loop
            // e tenta o próximo token.
        }
    }

    // ------------------------------------------------------------------
    // Espaços em branco e comentários
    // ------------------------------------------------------------------

    private void skipWhitespaceAndComments() {
        while (hasNext()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                advance();
            } else if (c == '/' && peekNext() == '/') {
                skipLineComment();
            } else if (c == '/' && peekNext() == '*') {
                skipBlockComment();
            } else {
                break;
            }
        }
    }

    private void skipLineComment() {
        while (hasNext() && peek() != '\n') {
            advance();
        }
    }

    /** AFD do comentário de bloco (não aninhado). */
    private void skipBlockComment() {
        int startLine = line;
        int startCol = col;
        advance(); // '/'
        advance(); // '*'
        Estado e = Estado.IN_BLOCK;

        while (true) {
            switch (e) {
                case IN_BLOCK:
                    if (!hasNext()) {
                        e = Estado.ERRO_COMENTARIO_EOF;
                    } else if (advance() == '*') {
                        e = Estado.BLOCK_STAR;
                    }
                    break;
                case BLOCK_STAR:
                    if (!hasNext()) {
                        e = Estado.ERRO_COMENTARIO_EOF;
                    } else {
                        char c = advance();
                        if (c == '/') {
                            return;                 // achou "*/": fim do comentário
                        } else if (c != '*') {
                            e = Estado.IN_BLOCK;    // "**" continua em BLOCK_STAR
                        }
                    }
                    break;
                case ERRO_COMENTARIO_EOF:
                    report(startLine, startCol, "comentário de bloco não fechado até o fim do arquivo");
                    return;
                default:
                    throw new IllegalStateException("estado inesperado: " + e);
            }
        }
    }

    // ------------------------------------------------------------------
    // AFD: identificador / palavra reservada  (automatos/afd-identificador.svg)
    // ------------------------------------------------------------------

    private Token scanIdentifier(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        Estado e = Estado.START;

        while (true) {
            switch (e) {
                case START:
                    // primeiro caractere já é letra ou '_' (garantido por nextToken)
                    sb.append(advance());
                    e = Estado.IN_IDENT;
                    break;
                case IN_IDENT:
                    // laço: letra | dígito | '_'  -> continua em IN_IDENT
                    if (hasNext() && (isLetter(peek()) || isDigit(peek()) || peek() == '_')) {
                        sb.append(advance());
                    } else {
                        e = Estado.FIM_IDENT;   // outro | EOF: não consome
                    }
                    break;
                case FIM_IDENT:
                    String lexema = sb.toString();
                    TokenType tipo = KEYWORDS.contains(lexema) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
                    return new Token(tipo, lexema, startLine, startCol);
                default:
                    throw new IllegalStateException("estado inesperado: " + e);
            }
        }
    }

    // ------------------------------------------------------------------
    // AFD: string  (automatos/afd-string.svg)
    // ------------------------------------------------------------------

    private Token scanString(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        Estado e = Estado.START;
        boolean temErro = false;   // escape inválido: a string é descartada
        int escLine = 0;
        int escCol = 0;

        while (true) {
            switch (e) {
                case START:
                    sb.append(advance());       // aspa de abertura
                    e = Estado.IN_STRING;
                    break;
                case IN_STRING:
                    if (!hasNext() || peek() == '\n') {
                        e = Estado.ERRO_NAO_FECHADA;
                    } else if (peek() == '"') {
                        sb.append(advance());   // aspa de fechamento
                        e = Estado.FIM_STRING;
                    } else if (peek() == '\\') {
                        escLine = line;
                        escCol = col;
                        sb.append(advance());
                        e = Estado.IN_ESCAPE;
                    } else {
                        sb.append(advance());   // qualquer outro caractere: laço
                    }
                    break;
                case IN_ESCAPE:
                    if (hasNext() && isEscape(peek(), '"')) {
                        sb.append(advance());   // \" \\ \n \t
                        e = Estado.IN_STRING;
                    } else {
                        e = Estado.ERRO_ESCAPE;
                    }
                    break;
                case ERRO_ESCAPE:
                    report(escLine, escCol, "sequência de escape inválida em string");
                    temErro = true;
                    if (hasNext() && peek() != '\n') {
                        advance();              // descarta o caractere inválido
                    }
                    e = Estado.IN_STRING;       // recuperação: volta a IN_STRING
                    break;
                case ERRO_NAO_FECHADA:
                    report(startLine, startCol, "string não fechada ("
                            + (hasNext() ? "fim de linha" : "fim de arquivo") + ")");
                    return null;
                case FIM_STRING:
                    return temErro ? null
                            : new Token(TokenType.STRING_LIT, sb.toString(), startLine, startCol);
                default:
                    throw new IllegalStateException("estado inesperado: " + e);
            }
        }
    }

    // ------------------------------------------------------------------
    // AFD: literal numérico  dígito+ ( "." dígito+ )?
    // ------------------------------------------------------------------

    private Token scanNumber(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        Estado e = Estado.START;

        while (true) {
            switch (e) {
                case START:
                    sb.append(advance());       // primeiro dígito
                    e = Estado.IN_INT;
                    break;
                case IN_INT:
                    if (hasNext() && isDigit(peek())) {
                        sb.append(advance());
                    } else if (hasNext() && peek() == '.' && isDigit(peekNext())) {
                        // só entra na parte fracionária se houver dígito depois do ponto;
                        // assim "3." vira o inteiro 3 e o '.' é tratado à parte
                        sb.append(advance());   // '.'
                        e = Estado.IN_FRAC;
                    } else {
                        e = Estado.FIM_INT;
                    }
                    break;
                case IN_FRAC:
                    if (hasNext() && isDigit(peek())) {
                        sb.append(advance());
                    } else {
                        e = Estado.FIM_DOUBLE;
                    }
                    break;
                case FIM_INT:
                    return new Token(TokenType.INT_LIT, sb.toString(), startLine, startCol);
                case FIM_DOUBLE:
                    return new Token(TokenType.DOUBLE_LIT, sb.toString(), startLine, startCol);
                default:
                    throw new IllegalStateException("estado inesperado: " + e);
            }
        }
    }

    // ------------------------------------------------------------------
    // AFD: literal char  'x'  ou  '\n'
    // ------------------------------------------------------------------

    private Token scanChar(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        Estado e = Estado.START;
        boolean temErro = false;
        String motivo = "char mal formado";
        int escLine = 0;
        int escCol = 0;

        while (true) {
            switch (e) {
                case START:
                    sb.append(advance());       // aspa simples de abertura
                    e = Estado.IN_CHAR;
                    break;
                case IN_CHAR:
                    if (!hasNext() || peek() == '\n') {
                        motivo = "char não fechado";
                        e = Estado.ERRO_CHAR;
                    } else if (peek() == '\'') {
                        motivo = "char vazio";
                        e = Estado.ERRO_CHAR;
                    } else if (peek() == '\\') {
                        escLine = line;
                        escCol = col;
                        sb.append(advance());
                        e = Estado.IN_CHAR_ESCAPE;
                    } else {
                        sb.append(advance());   // o único caractere permitido
                        e = Estado.FECHA_CHAR;
                    }
                    break;
                case IN_CHAR_ESCAPE:
                    if (hasNext() && isEscape(peek(), '\'')) {
                        sb.append(advance());
                    } else {
                        report(escLine, escCol, "sequência de escape inválida em char");
                        temErro = true;
                        if (hasNext() && peek() != '\n') {
                            advance();
                        }
                    }
                    e = Estado.FECHA_CHAR;
                    break;
                case FECHA_CHAR:
                    if (hasNext() && peek() == '\'') {
                        sb.append(advance());
                        e = Estado.FIM_CHAR;
                    } else {
                        motivo = "char não fechado ou com mais de um caractere";
                        e = Estado.ERRO_CHAR;
                    }
                    break;
                case ERRO_CHAR:
                    // recuperação: descarta até a aspa de fechamento ou o fim da linha
                    while (hasNext() && peek() != '\n' && peek() != '\'') {
                        advance();
                    }
                    if (hasNext() && peek() == '\'') {
                        advance();
                    }
                    report(startLine, startCol, motivo);
                    return null;
                case FIM_CHAR:
                    return temErro ? null
                            : new Token(TokenType.CHAR_LIT, sb.toString(), startLine, startCol);
                default:
                    throw new IllegalStateException("estado inesperado: " + e);
            }
        }
    }

    // ------------------------------------------------------------------
    // Operadores e delimitadores (maximal munch com lookahead de 1 caractere)
    // ------------------------------------------------------------------

    private Token scanOperatorOrDelimiter(int startLine, int startCol) {
        char c = advance();

        switch (c) {
            case '+': case '-': case '*': case '/': case '%':
                return new Token(TokenType.OPERATOR, String.valueOf(c), startLine, startCol);

            case '=':
                return new Token(TokenType.OPERATOR, match('=') ? "==" : "=", startLine, startCol);
            case '!':
                return new Token(TokenType.OPERATOR, match('=') ? "!=" : "!", startLine, startCol);
            case '<':
                return new Token(TokenType.OPERATOR, match('=') ? "<=" : "<", startLine, startCol);
            case '>':
                return new Token(TokenType.OPERATOR, match('=') ? ">=" : ">", startLine, startCol);

            case '&':
                if (match('&')) {
                    return new Token(TokenType.OPERATOR, "&&", startLine, startCol);
                }
                report(startLine, startCol, "'&' isolado (esperado '&&')");
                return null;
            case '|':
                if (match('|')) {
                    return new Token(TokenType.OPERATOR, "||", startLine, startCol);
                }
                report(startLine, startCol, "'|' isolado (esperado '||')");
                return null;

            case '(': case ')': case '{': case '}': case '[': case ']': case ',': case ';':
                return new Token(TokenType.DELIMITER, String.valueOf(c), startLine, startCol);

            default:
                report(startLine, startCol, "caractere inválido " + descreve(c));
                return null;
        }
    }
}
