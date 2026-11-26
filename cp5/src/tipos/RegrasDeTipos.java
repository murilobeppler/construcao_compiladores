package tipos;

import ast.BinOp;
import ast.Tipo;
import ast.UnOp;

/**
 * As regras de tipagem de operadores e de compatibilidade de atribuição, num só lugar.
 *
 * Cada método corresponde a regras do papel (tipos-a-mao.pdf), com o nome da regra entre
 * colchetes. O TypeChecker não decide tipos de operadores sozinho: ele chama estes métodos.
 * Por não depender da AST nem do parser, estas regras são testadas isoladamente (RegrasTest).
 *
 * Convenção: um método de operador devolve o tipo do resultado, ou null quando a regra
 * recusa os operandos (o chamador reporta o erro). Com qualquer operando ERRO, devolve ERRO
 * sem recusar: o erro já foi reportado e não deve gerar outro (regra [ERRO]).
 */
public final class RegrasDeTipos {

    private RegrasDeTipos() {
    }

    /** int ou double. */
    public static boolean numerico(Tipo t) {
        return t == Tipo.INT || t == Tipo.DOUBLE;
    }

    /**
     * Operadores binários.
     *
     * [ARIT-INT]    e1:int  e2:int                          =>  e1 op e2 : int     op em + - * /
     * [ARIT-DOUBLE] e1:num  e2:num, pelo menos um double     =>  e1 op e2 : double  op em + - * /
     * [MOD]         e1:int  e2:int                          =>  e1 % e2  : int
     * [REL-NUM]     e1:num  e2:num                          =>  e1 op e2 : bool    op em < <= > >=
     * [REL-CHAR]    e1:char e2:char                         =>  e1 op e2 : bool    op em < <= > >=
     * [IGUAL-NUM]   e1:num  e2:num                          =>  e1 op e2 : bool    op em == !=
     * [IGUAL]       e1:T    e2:T,   T em {bool, char}       =>  e1 op e2 : bool    op em == !=
     * [LOG]         e1:bool e2:bool                         =>  e1 op e2 : bool    op em && ||
     */
    public static Tipo binario(BinOp op, Tipo a, Tipo b) {
        if (op == null || a == null || b == null) {
            return Tipo.ERRO;
        }
        if (a == Tipo.ERRO || b == Tipo.ERRO) {
            return Tipo.ERRO;                                         // [ERRO]
        }
        switch (op) {
            case ADD: case SUB: case MUL: case DIV:
                if (a == Tipo.INT && b == Tipo.INT) {
                    return Tipo.INT;                                  // [ARIT-INT]
                }
                if (numerico(a) && numerico(b)) {
                    return Tipo.DOUBLE;                               // [ARIT-DOUBLE] (int -> double)
                }
                return null;
            case MOD:
                return a == Tipo.INT && b == Tipo.INT ? Tipo.INT : null;   // [MOD]
            case LT: case LE: case GT: case GE:
                if (numerico(a) && numerico(b)) {
                    return Tipo.BOOL;                                 // [REL-NUM]
                }
                if (a == Tipo.CHAR && b == Tipo.CHAR) {
                    return Tipo.BOOL;                                 // [REL-CHAR]
                }
                return null;
            case EQ: case NE:
                if (numerico(a) && numerico(b)) {
                    return Tipo.BOOL;                                 // [IGUAL-NUM]
                }
                if (a == b && (a == Tipo.BOOL || a == Tipo.CHAR)) {
                    return Tipo.BOOL;                                 // [IGUAL]
                }
                return null;                                          // string, tipos diferentes
            case AND: case OR:
                return a == Tipo.BOOL && b == Tipo.BOOL ? Tipo.BOOL : null; // [LOG]
            default:
                return null;
        }
    }

    /**
     * Operadores unários.
     *
     * [NOT]  e:bool  =>  !e : bool
     * [NEG]  e:T, T em {int, double}  =>  -e : T
     */
    public static Tipo unario(UnOp op, Tipo t) {
        if (op == null || t == null || t == Tipo.ERRO) {
            return Tipo.ERRO;                                         // [ERRO]
        }
        switch (op) {
            case NOT:
                return t == Tipo.BOOL ? Tipo.BOOL : null;             // [NOT]
            case NEG:
                return numerico(t) ? t : null;                        // [NEG]
            default:
                return null;
        }
    }

    /**
     * Compatibilidade de atribuição, usada em todo contexto em que um valor vai para um
     * lugar de tipo conhecido: atribuição, inicialização, argumento e return.
     *
     * [COMPAT-IGUAL]  T <= T
     * [COMPAT-AMPLIA] int <= double     (int -> double implícito; o inverso nunca)
     * [ERRO]          ERRO <= T  e  T <= ERRO
     */
    public static boolean atribuivel(Tipo destino, Tipo origem) {
        if (destino == null || origem == null || destino == Tipo.ERRO || origem == Tipo.ERRO) {
            return true;                                              // [ERRO]
        }
        if (destino == Tipo.VOID || origem == Tipo.VOID) {
            return false;                                             // void não é valor
        }
        if (destino == origem) {
            return true;                                              // [COMPAT-IGUAL]
        }
        return destino == Tipo.DOUBLE && origem == Tipo.INT;          // [COMPAT-AMPLIA]
    }

    /** Maior literal int: a palavra da TAM tem 16 bits (maxint = 32767). Regra [LIT-INT]. */
    public static final int MAX_INT = 32767;
}
