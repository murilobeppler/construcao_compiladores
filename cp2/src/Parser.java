import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Parser {

    private static final Set<String> TIPOS = Set.of("bool", "char", "double", "int", "string");
    private static final Set<String> FIRST_DECLTOP = Set.of("bool", "char", "double", "int", "string", "void");
    private static final Set<String> FIRST_STMT = Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{");
    private static final Set<String> FIRST_EXPR = Set.of("!", "(", "-", "CHAR_LIT", "DOUBLE_LIT", "ID", "INT_LIT", "STRING_LIT", "false", "true");
    private static final Set<String> FIRST_PRIMARY = Set.of("(", "CHAR_LIT", "DOUBLE_LIT", "ID", "INT_LIT", "STRING_LIT", "false", "true");
    private static final Set<String> FOLLOW_DECLSTOP = Set.of("EOF");
    private static final Set<String> FOLLOW_INITOPT = Set.of(";");
    private static final Set<String> FOLLOW_PARAMS = Set.of(")");
    private static final Set<String> FOLLOW_LISTAPARAMSREST = Set.of(")");
    private static final Set<String> FOLLOW_LISTASTMT = Set.of("}");
    private static final Set<String> FOLLOW_ELSEOPT = Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}");
    private static final Set<String> FOLLOW_RETURNOPT = Set.of(";");
    private static final Set<String> FOLLOW_ARGS = Set.of(")");
    private static final Set<String> FOLLOW_LISTAARGSREST = Set.of(")");
    private static final Set<String> FOLLOW_ORREST = Set.of(")", ",", ";");
    private static final Set<String> FOLLOW_ANDREST = Set.of(")", ",", ";", "||");
    private static final Set<String> FOLLOW_EQREST = Set.of("&&", ")", ",", ";", "||");
    private static final Set<String> FOLLOW_RELREST = Set.of("!=", "&&", ")", ",", ";", "==", "||");
    private static final Set<String> FOLLOW_ADDREST = Set.of("!=", "&&", ")", ",", ";", "<", "<=", "==", ">", ">=", "||");
    private static final Set<String> FOLLOW_MULREST = Set.of("!=", "&&", ")", "+", ",", "-", ";", "<", "<=", "==", ">", ">=", "||");
    private static final Set<String> FOLLOW_PRIMARYIDREST = Set.of("!=", "%", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "||");
    private static final Set<String> FOLLOW_DECLTOP = Set.of("EOF", "bool", "char", "double", "int", "string", "void");
    private static final Set<String> FOLLOW_DECLTOPRESTO = Set.of("EOF", "bool", "char", "double", "int", "string", "void");
    private static final Set<String> FOLLOW_TIPO = Set.of("ID");
    private static final Set<String> FOLLOW_STMT = Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}");
    private static final Set<String> FOLLOW_STMTID = Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}");
    private static final Set<String> FOLLOW_ELSETAIL = Set.of("ID", "bool", "char", "double", "if", "int", "return", "string", "while", "{", "}");
    private static final Set<String> FOLLOW_UNARY = Set.of("!=", "%", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "||");
    private static final Set<String> FOLLOW_PRIMARY = Set.of("!=", "%", "&&", ")", "*", "+", ",", "-", "/", ";", "<", "<=", "==", ">", ">=", "||");

    private static final Set<String> OP_EQ = Set.of("==", "!=");
    private static final Set<String> OP_REL = Set.of("<", "<=", ">", ">=");
    private static final Set<String> OP_ADD = Set.of("+", "-");
    private static final Set<String> OP_MUL = Set.of("*", "/", "%");
    private static final Set<String> LITERAIS =
            Set.of("INT_LIT", "DOUBLE_LIT", "STRING_LIT", "CHAR_LIT", "true", "false");

    private static final Set<String> ANCORAS = Set.of(";", "}", ")");

    private static final String FIM_EXPR = "operador ou fim da expressão (')', ',' ou ';')";

    private final Scanner scanner;
    private Token lookahead;
    private int indice = 0;              // quantos tokens já foram consumidos
    private int indiceUltimoErro = -1;   // índice do token onde o último erro foi reportado
    private final List<SyntaxError> errors = new ArrayList<>();

    public Parser(Scanner scanner) {
        this.scanner = scanner;
        this.lookahead = scanner.nextToken();
    }

    public boolean parse() {
        parsePrograma();
        return errors.isEmpty() && scanner.getErrors().isEmpty();
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    public List<LexicalError> getLexicalErrors() {
        return scanner.getErrors();
    }

    private static String key(Token t) {
        switch (t.type) {
            case IDENTIFIER: return "ID";
            case INT_LIT: return "INT_LIT";
            case DOUBLE_LIT: return "DOUBLE_LIT";
            case STRING_LIT: return "STRING_LIT";
            case CHAR_LIT: return "CHAR_LIT";
            case EOF: return "EOF";
            default: return t.lexeme;   // KEYWORD, OPERATOR, DELIMITER
        }
    }

    private boolean is(String terminal) {
        return key(lookahead).equals(terminal);
    }

    private boolean in(Set<String> conjunto) {
        return conjunto.contains(key(lookahead));
    }

    /** Consome o lookahead. No EOF não avança (o EOF fica "grudado"). */
    private void advance() {
        if (lookahead.type != TokenType.EOF) {
            lookahead = scanner.nextToken();
            indice++;
        }
    }

    /** Terminal vira match(): consome se bater; senão reporta erro (sem consumir). */
    private void match(String terminal) {
        if (is(terminal)) {
            advance();
        } else {
            erro("esperado " + nome(terminal) + ", mas encontrado " + encontrado());
        }
    }

    private static String nome(String terminal) {
        switch (terminal) {
            case "ID": return "identificador";
            case "EOF": return "fim do arquivo";
            default: return "'" + terminal + "'";
        }
    }

    private String encontrado() {
        switch (lookahead.type) {
            case EOF: return "fim do arquivo";
            case IDENTIFIER: return "identificador '" + lookahead.lexeme + "'";
            case INT_LIT: case DOUBLE_LIT: case STRING_LIT: case CHAR_LIT:
                return "literal " + lookahead.lexeme;
            default: return "'" + lookahead.lexeme + "'";
        }
    }

    /**
     * Registra um erro na posição do lookahead. Se já houve erro neste mesmo token,
     * não registra de novo (evita erros em cascata para um único problema).
     */
    private void erro(String mensagem) {
        if (indice == indiceUltimoErro) {
            return;
        }
        indiceUltimoErro = indice;
        errors.add(new SyntaxError(lookahead.line, lookahead.col, mensagem));
    }

    private void sincroniza(Set<String> sync) {
        while (!is("EOF") && !in(sync)) {
            advance();
        }
    }

    private static Set<String> uniao(Set<String> a, Set<String> b) {
        Set<String> r = new HashSet<>(a);
        r.addAll(b);
        return r;
    }

    /** Erro em parseA(): reporta e sincroniza em FOLLOW(A) ∪ âncoras. */
    private void recupera(String esperado, Set<String> follow) {
        erro("esperado " + esperado + ", mas encontrado " + encontrado());
        sincroniza(uniao(follow, ANCORAS));
    }

    // Programa -> DeclsTop EOF
    private void parsePrograma() {
        parseDeclsTop();
        match("EOF");
    }

    // DeclsTop -> DeclTop DeclsTop | ε
    private void parseDeclsTop() {
        if (in(FIRST_DECLTOP)) {
            parseDeclTop();
            parseDeclsTop();
        } else if (in(FOLLOW_DECLSTOP)) {
            // ε: só aceito porque o lookahead é EOF
        } else {
            erro("esperado declaração (tipo ou 'void'), mas encontrado " + encontrado());
            sincroniza(uniao(FIRST_DECLTOP, FOLLOW_DECLSTOP));   // consome >= 1 token aqui
            parseDeclsTop();
        }
    }

    // DeclTop -> 'void' ID FuncResto | Tipo ID DeclTopResto
    private void parseDeclTop() {
        if (is("void")) {
            match("void");
            match("ID");
            parseFuncResto();
        } else if (in(TIPOS)) {
            parseTipo();
            match("ID");
            parseDeclTopResto();
        } else {
            recupera("declaração", FOLLOW_DECLTOP);
        }
    }

    // DeclTopResto -> FuncResto | VarResto
    private void parseDeclTopResto() {
        if (is("(")) {
            parseFuncResto();
        } else if (is("=") || is(";")) {          // FIRST(VarResto)
            parseVarResto();
        } else {
            recupera("'(', '=' ou ';'", FOLLOW_DECLTOPRESTO);
            if (is(";")) {
                advance();                          // o ';' encerra a declaração abandonada
            }
        }
    }

    // FuncResto -> '(' Params ')' Bloco
    private void parseFuncResto() {
        match("(");
        parseParams();
        match(")");
        parseBloco();
    }

    // VarResto -> InitOpt ';'
    private void parseVarResto() {
        parseInitOpt();
        match(";");
    }

    // InitOpt -> '=' Expr | ε
    private void parseInitOpt() {
        if (is("=")) {
            match("=");
            parseExpr();
        } else if (in(FOLLOW_INITOPT)) {
            // ε
        } else {
            recupera("'=' ou ';'", FOLLOW_INITOPT);
        }
    }

    // Tipo -> 'int' | 'double' | 'bool' | 'char' | 'string'
    private void parseTipo() {
        if (in(TIPOS)) {
            advance();
        } else {
            recupera("tipo (int, double, bool, char ou string)", FOLLOW_TIPO);
        }
    }

    // Params -> ListaParams | ε
    private void parseParams() {
        if (in(TIPOS)) {                           // FIRST(ListaParams)
            parseListaParams();
        } else if (in(FOLLOW_PARAMS)) {
            // ε
        } else {
            recupera("parâmetro ou ')'", FOLLOW_PARAMS);
        }
    }

    // ListaParams -> Param ListaParamsRest
    private void parseListaParams() {
        parseParam();
        parseListaParamsRest();
    }

    // ListaParamsRest -> ',' Param ListaParamsRest | ε
    private void parseListaParamsRest() {
        if (is(",")) {
            match(",");
            parseParam();
            parseListaParamsRest();
        } else if (in(FOLLOW_LISTAPARAMSREST)) {
            // ε
        } else {
            recupera("',' ou ')'", FOLLOW_LISTAPARAMSREST);
        }
    }

    // Param -> Tipo ID
    private void parseParam() {
        parseTipo();
        match("ID");
    }

    // Bloco -> '{' ListaStmt '}'
    private void parseBloco() {
        match("{");
        parseListaStmt();
        match("}");
    }

    // ListaStmt -> Stmt ListaStmt | ε
    private void parseListaStmt() {
        if (in(FIRST_STMT)) {
            parseStmt();
            parseListaStmt();
        } else if (in(FOLLOW_LISTASTMT)) {
            // ε: lookahead é '}'
        } else if (is("EOF")) {
            // Fim do arquivo com bloco aberto: o erro "esperado '}'" é reportado
            // pelo match('}') de parseBloco, com mensagem mais útil.
        } else {
            erro("esperado comando ou '}', mas encontrado " + encontrado());
            sincroniza(uniao(uniao(FIRST_STMT, FOLLOW_LISTASTMT), Set.of(";")));
            if (is(";")) {
                advance();                          // o ';' encerra o comando inválido
            }
            parseListaStmt();                       // recomeça já sincronizado
        }
    }

    private void parseStmt() {
        if (in(TIPOS)) {
            parseTipo();
            match("ID");
            parseInitOpt();
            match(";");
        } else if (is("ID")) {
            match("ID");
            parseStmtId();
        } else if (is("if")) {
            match("if");
            match("(");
            parseExpr();
            match(")");
            parseBloco();
            parseElseOpt();
        } else if (is("while")) {
            match("while");
            match("(");
            parseExpr();
            match(")");
            parseBloco();
        } else if (is("return")) {
            match("return");
            parseReturnOpt();
            match(";");
        } else if (is("{")) {
            parseBloco();
        } else {
            recupera("comando", FOLLOW_STMT);
        }
    }

    // StmtId -> '=' Expr ';' | '(' Args ')' ';'
    private void parseStmtId() {
        if (is("=")) {
            match("=");
            parseExpr();
            match(";");
        } else if (is("(")) {
            match("(");
            parseArgs();
            match(")");
            match(";");
        } else {
            recupera("'=' ou '('", FOLLOW_STMTID);
            if (is(";")) {
                advance();
            }
        }
    }

    // ElseOpt -> 'else' ElseTail | ε
    private void parseElseOpt() {
        if (is("else")) {
            match("else");
            parseElseTail();
        } else if (in(FOLLOW_ELSEOPT)) {
            // ε: lookahead começa outro comando ou é '}'
        } else {
            recupera("'else', comando ou '}'", FOLLOW_ELSEOPT);
        }
    }

    // ElseTail -> Bloco | 'if' '(' Expr ')' Bloco ElseOpt
    private void parseElseTail() {
        if (is("{")) {
            parseBloco();
        } else if (is("if")) {
            match("if");
            match("(");
            parseExpr();
            match(")");
            parseBloco();
            parseElseOpt();
        } else {
            recupera("'{' ou 'if'", FOLLOW_ELSETAIL);
        }
    }

    // ReturnOpt -> Expr | ε
    private void parseReturnOpt() {
        if (in(FIRST_EXPR)) {
            parseExpr();
        } else if (in(FOLLOW_RETURNOPT)) {
            // ε: 'return;'
        } else {
            recupera("expressão ou ';'", FOLLOW_RETURNOPT);
        }
    }

    // Args -> ListaArgs | ε
    private void parseArgs() {
        if (in(FIRST_EXPR)) {
            parseListaArgs();
        } else if (in(FOLLOW_ARGS)) {
            // ε
        } else {
            recupera("argumento ou ')'", FOLLOW_ARGS);
        }
    }

    // ListaArgs -> Expr ListaArgsRest
    private void parseListaArgs() {
        parseExpr();
        parseListaArgsRest();
    }

    // ListaArgsRest -> ',' Expr ListaArgsRest | ε
    private void parseListaArgsRest() {
        if (is(",")) {
            match(",");
            parseExpr();
            parseListaArgsRest();
        } else if (in(FOLLOW_LISTAARGSREST)) {
            // ε
        } else {
            recupera("',' ou ')'", FOLLOW_LISTAARGSREST);
        }
    }

    // Expr -> OrExpr
    private void parseExpr() {
        parseOrExpr();
    }

    // OrExpr -> AndExpr OrRest
    private void parseOrExpr() {
        parseAndExpr();
        parseOrRest();
    }

    // OrRest -> '||' AndExpr OrRest | ε
    private void parseOrRest() {
        if (is("||")) {
            match("||");
            parseAndExpr();
            parseOrRest();
        } else if (in(FOLLOW_ORREST)) {
            // ε
        } else {
            recupera(FIM_EXPR, FOLLOW_ORREST);
        }
    }

    // AndExpr -> EqExpr AndRest
    private void parseAndExpr() {
        parseEqExpr();
        parseAndRest();
    }

    // AndRest -> '&&' EqExpr AndRest | ε
    private void parseAndRest() {
        if (is("&&")) {
            match("&&");
            parseEqExpr();
            parseAndRest();
        } else if (in(FOLLOW_ANDREST)) {
            // ε
        } else {
            recupera(FIM_EXPR, FOLLOW_ANDREST);
        }
    }

    // EqExpr -> RelExpr EqRest
    private void parseEqExpr() {
        parseRelExpr();
        parseEqRest();
    }

    // EqRest -> '==' RelExpr EqRest | '!=' RelExpr EqRest | ε
    private void parseEqRest() {
        if (in(OP_EQ)) {
            advance();
            parseRelExpr();
            parseEqRest();
        } else if (in(FOLLOW_EQREST)) {
            // ε
        } else {
            recupera(FIM_EXPR, FOLLOW_EQREST);
        }
    }

    // RelExpr -> AddExpr RelRest
    private void parseRelExpr() {
        parseAddExpr();
        parseRelRest();
    }

    // RelRest -> ('<' | '<=' | '>' | '>=') AddExpr RelRest | ε
    private void parseRelRest() {
        if (in(OP_REL)) {
            advance();
            parseAddExpr();
            parseRelRest();
        } else if (in(FOLLOW_RELREST)) {
            // ε
        } else {
            recupera(FIM_EXPR, FOLLOW_RELREST);
        }
    }

    // AddExpr -> MulExpr AddRest
    private void parseAddExpr() {
        parseMulExpr();
        parseAddRest();
    }

    // AddRest -> ('+' | '-') MulExpr AddRest | ε
    private void parseAddRest() {
        if (in(OP_ADD)) {
            advance();
            parseMulExpr();
            parseAddRest();
        } else if (in(FOLLOW_ADDREST)) {
            // ε
        } else {
            recupera(FIM_EXPR, FOLLOW_ADDREST);
        }
    }

    // MulExpr -> Unary MulRest
    private void parseMulExpr() {
        parseUnary();
        parseMulRest();
    }

    // MulRest -> ('*' | '/' | '%') Unary MulRest | ε
    private void parseMulRest() {
        if (in(OP_MUL)) {
            advance();
            parseUnary();
            parseMulRest();
        } else if (in(FOLLOW_MULREST)) {
            // ε
        } else {
            recupera(FIM_EXPR, FOLLOW_MULREST);
        }
    }

    // Unary -> '!' Unary | '-' Unary | Primary
    private void parseUnary() {
        if (is("!") || is("-")) {
            advance();
            parseUnary();
        } else if (in(FIRST_PRIMARY)) {
            parsePrimary();
        } else {
            recupera("expressão", FOLLOW_UNARY);
        }
    }

    // Primary -> literal | 'true' | 'false' | '(' Expr ')' | ID PrimaryIdRest
    private void parsePrimary() {
        if (in(LITERAIS)) {
            advance();
        } else if (is("(")) {
            match("(");
            parseExpr();
            match(")");
        } else if (is("ID")) {
            match("ID");
            parsePrimaryIdRest();
        } else {
            recupera("expressão", FOLLOW_PRIMARY);
        }
    }

    // PrimaryIdRest -> '(' Args ')' | ε
    private void parsePrimaryIdRest() {
        if (is("(")) {
            match("(");
            parseArgs();
            match(")");
        } else if (in(FOLLOW_PRIMARYIDREST)) {
            // ε: era apenas uma variável
        } else {
            recupera(FIM_EXPR, FOLLOW_PRIMARYIDREST);
        }
    }
}
