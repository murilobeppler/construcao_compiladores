/** Categorias de token da linguagem (ver especificacao-lexica.md). */
public enum TokenType {
    IDENTIFIER,   // total, x1, _aux
    KEYWORD,      // if, while, int, return, true, false ...
    INT_LIT,      // 10
    DOUBLE_LIT,   // 3.14
    STRING_LIT,   // "ok"
    CHAR_LIT,     // 'a'
    OPERATOR,     // = == < <= + && ...
    DELIMITER,    // ( ) { } [ ] , ;
    EOF           // fim do arquivo
}
