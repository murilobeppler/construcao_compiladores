package symbols;

/**
 * O que um nome é. Quem pergunta:
 * - CP5 (tipos): só FUNCAO pode ser chamada numa expressão; FUNCAO ou PROCEDIMENTO num
 *   comando de chamada; só VARIAVEL ou PARAMETRO pode receber atribuição;
 * - geração de código: PARAMETRO e VARIAVEL local ficam em lugares diferentes do registro
 *   de ativação.
 */
public enum Categoria {
    VARIAVEL("variável"),
    PARAMETRO("parâmetro"),
    FUNCAO("função"),
    PROCEDIMENTO("procedimento");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}

