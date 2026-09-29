# Parte 1 — Especificação Léxica da Linguagem

Este documento descreve formalmente o analisador léxico (scanner) da linguagem, definindo suas categorias de tokens, expressões regulares e regras de formação.

## 1. Categorias de Tokens

Abaixo estão definidas as cinco categorias principais de tokens da linguagem, suas notações (baseadas em expressões regulares estendidas) e as decisões de design adotadas.

| Token | Notação (Regex / EBNF) | Exemplos Válidos | Decisões de Design adotadas |
| :--- | :--- | :--- | :--- |
| **Identificador** | `[a-zA-Z_][a-zA-Z0-9_]*`<br>*(letra ou `_`, seguido de zero ou mais letras, dígitos ou `_`)* | `total`, `x1`, `_contaItens`, `valor_maximo` | **Case-sensitive?** Sim (`Total` é diferente de `total`).<br>**`_` permitido?** Sim, em qualquer posição.<br>**Tamanho máximo?** 255 caracteres. |
| **Palavra Reservada** | Mesmo padrão do identificador, mas validado via tabela de hash/busca no analisador. | `if`, `while`, `int`, `return` | As palavras-chave são sensíveis a maiúsculas/minúsculas e devem ser escritas inteiramente em minúsculas. (Lista completa na Seção 2). |
| **String** | `\" ([^\"\n\\] \| \\[nrt\"\\])* \"`<br>*(inicia com `"`, contendo qualquer caractere exceto `"`, `\n` ou `\`, ou uma sequência de escape válida, e termina com `"`)* | `"ok"`, `"linha 1"`, `"Olá\nMundo"`, `"Aspas: \""` | **Escapes:** Suporta `\n` (nova linha), `\r`, `\t` (tab), `\"` (aspas) e `\\` (barra).<br>**Fim de linha/EOF:** Se a string não for fechada antes de uma quebra de linha real (`\n`) ou do EOF, o analisador léxico emitirá um **Erro Léxico** (Unterminated String Literal). |
| **Operador** | `\+` \| `-` \| `\*` \| `/` \| `=` \| `==` \| `<` \| `<=` \| `>` \| `>=` \| `!=` \| `&&` \| `\|\|` \| `!` | `=`, `==`, `<=`, `+`, `&&` | **Quais existem?** Aritméticos, relacionais, lógicos e de atribuição.<br>**Forma composta:** `==`, `<=`, `>=`, `!=`, `&&`, `||`. |
| **Literal Numérico** | `[0-9]+ ( \. [0-9]+ )?`<br>*(um ou mais dígitos, opcionalmente seguidos por um ponto e um ou mais dígitos)* | `10`, `3.14`, `0`, `0.001` | **Tipos:** Inteiros e decimais (ponto flutuante).<br>**Bônus:** Não suporta notação científica (ex: `1e10`) nem hexadecimal (ex: `0x1A`), apenas base 10 padrão. |

---

## 2. Respostas às Regras da Linguagem

### 2.1. Qual é o alfabeto de entrada?
O alfabeto de entrada para o código-fonte da linguagem é o **conjunto de caracteres ASCII imprimíveis** (códigos 32 a 126), somados aos caracteres de controle de formatação: espaço (32), tabulação horizontal (`\t`), retorno de carro (`\r`) e quebra de linha (`\n`). 
*Nota:* Caracteres Unicode (como acentos em português) são permitidos **apenas** dentro de Literais de String e Comentários.

### 2.2. A linguagem é case-sensitive?
**Sim.** A linguagem é estritamente *case-sensitive*. 
Palavras reservadas e identificadores seguem a mesma regra: a palavra `While` será tratada como um identificador comum pelo analisador léxico, pois a palavra reservada reconhecida na linguagem é exclusivamente `while` (tudo minúsculo). Identificadores como `var`, `Var` e `VAR` representam três tokens distintos.

### 2.3. Espaços em branco e Comentários
*   **Espaços em branco:** Espaço simples (` `), tabulações (`\t`), quebras de linha (`\n`) e retornos de carro (`\r`) são considerados delimitadores em branco. O analisador léxico os ignora, exceto quando servem para separar tokens que, de outra forma, se aglutinariam (ex: `int a` vs `inta`).
*   **Comentário de Linha:** Iniciado por `//`. Tudo o que se segue após `//` até o final da linha (`\n` ou EOF) é ignorado pelo scanner.
*   **Comentário de Bloco:** Iniciado por `/*` e terminado por `*/`. Pode ocupar múltiplas linhas.
*   **Aninhamento:** Comentários de bloco **não** podem ser aninhados. A primeira ocorrência de `*/` fechará o comentário em andamento, independentemente de haver um `/*` interno (ex: `/* Comentário /* interno */ erro */`).

### 2.4. Regra de Desambiguação (Maximal Munch)
A linguagem utiliza rigorosamente a regra do **"Maximal Munch"** (consumo máximo). O scanner sempre consome a maior cadeia possível de caracteres da entrada da esquerda para a direita que corresponda a uma regra válida.
*   **Exemplo (`=` vs `==`):** Ao ler o caractere `=`, o scanner verifica o próximo caractere. Se for outro `=`, ele os agrupa formando o token composto de operador de igualdade (`==`). Ele não emitirá dois tokens de atribuição (`=`, `=`).
*   **Exemplo (`<=`):** Ao ler `<`, se o próximo caractere for `=`, o scanner emite o token `<=`.

### 2.5. Lista Exata de Palavras Reservadas
As palavras a seguir formam a lista fechada e restrita de palavras-chave da linguagem. Nenhuma delas pode ser utilizada como identificador:

1.  `int`
2.  `float`
3.  `bool`
4.  `string`
5.  `void`
6.  `if`
7.  `else`
8.  `while`
9.  `for`
10. `return`
11. `true`
12. `false`
13. `func`
14. `print`
15. `read`