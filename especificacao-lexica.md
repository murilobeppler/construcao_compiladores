# Especificação Léxica — Checkpoint 1

> RASCUNHO: revisem cada decisão e troquem o que o grupo quiser diferente.
> O scanner deve implementar exatamente o que está escrito aqui.

## 1. Alfabeto de entrada

- Letras ASCII: `a-z`, `A-Z`
- Dígitos: `0-9`
- Sublinhado: `_`
- Símbolos de operadores e delimitadores: `+ - * / % = ! < > & | ( ) { } [ ] , ; . " ' \`
- Espaço em branco: espaço, `\t`, `\n`, `\r`
- Dentro de strings, literais `char` e comentários, qualquer caractere é aceito (inclusive acentos), exceto os terminadores de cada construção.
- Qualquer outro caractere fora dessas situações é **erro léxico**.

## 2. Case-sensitivity

A linguagem é **case-sensitive**. `While` é um identificador; `while` é palavra reservada. Palavras reservadas e identificadores seguem a mesma regra.

## 3. Espaços em branco e comentários

- Espaços, tabs e quebras de linha separam tokens e são descartados.
- Comentário de linha: `//` até o fim da linha (descartado).
- Comentário de bloco: `/*` até `*/` (descartado). **Não podem ser aninhados.**
- Bloco não fechado até o EOF é **erro léxico**.

## 4. Categorias de token

| Token | Notação (regex/EBNF) | Exemplos válidos | Decisões |
| --- | --- | --- | --- |
| Identificador | `(letra \| "_") (letra \| dígito \| "_")*` | `total`, `x1`, `_aux`, `contaItens` | Case-sensitive; `_` permitido, inclusive no início; sem tamanho máximo |
| Palavra reservada | mesmo padrão do identificador + consulta em tabela | `if`, `while`, `int`, `return` | Lista fechada na seção 5 |
| String | `"` ( `[^"\\\n]` \| `\\` (`"` \| `\\` \| `n` \| `t`) )* `"` | `"ok"`, `"linha 1"`, `"diz \"oi\""` | Escapes: `\"`, `\\`, `\n`, `\t`. Não é multilinha |
| Literal char | `'` ( `[^'\\\n]` \| `\\` (`'` \| `\\` \| `n` \| `t`) ) `'` | `'a'`, `'\n'` | Exatamente um caractere (necessário para o tipo `char`) |
| Operador | 1 ou 2 caracteres (seção 6) | `=`, `==`, `<=`, `&&` | Maximal munch |
| Literal numérico | `dígito+ ( "." dígito+ )?` | `10`, `3.14`, `0` | Sem notação científica, hexadecimal ou sinal (o `-` é operador unário) |
| Delimitador | `( ) { } [ ] , ;` | `(`, `;` | Um caractere cada |

`letra` = `a-z | A-Z`; `dígito` = `0-9`.

Literais numéricos: sem ponto = `INT_LIT`; com ponto = `DOUBLE_LIT`. Um `3.` (ponto sem dígito depois) resulta no literal `3` seguido de erro léxico no `.` isolado.

## 5. Palavras reservadas (lista fechada)

| Grupo | Palavras |
| --- | --- |
| Tipos | `int`, `double`, `bool`, `char`, `string`, `void` |
| Controle | `if`, `else`, `while` |
| Funções | `return` |
| Literais booleanos | `true`, `false` |

## 6. Operadores

| Token | Lexemas |
| --- | --- |
| Aritméticos | `+` `-` `*` `/` `%` |
| Atribuição | `=` |
| Comparação | `==` `!=` `<` `<=` `>` `>=` |
| Lógicos | `&&` `\|\|` `!` |

Formas compostas: `==`, `!=`, `<=`, `>=`, `&&`, `||`. Um `&` ou `|` isolado **não** é operador válido (erro léxico).

## 7. Desambiguação: maximal munch

O scanner sempre consome o prefixo válido mais longo antes de decidir o token. Exemplos: `<=` é um token, não `<` e `=`; `==` é um token, não dois `=`; `iff` é um identificador, não `if` seguido de `f`; `/` sozinho é divisão, mas `//` e `/*` iniciam comentários.

## 8. Erros léxicos

Todo erro é reportado com linha e coluna, e o scanner segue para o próximo token.

| Situação | Comportamento |
| --- | --- |
| Caractere fora do alfabeto | Reporta, descarta o caractere, continua |
| String não fechada (fim de linha ou EOF) | Reporta na posição de abertura, continua na linha seguinte |
| Char mal formado ou não fechado | Reporta, continua |
| Comentário de bloco não fechado até EOF | Reporta na posição de abertura |
| `&` ou `\|` isolado | Reporta, continua |
| Escape inválido em string (ex.: `\q`) | Reporta, continua |
