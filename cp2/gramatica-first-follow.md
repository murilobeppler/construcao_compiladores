# Checkpoint 2 — Gramática final, FIRST/FOLLOW e verificação LL(1)

## 0. Convenções

- Os **terminais** são os tokens do scanner do CP1, sem modificação: `ID` = `IDENTIFIER`; `INT_LIT`, `DOUBLE_LIT`, `STRING_LIT`, `CHAR_LIT`; palavras reservadas, operadores e delimitadores são escritos pelo próprio lexema (entre aspas simples nas produções); `EOF` = fim do arquivo.
- `ε` é a produção vazia; `$` é o marcador de fim de entrada usado só no FOLLOW do símbolo inicial.
- `[` e `]` existem no léxico do CP1, mas esta linguagem não tem vetores; por isso não aparecem na gramática (nenhum token do CP1 foi alterado ou removido).

## 1. Gramática final

```
Programa → DeclsTop EOF
DeclsTop → DeclTop DeclsTop
         | ε
DeclTop → 'void' ID FuncResto
        | Tipo ID DeclTopResto
DeclTopResto → FuncResto
             | VarResto
FuncResto → '(' Params ')' Bloco
VarResto → InitOpt ';'
InitOpt → '=' Expr
        | ε
Tipo → 'int'
     | 'double'
     | 'bool'
     | 'char'
     | 'string'
Params → ListaParams
       | ε
ListaParams → Param ListaParamsRest
ListaParamsRest → ',' Param ListaParamsRest
                | ε
Param → Tipo ID
Bloco → '{' ListaStmt '}'
ListaStmt → Stmt ListaStmt
          | ε
Stmt → Tipo ID InitOpt ';'
     | ID StmtId
     | 'if' '(' Expr ')' Bloco ElseOpt
     | 'while' '(' Expr ')' Bloco
     | 'return' ReturnOpt ';'
     | Bloco
StmtId → '=' Expr ';'
       | '(' Args ')' ';'
ElseOpt → 'else' ElseTail
        | ε
ElseTail → Bloco
         | 'if' '(' Expr ')' Bloco ElseOpt
ReturnOpt → Expr
          | ε
Args → ListaArgs
     | ε
ListaArgs → Expr ListaArgsRest
ListaArgsRest → ',' Expr ListaArgsRest
              | ε
Expr → OrExpr
OrExpr → AndExpr OrRest
OrRest → '||' AndExpr OrRest
       | ε
AndExpr → EqExpr AndRest
AndRest → '&&' EqExpr AndRest
        | ε
EqExpr → RelExpr EqRest
EqRest → '==' RelExpr EqRest
       | '!=' RelExpr EqRest
       | ε
RelExpr → AddExpr RelRest
RelRest → '<' AddExpr RelRest
        | '<=' AddExpr RelRest
        | '>' AddExpr RelRest
        | '>=' AddExpr RelRest
        | ε
AddExpr → MulExpr AddRest
AddRest → '+' MulExpr AddRest
        | '-' MulExpr AddRest
        | ε
MulExpr → Unary MulRest
MulRest → '*' Unary MulRest
        | '/' Unary MulRest
        | '%' Unary MulRest
        | ε
Unary → '!' Unary
      | '-' Unary
      | Primary
Primary → INT_LIT
        | DOUBLE_LIT
        | STRING_LIT
        | CHAR_LIT
        | 'true'
        | 'false'
        | '(' Expr ')'
        | ID PrimaryIdRest
PrimaryIdRest → '(' Args ')'
              | ε
```

**Decisões de projeto (sintaxe concreta escolhida pelo grupo):**

1. O programa é uma sequência de declarações de nível superior: **variáveis globais** (`int x = 1;`) e **funções/procedimentos** (`int f(int a) { ... }`, `void p() { ... }`). Comandos só existem dentro de blocos.
2. `if`, `else` e `while` exigem **bloco entre chaves** (`{ ... }`). Isso elimina o *else* pendente (ver seção 2). `else if` é aceito por `ElseTail`.
3. Atribuição (`x = e;`) e chamada (`f(a, b);`) são **comandos**, não expressões. Chamada de função também pode ser usada dentro de expressões (`x = f(1) + 2;`).
4. `return;` (procedimento) e `return expr;` (função) são aceitos; a gramática não checa tipos (isso é semântica).
5. Precedência, da menor para a maior: `||`, `&&`, `== !=`, `< <= > >=`, `+ -`, `* / %`, unários `! -`. Todos os binários são associativos à esquerda (na árvore, não na gramática).

## 2. Problemas da primeira versão e correções (antes → depois)

A primeira versão da gramática do grupo tinha recursão à esquerda, prefixos comuns entre alternativas e o problema clássico do *else* pendente. A versão "antes" completa foi submetida ao mesmo verificador (seção 4); os não-terminais que violam a condição LL(1) são:

| Não-terminal | Problema | Evidência (calculada) | Correção aplicada |
| --- | --- | --- | --- |
| `DeclTop` | prefixo comum `Tipo ID` em duas alternativas (função × variável) | (a) FIRST das alternativas 2, 3 têm interseção {`bool`, `char`, `double`, `int`, `string`} | fatoração à esquerda: `Tipo ID DeclTopResto`, com `FuncResto` e `VarResto` |
| `ListaParams` | recursão à esquerda direta | (a) FIRST das alternativas 1, 2 têm interseção {`bool`, `char`, `double`, `int`, `string`} | reescrita com `ListaParamsRest` (cauda recursiva à direita) |
| `Stmt` | prefixo comum `ID` (atribuição × chamada de procedimento) | (a) FIRST das alternativas 2, 3 têm interseção {`ID`} | fatoração: `ID StmtId` |
| `ElseOpt` | *else* pendente: `else` pertence ao FIRST de `else Stmt` e também ao FOLLOW de `ElseOpt` | (c) FIRST(alt 1) ∩ FOLLOW(ElseOpt) = {`else`} | corpo de `if`/`while`/`else` passa a ser sempre um `Bloco` entre chaves |
| `ListaArgs` | recursão à esquerda direta | (a) FIRST das alternativas 1, 2 têm interseção {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | reescrita com `ListaArgsRest` |
| `Expr` | recursão à esquerda direta | (a) FIRST das alternativas 1, 2 têm interseção {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | níveis de precedência com `OrRest` etc. |
| `AndExpr` | recursão à esquerda direta | (a) FIRST das alternativas 1, 2 têm interseção {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | reescrita com `AndRest` |
| `EqExpr` | recursão à esquerda direta | (a) FIRST das alternativas 1, 2, 3 têm interseção {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | reescrita com `EqRest` |
| `RelExpr` | recursão à esquerda direta | (a) FIRST das alternativas 1, 2, 3, 4, 5 têm interseção {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | reescrita com `RelRest` |
| `AddExpr` | recursão à esquerda direta | (a) FIRST das alternativas 1, 2, 3 têm interseção {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | reescrita com `AddRest` |
| `MulExpr` | recursão à esquerda direta | (a) FIRST das alternativas 1, 2, 3, 4 têm interseção {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | reescrita com `MulRest` |
| `Primary` | prefixo comum `ID` (variável × chamada de função) | (a) FIRST das alternativas 8, 9 têm interseção {`ID`} | fatoração: `ID PrimaryIdRest` |

Gramática "antes" (para consulta):

```
Programa → DeclsTop EOF
DeclsTop → DeclTop DeclsTop
         | ε
DeclTop → 'void' ID '(' Params ')' Bloco
        | Tipo ID '(' Params ')' Bloco
        | Tipo ID InitOpt ';'
InitOpt → '=' Expr
        | ε
Tipo → 'int'
     | 'double'
     | 'bool'
     | 'char'
     | 'string'
Params → ListaParams
       | ε
ListaParams → Param
            | ListaParams ',' Param
Param → Tipo ID
Bloco → '{' ListaStmt '}'
ListaStmt → Stmt ListaStmt
          | ε
Stmt → Tipo ID InitOpt ';'
     | ID '=' Expr ';'
     | ID '(' Args ')' ';'
     | 'if' '(' Expr ')' Stmt ElseOpt
     | 'while' '(' Expr ')' Stmt
     | 'return' ReturnOpt ';'
     | Bloco
ElseOpt → 'else' Stmt
        | ε
ReturnOpt → Expr
          | ε
Args → ListaArgs
     | ε
ListaArgs → Expr
          | ListaArgs ',' Expr
Expr → Expr '||' AndExpr
     | AndExpr
AndExpr → AndExpr '&&' EqExpr
        | EqExpr
EqExpr → EqExpr '==' RelExpr
       | EqExpr '!=' RelExpr
       | RelExpr
RelExpr → RelExpr '<' AddExpr
        | RelExpr '<=' AddExpr
        | RelExpr '>' AddExpr
        | RelExpr '>=' AddExpr
        | AddExpr
AddExpr → AddExpr '+' MulExpr
        | AddExpr '-' MulExpr
        | MulExpr
MulExpr → MulExpr '*' Unary
        | MulExpr '/' Unary
        | MulExpr '%' Unary
        | Unary
Unary → '!' Unary
      | '-' Unary
      | Primary
Primary → INT_LIT
        | DOUBLE_LIT
        | STRING_LIT
        | CHAR_LIT
        | 'true'
        | 'false'
        | '(' Expr ')'
        | ID
        | ID '(' Args ')'
```

Exemplo da correção do *else* pendente: com `ElseOpt → else Stmt | ε` e `Stmt → if ( Expr ) Stmt ElseOpt`, o terminal `else` fica em FOLLOW(Stmt) (vem de `ElseOpt` logo após um `Stmt`) e, portanto, em FOLLOW(ElseOpt); a alternativa `else Stmt` também começa com `else` — a cláusula (c) falha. Na versão final o corpo é sempre `Bloco`, então `else` nunca segue um `Stmt` e FOLLOW(ElseOpt) = FIRST(Stmt) ∪ {`}`}, que não contém `else`.

## 3. Tabela de FIRST e FOLLOW

| Não-terminal | Anulável | FIRST | FOLLOW |
| --- | --- | --- | --- |
| `Programa` | não | {`EOF`, `bool`, `char`, `double`, `int`, `string`, `void`} | {`$`} |
| `DeclsTop` | sim | {`bool`, `char`, `double`, `int`, `string`, `void`, `ε`} | {`EOF`} |
| `DeclTop` | não | {`bool`, `char`, `double`, `int`, `string`, `void`} | {`EOF`, `bool`, `char`, `double`, `int`, `string`, `void`} |
| `DeclTopResto` | não | {`(`, `;`, `=`} | {`EOF`, `bool`, `char`, `double`, `int`, `string`, `void`} |
| `FuncResto` | não | {`(`} | {`EOF`, `bool`, `char`, `double`, `int`, `string`, `void`} |
| `VarResto` | não | {`;`, `=`} | {`EOF`, `bool`, `char`, `double`, `int`, `string`, `void`} |
| `InitOpt` | sim | {`=`, `ε`} | {`;`} |
| `Tipo` | não | {`bool`, `char`, `double`, `int`, `string`} | {`ID`} |
| `Params` | sim | {`bool`, `char`, `double`, `int`, `string`, `ε`} | {`)`} |
| `ListaParams` | não | {`bool`, `char`, `double`, `int`, `string`} | {`)`} |
| `ListaParamsRest` | sim | {`,`, `ε`} | {`)`} |
| `Param` | não | {`bool`, `char`, `double`, `int`, `string`} | {`)`, `,`} |
| `Bloco` | não | {`{`} | {`EOF`, `ID`, `bool`, `char`, `double`, `else`, `if`, `int`, `return`, `string`, `void`, `while`, `{`, `}`} |
| `ListaStmt` | sim | {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `ε`} | {`}`} |
| `Stmt` | não | {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`} | {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `}`} |
| `StmtId` | não | {`(`, `=`} | {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `}`} |
| `ElseOpt` | sim | {`else`, `ε`} | {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `}`} |
| `ElseTail` | não | {`if`, `{`} | {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `}`} |
| `ReturnOpt` | sim | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`, `ε`} | {`;`} |
| `Args` | sim | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`, `ε`} | {`)`} |
| `ListaArgs` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`)`} |
| `ListaArgsRest` | sim | {`,`, `ε`} | {`)`} |
| `Expr` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`)`, `,`, `;`} |
| `OrExpr` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`)`, `,`, `;`} |
| `OrRest` | sim | {`\|\|`, `ε`} | {`)`, `,`, `;`} |
| `AndExpr` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`)`, `,`, `;`, `\|\|`} |
| `AndRest` | sim | {`&&`, `ε`} | {`)`, `,`, `;`, `\|\|`} |
| `EqExpr` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`&&`, `)`, `,`, `;`, `\|\|`} |
| `EqRest` | sim | {`!=`, `==`, `ε`} | {`&&`, `)`, `,`, `;`, `\|\|`} |
| `RelExpr` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`!=`, `&&`, `)`, `,`, `;`, `==`, `\|\|`} |
| `RelRest` | sim | {`<`, `<=`, `>`, `>=`, `ε`} | {`!=`, `&&`, `)`, `,`, `;`, `==`, `\|\|`} |
| `AddExpr` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`!=`, `&&`, `)`, `,`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`} |
| `AddRest` | sim | {`+`, `-`, `ε`} | {`!=`, `&&`, `)`, `,`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`} |
| `MulExpr` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`!=`, `&&`, `)`, `+`, `,`, `-`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`} |
| `MulRest` | sim | {`%`, `*`, `/`, `ε`} | {`!=`, `&&`, `)`, `+`, `,`, `-`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`} |
| `Unary` | não | {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`!=`, `%`, `&&`, `)`, `*`, `+`, `,`, `-`, `/`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`} |
| `Primary` | não | {`(`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`} | {`!=`, `%`, `&&`, `)`, `*`, `+`, `,`, `-`, `/`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`} |
| `PrimaryIdRest` | sim | {`(`, `ε`} | {`!=`, `%`, `&&`, `)`, `*`, `+`, `,`, `-`, `/`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`} |

## 4. Verificação da condição LL(1), não-terminal por não-terminal

Condições, para cada não-terminal `A → α₁ | α₂ | …`: **(a)** os FIRST das alternativas são dois a dois disjuntos; **(b)** no máximo uma alternativa é anulável; **(c)** se `αᵢ` é anulável, FIRST(αⱼ) ∩ FOLLOW(A) = ∅ para toda outra alternativa `αⱼ`. "n/a" = a condição é vacuamente verdadeira (uma única alternativa, ou nenhuma alternativa anulável).

| Não-terminal | Alternativas | (a) FIRST disjuntos | (b) ≤ 1 anulável | (c) FIRST(outras) ∩ FOLLOW = ∅ | LL(1) |
| --- | --- | --- | --- | --- | --- |
| `Programa` | 1 | n/a | n/a | n/a | ✓ |
| `DeclsTop` | 2 | ✓ | ✓ | ✓ | ✓ |
| `DeclTop` | 2 | ✓ | n/a | n/a | ✓ |
| `DeclTopResto` | 2 | ✓ | n/a | n/a | ✓ |
| `FuncResto` | 1 | n/a | n/a | n/a | ✓ |
| `VarResto` | 1 | n/a | n/a | n/a | ✓ |
| `InitOpt` | 2 | ✓ | ✓ | ✓ | ✓ |
| `Tipo` | 5 | ✓ | n/a | n/a | ✓ |
| `Params` | 2 | ✓ | ✓ | ✓ | ✓ |
| `ListaParams` | 1 | n/a | n/a | n/a | ✓ |
| `ListaParamsRest` | 2 | ✓ | ✓ | ✓ | ✓ |
| `Param` | 1 | n/a | n/a | n/a | ✓ |
| `Bloco` | 1 | n/a | n/a | n/a | ✓ |
| `ListaStmt` | 2 | ✓ | ✓ | ✓ | ✓ |
| `Stmt` | 6 | ✓ | n/a | n/a | ✓ |
| `StmtId` | 2 | ✓ | n/a | n/a | ✓ |
| `ElseOpt` | 2 | ✓ | ✓ | ✓ | ✓ |
| `ElseTail` | 2 | ✓ | n/a | n/a | ✓ |
| `ReturnOpt` | 2 | ✓ | ✓ | ✓ | ✓ |
| `Args` | 2 | ✓ | ✓ | ✓ | ✓ |
| `ListaArgs` | 1 | n/a | n/a | n/a | ✓ |
| `ListaArgsRest` | 2 | ✓ | ✓ | ✓ | ✓ |
| `Expr` | 1 | n/a | n/a | n/a | ✓ |
| `OrExpr` | 1 | n/a | n/a | n/a | ✓ |
| `OrRest` | 2 | ✓ | ✓ | ✓ | ✓ |
| `AndExpr` | 1 | n/a | n/a | n/a | ✓ |
| `AndRest` | 2 | ✓ | ✓ | ✓ | ✓ |
| `EqExpr` | 1 | n/a | n/a | n/a | ✓ |
| `EqRest` | 3 | ✓ | ✓ | ✓ | ✓ |
| `RelExpr` | 1 | n/a | n/a | n/a | ✓ |
| `RelRest` | 5 | ✓ | ✓ | ✓ | ✓ |
| `AddExpr` | 1 | n/a | n/a | n/a | ✓ |
| `AddRest` | 3 | ✓ | ✓ | ✓ | ✓ |
| `MulExpr` | 1 | n/a | n/a | n/a | ✓ |
| `MulRest` | 4 | ✓ | ✓ | ✓ | ✓ |
| `Unary` | 3 | ✓ | n/a | n/a | ✓ |
| `Primary` | 8 | ✓ | n/a | n/a | ✓ |
| `PrimaryIdRest` | 2 | ✓ | ✓ | ✓ | ✓ |

**Detalhe dos casos não triviais** (mais de uma alternativa ou alternativa anulável):

- **DeclsTop** — FOLLOW(DeclsTop) = {`EOF`}
  - α1 = `DeclTop DeclsTop`: FIRST = {`bool`, `char`, `double`, `int`, `string`, `void`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(DeclsTop) = ∅ ✓
- **DeclTop** — FOLLOW(DeclTop) = {`EOF`, `bool`, `char`, `double`, `int`, `string`, `void`}
  - α1 = `'void' ID FuncResto`: FIRST = {`void`}
  - α2 = `Tipo ID DeclTopResto`: FIRST = {`bool`, `char`, `double`, `int`, `string`}
- **DeclTopResto** — FOLLOW(DeclTopResto) = {`EOF`, `bool`, `char`, `double`, `int`, `string`, `void`}
  - α1 = `FuncResto`: FIRST = {`(`}
  - α2 = `VarResto`: FIRST = {`;`, `=`}
- **InitOpt** — FOLLOW(InitOpt) = {`;`}
  - α1 = `'=' Expr`: FIRST = {`=`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(InitOpt) = ∅ ✓
- **Tipo** — FOLLOW(Tipo) = {`ID`}
  - α1 = `'int'`: FIRST = {`int`}
  - α2 = `'double'`: FIRST = {`double`}
  - α3 = `'bool'`: FIRST = {`bool`}
  - α4 = `'char'`: FIRST = {`char`}
  - α5 = `'string'`: FIRST = {`string`}
- **Params** — FOLLOW(Params) = {`)`}
  - α1 = `ListaParams`: FIRST = {`bool`, `char`, `double`, `int`, `string`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(Params) = ∅ ✓
- **ListaParamsRest** — FOLLOW(ListaParamsRest) = {`)`}
  - α1 = `',' Param ListaParamsRest`: FIRST = {`,`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(ListaParamsRest) = ∅ ✓
- **ListaStmt** — FOLLOW(ListaStmt) = {`}`}
  - α1 = `Stmt ListaStmt`: FIRST = {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(ListaStmt) = ∅ ✓
- **Stmt** — FOLLOW(Stmt) = {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `}`}
  - α1 = `Tipo ID InitOpt ';'`: FIRST = {`bool`, `char`, `double`, `int`, `string`}
  - α2 = `ID StmtId`: FIRST = {`ID`}
  - α3 = `'if' '(' Expr ')' Bloco ElseOpt`: FIRST = {`if`}
  - α4 = `'while' '(' Expr ')' Bloco`: FIRST = {`while`}
  - α5 = `'return' ReturnOpt ';'`: FIRST = {`return`}
  - α6 = `Bloco`: FIRST = {`{`}
- **StmtId** — FOLLOW(StmtId) = {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `}`}
  - α1 = `'=' Expr ';'`: FIRST = {`=`}
  - α2 = `'(' Args ')' ';'`: FIRST = {`(`}
- **ElseOpt** — FOLLOW(ElseOpt) = {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `}`}
  - α1 = `'else' ElseTail`: FIRST = {`else`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(ElseOpt) = ∅ ✓
- **ElseTail** — FOLLOW(ElseTail) = {`ID`, `bool`, `char`, `double`, `if`, `int`, `return`, `string`, `while`, `{`, `}`}
  - α1 = `Bloco`: FIRST = {`{`}
  - α2 = `'if' '(' Expr ')' Bloco ElseOpt`: FIRST = {`if`}
- **ReturnOpt** — FOLLOW(ReturnOpt) = {`;`}
  - α1 = `Expr`: FIRST = {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(ReturnOpt) = ∅ ✓
- **Args** — FOLLOW(Args) = {`)`}
  - α1 = `ListaArgs`: FIRST = {`!`, `(`, `-`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(Args) = ∅ ✓
- **ListaArgsRest** — FOLLOW(ListaArgsRest) = {`)`}
  - α1 = `',' Expr ListaArgsRest`: FIRST = {`,`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(ListaArgsRest) = ∅ ✓
- **OrRest** — FOLLOW(OrRest) = {`)`, `,`, `;`}
  - α1 = `'\|\|' AndExpr OrRest`: FIRST = {`\|\|`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(OrRest) = ∅ ✓
- **AndRest** — FOLLOW(AndRest) = {`)`, `,`, `;`, `\|\|`}
  - α1 = `'&&' EqExpr AndRest`: FIRST = {`&&`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(AndRest) = ∅ ✓
- **EqRest** — FOLLOW(EqRest) = {`&&`, `)`, `,`, `;`, `\|\|`}
  - α1 = `'==' RelExpr EqRest`: FIRST = {`==`}
  - α2 = `'!=' RelExpr EqRest`: FIRST = {`!=`}
  - α3 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(EqRest) = ∅ ✓
- **RelRest** — FOLLOW(RelRest) = {`!=`, `&&`, `)`, `,`, `;`, `==`, `\|\|`}
  - α1 = `'<' AddExpr RelRest`: FIRST = {`<`}
  - α2 = `'<=' AddExpr RelRest`: FIRST = {`<=`}
  - α3 = `'>' AddExpr RelRest`: FIRST = {`>`}
  - α4 = `'>=' AddExpr RelRest`: FIRST = {`>=`}
  - α5 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(RelRest) = ∅ ✓
- **AddRest** — FOLLOW(AddRest) = {`!=`, `&&`, `)`, `,`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`}
  - α1 = `'+' MulExpr AddRest`: FIRST = {`+`}
  - α2 = `'-' MulExpr AddRest`: FIRST = {`-`}
  - α3 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(AddRest) = ∅ ✓
- **MulRest** — FOLLOW(MulRest) = {`!=`, `&&`, `)`, `+`, `,`, `-`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`}
  - α1 = `'*' Unary MulRest`: FIRST = {`*`}
  - α2 = `'/' Unary MulRest`: FIRST = {`/`}
  - α3 = `'%' Unary MulRest`: FIRST = {`%`}
  - α4 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(MulRest) = ∅ ✓
- **Unary** — FOLLOW(Unary) = {`!=`, `%`, `&&`, `)`, `*`, `+`, `,`, `-`, `/`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`}
  - α1 = `'!' Unary`: FIRST = {`!`}
  - α2 = `'-' Unary`: FIRST = {`-`}
  - α3 = `Primary`: FIRST = {`(`, `CHAR_LIT`, `DOUBLE_LIT`, `ID`, `INT_LIT`, `STRING_LIT`, `false`, `true`}
- **Primary** — FOLLOW(Primary) = {`!=`, `%`, `&&`, `)`, `*`, `+`, `,`, `-`, `/`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`}
  - α1 = `INT_LIT`: FIRST = {`INT_LIT`}
  - α2 = `DOUBLE_LIT`: FIRST = {`DOUBLE_LIT`}
  - α3 = `STRING_LIT`: FIRST = {`STRING_LIT`}
  - α4 = `CHAR_LIT`: FIRST = {`CHAR_LIT`}
  - α5 = `'true'`: FIRST = {`true`}
  - α6 = `'false'`: FIRST = {`false`}
  - α7 = `'(' Expr ')'`: FIRST = {`(`}
  - α8 = `ID PrimaryIdRest`: FIRST = {`ID`}
- **PrimaryIdRest** — FOLLOW(PrimaryIdRest) = {`!=`, `%`, `&&`, `)`, `*`, `+`, `,`, `-`, `/`, `;`, `<`, `<=`, `==`, `>`, `>=`, `\|\|`}
  - α1 = `'(' Args ')'`: FIRST = {`(`}
  - α2 = `ε` (anulável): FIRST = ∅
  - (c) união dos FIRST das não anuláveis ∩ FOLLOW(PrimaryIdRest) = ∅ ✓

Resultado: **todos os 38 não-terminais satisfazem (a), (b) e (c); a gramática é LL(1)**.

## 5. Como o parser usa estas tabelas

- Cada não-terminal é um método `parseX()`; a alternativa é escolhida olhando o *lookahead* contra os FIRST (seção 3).
- Quando existe alternativa `ε`, ela só é aceita se o lookahead está em FOLLOW do não-terminal (versão robusta); caso contrário o parser reporta erro.
- Conjunto de sincronização do modo pânico: FOLLOW(A) ∪ âncoras `;`, `}` e `)`.
