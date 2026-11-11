/**
 * Estados dos AFDs. Os nomes de identificador e string são os mesmos dos
 * desenhos em automatos/ (afd-identificador.svg e afd-string.svg).
 */
public enum Estado {
    START,

    // identificador / palavra reservada
    IN_IDENT, FIM_IDENT,

    // string
    IN_STRING, IN_ESCAPE, FIM_STRING, ERRO_NAO_FECHADA, ERRO_ESCAPE,

    // literal numérico
    IN_INT, IN_FRAC, FIM_INT, FIM_DOUBLE,

    // literal char
    IN_CHAR, IN_CHAR_ESCAPE, FECHA_CHAR, FIM_CHAR, ERRO_CHAR,

    // comentário de bloco
    IN_BLOCK, BLOCK_STAR, ERRO_COMENTARIO_EOF
}
