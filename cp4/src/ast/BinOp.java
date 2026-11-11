package ast;

/** Operadores binários, em ordem crescente de precedência (por grupos). */
public enum BinOp {
    OR("||"),
    AND("&&"),
    EQ("=="), NE("!="),
    LT("<"), LE("<="), GT(">"), GE(">="),
    ADD("+"), SUB("-"),
    MUL("*"), DIV("/"), MOD("%");

    private final String simbolo;

    BinOp(String simbolo) {
        this.simbolo = simbolo;
    }

    public String simbolo() {
        return simbolo;
    }

    /** Converte o lexema do operador; devolve null se não for binário. */
    public static BinOp deSimbolo(String s) {
        for (BinOp op : values()) {
            if (op.simbolo.equals(s)) {
                return op;
            }
        }
        return null;
    }
}
