import java.util.List;

import ast.Program;

public class ParseResult {
    public final Program program;
    public final List<SyntaxError> syntaxErrors;
    public final List<LexicalError> lexicalErrors;
    public final boolean aceito;

    public ParseResult(Program program, List<SyntaxError> syntaxErrors, List<LexicalError> lexicalErrors) {
        this.program = program;
        this.syntaxErrors = syntaxErrors;
        this.lexicalErrors = lexicalErrors;
        this.aceito = program != null;
    }
}
