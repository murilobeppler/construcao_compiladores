package ast;

/** Os cinco tipos básicos da linguagem (palavras reservadas de tipo, sem 'void'). */
public enum Tipo {
    INT("int"), DOUBLE("double"), BOOL("bool"), CHAR("char"), STRING("string");

    private final String lexema;

    Tipo(String lexema) {
        this.lexema = lexema;
    }

    public String lexema() {
        return lexema;
    }

    /** Converte o lexema da palavra reservada; devolve null se não for um tipo. */
    public static Tipo deLexema(String s) {
        for (Tipo t : values()) {
            if (t.lexema.equals(s)) {
                return t;
            }
        }
        return null;
    }
}
