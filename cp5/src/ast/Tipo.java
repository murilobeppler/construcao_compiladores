package ast;

/**
 * Tipos da linguagem.
 *
 * Os cinco primeiros são os tipos básicos (palavras reservadas de tipo). CP5 acrescentou:
 * - VOID: "tipo" de retorno de procedimento. Nenhuma variável, parâmetro ou expressão
 *   válida tem esse tipo; ele só aparece em Symbol.type de procedimentos.
 * - ERRO: tipo de uma expressão em que já foi reportado um erro. É compatível com tudo e
 *   não gera nova mensagem (evita erros em cascata). Ver tipos/RegrasDeTipos.
 */
public enum Tipo {
    INT("int"), DOUBLE("double"), BOOL("bool"), CHAR("char"), STRING("string"),
    VOID("void"), ERRO("<erro>");

    private final String lexema;

    Tipo(String lexema) {
        this.lexema = lexema;
    }

    public String lexema() {
        return lexema;
    }

    /** Converte o lexema da palavra reservada de tipo; devolve null se não for um tipo básico. */
    public static Tipo deLexema(String s) {
        for (Tipo t : values()) {
            if (t != VOID && t != ERRO && t.lexema.equals(s)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return lexema;
    }
}
