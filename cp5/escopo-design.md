# Checkpoint 4 — Tabela de Símbolos, Escopo e Passada de Identificação

Este documento detalha o desenho do ambiente, o modelo de símbolos, a justificativa da implementação da tabela de símbolos e as decisões de projeto de escopo da linguagem, servindo de especificação para a análise contextual e a passada de identificação.

---

## 1. Desenho do Ambiente e Resoluções

O programa abaixo ilustra todos os requisitos exigidos: linhas numeradas, dois níveis de blocos aninhados, sombreamento de identificador, função com parâmetro e tentativa de uso de variável após o fechamento de bloco.

### 1.1 Código-fonte do programa de desenho (`test/programas/prog-desenho.txt`)

```c
1: int x = 10;
2: void f(int p) {
3:     int x = 20;
4:     {
5:         int y = p + x;
6:         escreve(y);
7:     }
8:     escreve(x);
9:     y = 30;
10: }
```

### 1.2 Rastreamento do Ambiente e Resolução de Usos

O ambiente inicial (nível 0) já contém os identificadores predefinidos da linguagem (`escreve@pre`, procedimento predefinido). Conforme a passada de identificação do grupo, sub-rotinas de nível superior sofrem uma passada prévia de cabeçalhos; assim, `f@2` entra no escopo global antes da execução do corpo.

Notação: `k{s1 s2 ...}` indica o escopo no nível de aninhamento `k`. `x@L` indica o símbolo `x` declarado na linha `L`. `x -> x@L` indica que o uso de `x` resolve para a declaração na linha `L`.

| Ponto / Linha | Ação / Evento | Ambiente Resultante (pilha de escopos) | Resolução de Usos (no formato `x@L`) |
| :--- | :--- | :--- | :--- |
| **Início** | Ambiente inicial | `0{escreve@pre}` | — |
| **Pré-passada** | Declaração de `f` | `0{escreve@pre f@2}` | — |
| **Linha 1** | Declaração `int x = 10;` | `0{escreve@pre f@2 x@1}` | — |
| **Linha 2** | Entrada em `f`: abre escopo (nível 1); declara parâmetro `p` | `0{escreve@pre f@2 x@1}`<br>`1{p@2}` | — |
| **Linha 3** | Declaração `int x = 20;` *(sombreia `x@1`)* | `0{escreve@pre f@2 x@1}`<br>`1{p@2 x@3}` | — |
| **Linha 4** | Entrada no bloco interno: abre escopo (nível 2) | `0{escreve@pre f@2 x@1}`<br>`1{p@2 x@3}`<br>`2{}` | — |
| **Linha 5** | Uso de `p` e `x` na inicialização; depois declaração `y` | `0{escreve@pre f@2 x@1}`<br>`1{p@2 x@3}`<br>`2{y@5}` | `p` resolve para `p@2`<br>`x` resolve para `x@3` |
| **Linha 6** | Chamada `escreve(y);` | `0{escreve@pre f@2 x@1}`<br>`1{p@2 x@3}`<br>`2{y@5}` | `escreve` resolve para `escreve@pre`<br>`y` resolve para `y@5` |
| **Linha 7** | Fim do bloco interno: fecha escopo (nível 2 descartado) | `0{escreve@pre f@2 x@1}`<br>`1{p@2 x@3}` | *(o símbolo `y@5` deixa de existir)* |
| **Linha 8** | Chamada `escreve(x);` | `0{escreve@pre f@2 x@1}`<br>`1{p@2 x@3}` | `escreve` resolve para `escreve@pre`<br>`x` resolve para `x@3` *(restaurado)* |
| **Linha 9** | Comando `y = 30;` | `0{escreve@pre f@2 x@1}`<br>`1{p@2 x@3}` | `y` resolve para **? (não declarado)**<br>*(reporta erro contextual)* |
| **Linha 10** | Fim de `f`: fecha escopo (nível 1 descartado) | `0{escreve@pre f@2 x@1}` | — |

---

## 2. O que o `Symbol` guarda e por quê

A classe `symbols.Symbol` representa uma entrada na tabela de símbolos com os seguintes campos:

```java
public class Symbol {
    public final String name;       // Nome do identificador
    public final Categoria kind;     // VARIAVEL, PARAMETRO, FUNCAO, PROCEDIMENTO
    public final Node node;          // Nó da AST onde foi declarado (null se predefinido)
    public final int level;          // Nível de aninhamento (0 = global)

    // Campos reservados para fases posteriores (Aulas 12 a 19):
    public Tipo type;                // Tipo do dado ou tipo de retorno (CP5)
    public int offset = -1;          // Deslocamento no registro de ativação (Geração de Código)
}
```

### Justificativa de cada campo e fases consumidoras:

1. **`name` (`String`)**:
   - *Por quê:* Identifica textualmente o símbolo.
   - *Fase:* Passada de identificação (busca e comparação na tabela) e mensagens de erro contextual.
2. **`kind` (`Categoria`)**:
   - *Por quê:* Distingue o papel do identificador: se é uma variável, parâmetro formal, função (com retorno) ou procedimento (`void`).
   - *Fase:* Passada de tipos (CP5) — verifica se um identificador em expressão de chamada é de fato uma função; se o alvo de atribuição é variável/parâmetro; e geração de código — parâmetros e variáveis locais residem em posições distintas do registro de ativação.
3. **`node` (`Node`)**:
   - *Por quê:* Aponta diretamente para o nó original da declaração (`VarDecl`, `Param`, `FuncDecl` ou `ProcDecl`).
   - *Fase:* Permite recuperar a linha e coluna exatas da declaração original para mensagens explicativas de erro (ex.: `'a' já declarado neste escopo (linha 1)`). No CP5, permite inspecionar a lista de parâmetros formais para checagem de aridade e tipos de argumentos.
4. **`level` (`int`)**:
   - *Por quê:* Guarda a profundidade léxica onde o símbolo reside (0 = global, 1 = sub-rotina, 2+ = blocos aninhados).
   - *Fase:* Passada de identificação (garante rastreabilidade de escopo) e geração de código (distinção entre variáveis globais em segmento de dados e variáveis locais na pilha).
5. **`type` (`Tipo`) e `offset` (`int`) [Reservados]**:
   - *Por quê:* Já incluídos na classe sem valor inicial obrigatório (`type = null`, `offset = -1`). O CP5 (checagem de tipos) preenche `type` sem alterar as assinaturas das classes, e a geração de código preenche `offset` no frame de pilha.

---

## 3. Escolha e Justificativa da Implementação da Tabela

A implementação escolhida pelo grupo foi a **Pilha de Mapas** (`symbols.PilhaDeEscopos`).

### Justificativa:
1. **Mapeamento direto ao modelo mental**: Cada bloco aberto corresponde naturalmente a um mapa independente empilhado, e o encerramento do bloco descarta todo o escopo local em $O(1)$ sem risco de efeitos colaterais nos escopos externos.
2. **Simplicidade e corretude**: Não requer ponteiros reversos nem listas de restauração na tabela de dispersão (como na tabela única com registro), tornando a depuração e o rastreamento em testes transparentes e determinísticos.
3. **Desempenho adequado à profundidade de linguagens imperativas**: Como a profundidade de aninhamento de blocos em programas típicos é pequena ($< 10$), a busca linear do topo até o escopo global (`lookup`) percorre pouquíssimos mapas, operando em tempo praticamente constante na prática.

### Tratamento de `closeScope` sem escopo aberto:
O escopo global (nível 0) é instanciado na construção da tabela e **nunca** deve ser fechado. Se `closeScope()` for invocado quando restar apenas o escopo global, o método lança `IllegalStateException` com mensagem explicativa:
`"closeScope sem escopo aberto: o escopo global (nível 0) não pode ser fechado"`.

---

## 4. As Cinco Decisões de Projeto de Escopo

### Decisão 1: Sombreamento (Shadowing)
- **Escolha do grupo:** Um identificador declarado em um escopo interno **pode sombrear** um identificador de mesmo nome declarado em um escopo mais externo. A redeclaração no mesmo bloco é **sempre erro**.
- **Alternativa descartada:** Proibir sombreamento em qualquer nível (como em linguagens como C# para variáveis locais em blocos aninhados).
- **Motivo:** O sombreamento é padrão e ergonômico em linguagens baseadas em C, permitindo que blocos internos utilizem identificadores comuns (como `i`, `x`, `temp`) sem receio de colisão acidental com variáveis globais ou de funções externas. Dentro do mesmo escopo, proibir redeclaração evita ambiguidades e erros de programação.
- **Programa de teste:** `test/programas/decisao1-sombreamento.txt` (válido com sombreamento) e `test/programas/prog2-erro-redeclaracao.txt` (erro de redeclaração no mesmo escopo).

### Decisão 2: Declarar Antes de Usar
- **Escolha do grupo:** Para **variáveis**, o uso deve textualmente suceder a declaração. A inicialização de uma variável (`int x = init;`) é avaliada **antes** de `x` entrar na tabela de símbolos; logo, `int x = x;` busca `x` no escopo externo (ou gera erro se não houver). Para **funções e procedimentos**, aplica-se uma passada prévia de cabeçalhos no nível global, permitindo que sejam invocadas antes do texto de sua definição.
- **Alternativa descartada:** Exigir declaração estrita antes do uso também para funções (exigindo protótipos como em C) ou içar (*hoisting*) variáveis locais para o topo do bloco (como em JavaScript `var`).
- **Motivo:** Exigir declaração prévia para variáveis impede leitura de valores indefinidos e ciclos espúrios. Permitir chamada prévia de funções viabiliza chamadas naturais sem a complicação sintática de protótipos redundantes.
- **Programa de teste:** `test/programas/decisao2-decl-antes-uso.txt` (erro ao usar variável antes da declaração e em auto-inicialização).

### Decisão 3: Funções Mutuamente Recursivas
- **Escolha do grupo:** Funções e procedimentos **podem ser mutuamente recursivos**.
- **Alternativa descartada:** Proibir recursão mútua ou exigir declaração antecipada/protótipo explícito.
- **Motivo:** Como a gramática da linguagem não inclui cabeçalhos/protótipos sem corpo (`int f();`), a única maneira de suportar recursão mútua é coletar todos os cabeçalhos de funções de nível superior em uma passada prévia (`PassadaPreviaSubrotinas`), inserindo-os no escopo global antes de percorrer os corpos das sub-rotinas.
- **Programa de teste:** `test/programas/decisao3-recursao-mutua.txt` (implementação de `par` e `impar` chamando uma à outra).

### Decisão 4: Parâmetros e Corpo da Sub-rotina
- **Escolha do grupo:** Parâmetros formais e o corpo mais externo da função **compartilham o mesmo escopo** (nível 1).
- **Alternativa descartada:** O corpo da função abrir um novo nível de escopo interno, permitindo que uma variável local no início do corpo sombreie um parâmetro formal de mesmo nome (`void f(int a) { int a = 1; }`).
- **Motivo:** Um parâmetro já é uma variável local inicializada pelo chamador. Permitir redeclarar uma variável com o mesmo nome no mesmo nível do corpo é fonte frequente de confusão e erros lógicos. No entanto, blocos aninhados adicionais dentro da função (`{ int a = 2; }`) abrem nível 2 e podem sombrear o parâmetro normalmente.
- **Programa de teste:** `test/programas/decisao4-param-corpo.txt` (gera erro de redeclaração ao tentar declarar variável de mesmo nome de parâmetro no corpo principal).

### Decisão 5: Espaços de Nomes (Namespaces)
- **Escolha do grupo:** Variáveis, parâmetros, funções e procedimentos **compartilham um único espaço de nomes**, exatamente como na linguagem C.
- **Alternativa descartada:** Espaços de nomes separados para variáveis e funções (como em Common Lisp ou Java, onde campos e métodos têm tabelas distintas).
- **Motivo:** Em linguagens simples sem classes nem ponteiros para funções, ter uma única tabela de símbolos simplifica a arquitetura contextual e evita que uma variável mascare uma função de mesmo nome sem detecção imediata de conflito no mesmo escopo.
- **Programa de teste:** `test/programas/decisao5-espaco-nomes.txt` (gera erro contextual de redeclaração ao declarar uma variável global e uma função com o mesmo nome `f`).

---

## 5. Como Compilar e Executar a Suíte de Testes

A partir da pasta `cp4`:

```bash
# Compilar todo o fonte, AST, visitors, símbolos e testes
javac -encoding UTF-8 -d out src/*.java src/ast/*.java src/visitor/*.java src/symbols/*.java test/*.java

# 1. Testes unitários da SymbolTable (isolada)
java -cp out SymbolTableTest

# 2. Testes da passada de identificação e decisões de escopo
java -cp out IdentificationTest

# 3. Testes de regressão do CP3 (construção da AST e precedência)
java -cp out AstTest

# 4. Testes de regressão do CP2 (análise sintática e recuperação)
java -cp out ParserTest

# 5. Execução manual de programa de exemplo
java -cp out ParserMain test/programas/prog1-valido-sombreamento.txt --resolucao
```

