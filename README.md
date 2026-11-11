# Projeto do Compilador — Checkpoints 1 a 4

Este repositório contém o desenvolvimento modular do compilador para a disciplina de Construção de Compiladores. Cada etapa (checkpoint) é **autocontida**, permitindo a compilação, execução e testes isolados, mantendo histórico de regressão entre as fases.

---

## Estrutura do Repositório

```
construcao_compiladores/
├── README.md                      # Visão geral e instruções do projeto
├── .gitignore                     # Ignora binários compilados (out/, *.class, .idea)
│
├── cp1/                           # Checkpoint 1: Analisador Léxico (Scanner)
│   ├── automatos/                 # Diagramas dos AFDs (identificadores, strings)
│   ├── exemplos/                  # Exemplos de código para teste manual
│   ├── src/                       # Scanner, Token, TokenType, Estado, LexicalError, Main
│   ├── test/                      # ScannerTest (29 testes unitários)
│   ├── especificacao-lexica.md    # Especificação formal da linguagem léxica
│   ├── evidencia-testes.txt       # Evidência de execução da suíte de testes
│   └── log-uso-ia.md              # Log de transparência no uso de IA
│
├── cp2/                           # Checkpoint 2: Analisador Sintático LL(1) (Parser)
│   ├── exemplos/                  # Programas válidos e com erros sintáticos
│   ├── src/                       # Parser recursivo descendente com recuperação de erros
│   ├── test/                      # ParserTest (18 testes: válidos, inválidos, fuzzing)
│   ├── gramatica-first-follow.md  # Gramática livre de contexto, FIRST, FOLLOW e LL(1)
│   ├── evidencia-testes.txt       # Evidência de execução da suíte de testes
│   └── log-uso-ia.md              # Log de transparência no uso de IA
│
├── cp3/                           # Checkpoint 3: Árvore Sintática Abstrata (AST) e Visitors
│   ├── exemplos/                  # Imagem e modelo da árvore desenhada à mão
│   ├── src/
│   │   ├── ast/                   # Hierarquia de 20 nós da AST (Program, Stmt, Expr, etc.)
│   │   └── visitor/               # Interface Visitor<T>, TreePrinter, SourcePrinter
│   ├── test/
│   │   ├── AstTest.java           # Testes de AST: precedência, associatividade, ida e volta
│   │   ├── ParserTest.java        # Regressão sintática do CP2
│   │   ├── programas/             # Programas de entrada para testes da AST
│   │   └── esperado/              # Saídas textuais esperadas da árvore
│   ├── ast-design.md              # Decisões de projeto da AST e mapeamento sintático
│   └── log-uso-ia.md              # Log de transparência no uso de IA
│
└── cp4/                           # Checkpoint 4: Tabela de Símbolos e Passada de Identificação
    ├── exemplos/                  # Exemplos válidos de programas completos
    ├── src/
    │   ├── ast/                   # AST do CP3 com campo 'decl' em nós de uso (Id, Assign, Call)
    │   ├── symbols/               # SymbolTable, PilhaDeEscopos, Symbol, Categoria, ContextualError
    │   └── visitor/               # IdentificationVisitor, ResolutionPrinter, VisitorVazio
    ├── test/
    │   ├── SymbolTableTest.java   # Testes unitários isolados da PilhaDeEscopos (24 testes)
    │   ├── IdentificationTest.java# Testes da passada de identificação e decisões de escopo (14 testes)
    │   ├── AstTest.java           # Regressão do CP3 (19 testes)
    │   ├── ParserTest.java        # Regressão do CP2 (19 testes)
    │   ├── programas/             # Programas cobrindo as 5 decisões de escopo e casos de erro
    │   ├── esperado/              # Saídas esperadas (resolução e erros) escritas à mão
    │   └── regressao-cp3/         # Arquivos de teste de regressão do CP3
    ├── escopo-design.md           # Rastreamento de ambiente, Symbol e as 5 decisões de projeto
    └── log-uso-ia.md              # Log de transparência no uso de IA
```

---

## Como Compilar e Executar Cada Checkpoint

Requisitos: Java 11 ou superior (testado com JDK 25).

### Checkpoint 1 — Léxico
```bash
cd cp1
javac -encoding UTF-8 -d out src/*.java test/*.java
java -cp out ScannerTest
java -cp out Main exemplos/valido.txt
```

### Checkpoint 2 — Sintático
```bash
cd cp2
javac -encoding UTF-8 -d out src/*.java test/*.java
java -cp out ParserTest
java -cp out ParserMain exemplos/valido.txt
```

### Checkpoint 3 — AST & Visitors
```bash
cd cp3
javac -encoding UTF-8 -d out src/*.java src/ast/*.java src/visitor/*.java test/*.java
java -cp out AstTest
java -cp out ParserTest
java -cp out ParserMain exemplos/valido.txt --arvore
java -cp out ParserMain exemplos/valido.txt --fonte
```

### Checkpoint 4 — Tabela de Símbolos & Identificação
```bash
cd cp4
javac -encoding UTF-8 -d out src/*.java src/ast/*.java src/visitor/*.java src/symbols/*.java test/*.java

# Testes unitários da tabela de símbolos isolada (24 asserções)
java -cp out SymbolTableTest

# Testes da passada de identificação e das 5 decisões de escopo (14 asserções)
java -cp out IdentificationTest

# Testes de regressão contínua
java -cp out AstTest
java -cp out ParserTest

# Execução com exibição da resolução dos identificadores
java -cp out ParserMain test/programas/prog1-valido-sombreamento.txt --resolucao
```

---

## Resumo dos Checkpoints

| Checkpoint | Foco Principal | Estrutura de Saída | Verificação |
| :--- | :--- | :--- | :--- |
| **CP1** | Reconhecimento de tokens com AFDs sem regex externo | Fluxo de `Token` com posição linha:coluna | 29 testes cobrindo maximal munch, literais, escapes e erros |
| **CP2** | Parser LL(1) recursivo descendente | Reconhecimento sintático com recuperação em modo pânico | 18 testes sintáticos e fuzzing com 5000 sequências aleatórias |
| **CP3** | Construção da AST e padrão Visitor | Nós semânticos desacoplados de pontuação; `TreePrinter` e `SourcePrinter` | 19 testes de AST, validação de associatividade e ida-e-volta |
| **CP4** | Análise contextual e tabela de símbolos | Decoração dos nós de uso com `Symbol` e relatório de erros semânticos | 76 testes no total (unitários, semânticos e regressão completa) |

