package ast;

/** Operadores unários. */
public enum UnOp {
    NOT("!"), NEG("-");

    private final String simbolo;

    UnOp(String simbolo) {
        this.simbolo = simbolo;
    }

    public String simbolo() {
        return simbolo;
    }

    public static UnOp deSimbolo(String s) {
        for (UnOp op : values()) {
            if (op.simbolo.equals(s)) {
                return op;
            }
        }
        return null;
    }
}
