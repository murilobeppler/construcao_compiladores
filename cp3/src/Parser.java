import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ast.*;

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

    public ParseResult parse() {
        Program raiz = parsePrograma();
        boolean aceito = errors.isEmpty() && scanner.getErrors().isEmpty();
        return new ParseResult(aceito ? raiz : null, errors, scanner.getErrors());
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

    private void advance() {
        if (lookahead.type != TokenType.EOF) {
            lookahead = scanner.nextToken();
            indice++;
        }
    }

    private Token match(String terminal) {
        if (is(terminal)) {
            Token t = lookahead;
            advance();
            return t;
        }
        erro("esperado " + nome(terminal) + ", mas encontrado " + encontrado());
        return null;
    }

    private static String lexema(Token t) {
        return t == null ? null : t.lexeme;
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

    // =====================================================================
    // Programa e declarações
    // =====================================================================

    // Programa -> DeclsTop EOF
    private Program parsePrograma() {
        Token inicio = lookahead;
        List<TopDecl> decls = new ArrayList<>();
        parseDeclsTop(decls);
        match("EOF");
        return new Program(decls, inicio.line, inicio.col);
    }

    // DeclsTop -> DeclTop DeclsTop | ε        (não vira nó: vira a lista do Program)
    private void parseDeclsTop(List<TopDecl> decls) {
        if (in(FIRST_DECLTOP)) {
            decls.add(parseDeclTop());
            parseDeclsTop(decls);
        } else if (in(FOLLOW_DECLSTOP)) {
            // ε: só aceito porque o lookahead é EOF
        } else {
            erro("esperado declaração (tipo ou 'void'), mas encontrado " + encontrado());
            sincroniza(uniao(FIRST_DECLTOP, FOLLOW_DECLSTOP));   // consome >= 1 token aqui
            parseDeclsTop(decls);
        }
    }

    // DeclTop -> 'void' ID FuncResto | Tipo ID DeclTopResto
    private TopDecl parseDeclTop() {
        Token inicio = lookahead;
        if (is("void")) {
            match("void");
            Token id = match("ID");
            FuncResto f = parseFuncResto();
            return new ProcDecl(lexema(id), f.params, f.body, inicio.line, inicio.col);
        } else if (in(TIPOS)) {
            TypeNode tipo = parseTipo();
            Token id = match("ID");
            return parseDeclTopResto(inicio, tipo, lexema(id));
        } else {
            recupera("declaração", FOLLOW_DECLTOP);
            return null;
        }
    }

    // DeclTopResto -> FuncResto | VarResto   (fatoração: não vira nó; decide FuncDecl x VarDecl)
    private TopDecl parseDeclTopResto(Token inicio, TypeNode tipo, String nome) {
        if (is("(")) {
            FuncResto f = parseFuncResto();
            return new FuncDecl(tipo, nome, f.params, f.body, inicio.line, inicio.col);
        } else if (is("=") || is(";")) {          // FIRST(VarResto)
            Expr init = parseVarResto();
            return new VarDecl(tipo, nome, init, inicio.line, inicio.col);
        } else {
            recupera("'(', '=' ou ';'", FOLLOW_DECLTOPRESTO);
            if (is(";")) {
                advance();                          
            }
            return null;
        }
    }

    private static class FuncResto {
        final List<Param> params;
        final Block body;

        FuncResto(List<Param> params, Block body) {
            this.params = params;
            this.body = body;
        }
    }

    private FuncResto parseFuncResto() {
        match("(");
        List<Param> params = parseParams();
        match(")");
        Block body = parseBloco();
        return new FuncResto(params, body);
    }

    // VarResto -> InitOpt ';'                (fatoração: não vira nó; devolve o init)
    private Expr parseVarResto() {
        Expr init = parseInitOpt();
        match(";");
        return init;
    }

    // InitOpt -> '=' Expr | ε                (vira o atributo init, null no ε)
    private Expr parseInitOpt() {
        if (is("=")) {
            match("=");
            return parseExpr();
        } else if (in(FOLLOW_INITOPT)) {
            return null;                            // ε
        } else {
            recupera("'=' ou ';'", FOLLOW_INITOPT);
            return null;
        }
    }

    // Tipo -> 'int' | 'double' | 'bool' | 'char' | 'string'
    private TypeNode parseTipo() {
        if (in(TIPOS)) {
            Token t = lookahead;
            advance();
            return new TypeNode(Tipo.deLexema(t.lexeme), t.line, t.col);
        } else {
            recupera("tipo (int, double, bool, char ou string)", FOLLOW_TIPO);
            return null;
        }
    }

    // Params -> ListaParams | ε              (vira a lista de Param; vazia no ε)
    private List<Param> parseParams() {
        List<Param> params = new ArrayList<>();
        if (in(TIPOS)) {                           // FIRST(ListaParams)
            parseListaParams(params);
        } else if (in(FOLLOW_PARAMS)) {
            // ε
        } else {
            recupera("parâmetro ou ')'", FOLLOW_PARAMS);
        }
        return params;
    }

    // ListaParams -> Param ListaParamsRest
    private void parseListaParams(List<Param> params) {
        params.add(parseParam());
        parseListaParamsRest(params);
    }

    // ListaParamsRest -> ',' Param ListaParamsRest | ε
    private void parseListaParamsRest(List<Param> params) {
        if (is(",")) {
            match(",");
            params.add(parseParam());
            parseListaParamsRest(params);
        } else if (in(FOLLOW_LISTAPARAMSREST)) {
            // ε
        } else {
            recupera("',' ou ')'", FOLLOW_LISTAPARAMSREST);
        }
    }

    // Param -> Tipo ID
    private Param parseParam() {
        Token inicio = lookahead;
        TypeNode tipo = parseTipo();
        Token id = match("ID");
        return new Param(tipo, lexema(id), inicio.line, inicio.col);
    }

    // =====================================================================
    // Comandos
    // =====================================================================

    // Bloco -> '{' ListaStmt '}'
    private Block parseBloco() {
        Token inicio = lookahead;
        match("{");
        List<Stmt> stmts = new ArrayList<>();
        parseListaStmt(stmts);
        match("}");
        return new Block(stmts, inicio.line, inicio.col);
    }

    // ListaStmt -> Stmt ListaStmt | ε        (não vira nó: vira a lista do Block)
    private void parseListaStmt(List<Stmt> stmts) {
        if (in(FIRST_STMT)) {
            stmts.add(parseStmt());
            parseListaStmt(stmts);
        } else if (in(FOLLOW_LISTASTMT)) {
            // ε: lookahead é '}'
        } else if (is("EOF")) {
        } else {
            erro("esperado comando ou '}', mas encontrado " + encontrado());
            sincroniza(uniao(uniao(FIRST_STMT, FOLLOW_LISTASTMT), Set.of(";")));
            if (is(";")) {
                advance();                          // o ';' encerra o comando inválido
            }
            parseListaStmt(stmts);                  // recomeça já sincronizado
        }
    }

    private Stmt parseStmt() {
        Token inicio = lookahead;
        if (in(TIPOS)) {
            TypeNode tipo = parseTipo();
            Token id = match("ID");
            Expr init = parseInitOpt();
            match(";");
            return new VarDecl(tipo, lexema(id), init, inicio.line, inicio.col);
        } else if (is("ID")) {
            Token id = match("ID");
            return parseStmtId(id);
        } else if (is("if")) {
            return parseIf();
        } else if (is("while")) {
            match("while");
            match("(");
            Expr cond = parseExpr();
            match(")");
            Block body = parseBloco();
            return new While(cond, body, inicio.line, inicio.col);
        } else if (is("return")) {
            match("return");
            Expr valor = parseReturnOpt();
            match(";");
            return new Return(valor, inicio.line, inicio.col);
        } else if (is("{")) {
            return parseBloco();
        } else {
            recupera("comando", FOLLOW_STMT);
            return null;
        }
    }

    private If parseIf() {
        Token inicio = lookahead;
        match("if");
        match("(");
        Expr cond = parseExpr();
        match(")");
        Block entao = parseBloco();
        Stmt senao = parseElseOpt();
        return new If(cond, entao, senao, inicio.line, inicio.col);
    }

    // StmtId -> '=' Expr ';' | '(' Args ')' ';'   (fatoração: decide Assign x CallStmt)
    private Stmt parseStmtId(Token id) {
        if (is("=")) {
            match("=");
            Expr valor = parseExpr();
            match(";");
            return new Assign(id.lexeme, valor, id.line, id.col);
        } else if (is("(")) {
            match("(");
            List<Expr> args = parseArgs();
            match(")");
            match(";");
            Call chamada = new Call(id.lexeme, args, id.line, id.col);
            return new CallStmt(chamada, id.line, id.col);
        } else {
            recupera("'=' ou '('", FOLLOW_STMTID);
            if (is(";")) {
                advance();
            }
            return null;
        }
    }

    // ElseOpt -> 'else' ElseTail | ε         (vira o atributo elseBranch, null no ε)
    private Stmt parseElseOpt() {
        if (is("else")) {
            match("else");
            return parseElseTail();
        } else if (in(FOLLOW_ELSEOPT)) {
            return null;                            // ε: lookahead começa outro comando ou é '}'
        } else {
            recupera("'else', comando ou '}'", FOLLOW_ELSEOPT);
            return null;
        }
    }

    // ElseTail -> Bloco | 'if' '(' Expr ')' Bloco ElseOpt
    private Stmt parseElseTail() {
        if (is("{")) {
            return parseBloco();
        } else if (is("if")) {
            return parseIf();
        } else {
            recupera("'{' ou 'if'", FOLLOW_ELSETAIL);
            return null;
        }
    }

    // ReturnOpt -> Expr | ε                  (vira o atributo value, null no ε)
    private Expr parseReturnOpt() {
        if (in(FIRST_EXPR)) {
            return parseExpr();
        } else if (in(FOLLOW_RETURNOPT)) {
            return null;                            // ε: 'return;'
        } else {
            recupera("expressão ou ';'", FOLLOW_RETURNOPT);
            return null;
        }
    }

    // Args -> ListaArgs | ε                  (vira a lista de argumentos; vazia no ε)
    private List<Expr> parseArgs() {
        List<Expr> args = new ArrayList<>();
        if (in(FIRST_EXPR)) {
            parseListaArgs(args);
        } else if (in(FOLLOW_ARGS)) {
            // ε
        } else {
            recupera("argumento ou ')'", FOLLOW_ARGS);
        }
        return args;
    }

    // ListaArgs -> Expr ListaArgsRest
    private void parseListaArgs(List<Expr> args) {
        args.add(parseExpr());
        parseListaArgsRest(args);
    }

    // ListaArgsRest -> ',' Expr ListaArgsRest | ε
    private void parseListaArgsRest(List<Expr> args) {
        if (is(",")) {
            match(",");
            args.add(parseExpr());
            parseListaArgsRest(args);
        } else if (in(FOLLOW_LISTAARGSREST)) {
            // ε
        } else {
            recupera("',' ou ')'", FOLLOW_LISTAARGSREST);
        }
    }

    // Expr -> OrExpr
    private Expr parseExpr() {
        return parseOrExpr();
    }

    // OrExpr -> AndExpr OrRest
    private Expr parseOrExpr() {
        Token inicio = lookahead;
        Expr esq = parseAndExpr();
        return parseOrRest(esq, inicio);
    }

    // OrRest -> '||' AndExpr OrRest | ε
    private Expr parseOrRest(Expr esq, Token inicio) {
        if (is("||")) {
            match("||");
            Expr dir = parseAndExpr();
            return parseOrRest(new Bin(BinOp.OR, esq, dir, inicio.line, inicio.col), inicio);
        } else if (in(FOLLOW_ORREST)) {
            return esq;                             // ε
        } else {
            recupera(FIM_EXPR, FOLLOW_ORREST);
            return esq;
        }
    }

    // AndExpr -> EqExpr AndRest
    private Expr parseAndExpr() {
        Token inicio = lookahead;
        Expr esq = parseEqExpr();
        return parseAndRest(esq, inicio);
    }

    // AndRest -> '&&' EqExpr AndRest | ε
    private Expr parseAndRest(Expr esq, Token inicio) {
        if (is("&&")) {
            match("&&");
            Expr dir = parseEqExpr();
            return parseAndRest(new Bin(BinOp.AND, esq, dir, inicio.line, inicio.col), inicio);
        } else if (in(FOLLOW_ANDREST)) {
            return esq;
        } else {
            recupera(FIM_EXPR, FOLLOW_ANDREST);
            return esq;
        }
    }

    // EqExpr -> RelExpr EqRest
    private Expr parseEqExpr() {
        Token inicio = lookahead;
        Expr esq = parseRelExpr();
        return parseEqRest(esq, inicio);
    }

    // EqRest -> '==' RelExpr EqRest | '!=' RelExpr EqRest | ε
    private Expr parseEqRest(Expr esq, Token inicio) {
        if (in(OP_EQ)) {
            BinOp op = BinOp.deSimbolo(lookahead.lexeme);
            advance();
            Expr dir = parseRelExpr();
            return parseEqRest(new Bin(op, esq, dir, inicio.line, inicio.col), inicio);
        } else if (in(FOLLOW_EQREST)) {
            return esq;
        } else {
            recupera(FIM_EXPR, FOLLOW_EQREST);
            return esq;
        }
    }

    // RelExpr -> AddExpr RelRest
    private Expr parseRelExpr() {
        Token inicio = lookahead;
        Expr esq = parseAddExpr();
        return parseRelRest(esq, inicio);
    }

    // RelRest -> ('<' | '<=' | '>' | '>=') AddExpr RelRest | ε
    private Expr parseRelRest(Expr esq, Token inicio) {
        if (in(OP_REL)) {
            BinOp op = BinOp.deSimbolo(lookahead.lexeme);
            advance();
            Expr dir = parseAddExpr();
            return parseRelRest(new Bin(op, esq, dir, inicio.line, inicio.col), inicio);
        } else if (in(FOLLOW_RELREST)) {
            return esq;
        } else {
            recupera(FIM_EXPR, FOLLOW_RELREST);
            return esq;
        }
    }

    // AddExpr -> MulExpr AddRest
    private Expr parseAddExpr() {
        Token inicio = lookahead;
        Expr esq = parseMulExpr();
        return parseAddRest(esq, inicio);
    }

    // AddRest -> ('+' | '-') MulExpr AddRest | ε
    private Expr parseAddRest(Expr esq, Token inicio) {
        if (in(OP_ADD)) {
            BinOp op = BinOp.deSimbolo(lookahead.lexeme);
            advance();
            Expr dir = parseMulExpr();
            return parseAddRest(new Bin(op, esq, dir, inicio.line, inicio.col), inicio);
        } else if (in(FOLLOW_ADDREST)) {
            return esq;
        } else {
            recupera(FIM_EXPR, FOLLOW_ADDREST);
            return esq;
        }
    }

    // MulExpr -> Unary MulRest
    private Expr parseMulExpr() {
        Token inicio = lookahead;
        Expr esq = parseUnary();
        return parseMulRest(esq, inicio);
    }

    // MulRest -> ('*' | '/' | '%') Unary MulRest | ε
    private Expr parseMulRest(Expr esq, Token inicio) {
        if (in(OP_MUL)) {
            BinOp op = BinOp.deSimbolo(lookahead.lexeme);
            advance();
            Expr dir = parseUnary();
            return parseMulRest(new Bin(op, esq, dir, inicio.line, inicio.col), inicio);
        } else if (in(FOLLOW_MULREST)) {
            return esq;
        } else {
            recupera(FIM_EXPR, FOLLOW_MULREST);
            return esq;
        }
    }

    // Unary -> '!' Unary | '-' Unary | Primary
    private Expr parseUnary() {
        if (is("!") || is("-")) {
            Token op = lookahead;
            advance();
            Expr operando = parseUnary();
            return new Unary(UnOp.deSimbolo(op.lexeme), operando, op.line, op.col);
        } else if (in(FIRST_PRIMARY)) {
            return parsePrimary();
        } else {
            recupera("expressão", FOLLOW_UNARY);
            return null;
        }
    }

    // Primary -> literal | 'true' | 'false' | '(' Expr ')' | ID PrimaryIdRest
    private Expr parsePrimary() {
        if (in(LITERAIS)) {
            Token t = lookahead;
            advance();
            return literal(t);
        } else if (is("(")) {
            match("(");
            Expr e = parseExpr();                   // os parênteses não viram nó:
            match(")");                             // a árvore já guarda o agrupamento
            return e;
        } else if (is("ID")) {
            Token id = match("ID");
            return parsePrimaryIdRest(id);
        } else {
            recupera("expressão", FOLLOW_PRIMARY);
            return null;
        }
    }

    /** Nó de literal a partir do token (o lexema de string/char inclui as aspas). */
    private static Expr literal(Token t) {
        switch (t.type) {
            case INT_LIT:
                return new IntLit(t.lexeme, t.line, t.col);
            case DOUBLE_LIT:
                return new DoubleLit(t.lexeme, t.line, t.col);
            case STRING_LIT:
                return new StringLit(semAspas(t.lexeme), t.line, t.col);
            case CHAR_LIT:
                return new CharLit(semAspas(t.lexeme), t.line, t.col);
            default:                                // KEYWORD 'true' ou 'false'
                return new BoolLit(t.lexeme.equals("true"), t.line, t.col);
        }
    }

    private static String semAspas(String lexema) {
        return lexema.length() >= 2 ? lexema.substring(1, lexema.length() - 1) : lexema;
    }

    // PrimaryIdRest -> '(' Args ')' | ε      (fatoração: decide Call x Id)
    private Expr parsePrimaryIdRest(Token id) {
        if (is("(")) {
            match("(");
            List<Expr> args = parseArgs();
            match(")");
            return new Call(id.lexeme, args, id.line, id.col);
        } else if (in(FOLLOW_PRIMARYIDREST)) {
            return new Id(id.lexeme, id.line, id.col);   // ε: era apenas uma variável
        } else {
            recupera(FIM_EXPR, FOLLOW_PRIMARYIDREST);
            return new Id(id.lexeme, id.line, id.col);
        }
    }
}
