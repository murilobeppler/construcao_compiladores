package tipos;

import java.util.ArrayList;
import java.util.List;

import ast.Program;
import symbols.ContextualError;
import visitor.IdentificationVisitor;

/**
 * Análise contextual completa: identificação (CP4) seguida da verificação de tipos (CP5).
 *
 * A verificação de tipos roda mesmo se a identificação achou erros: um nome não declarado
 * fica com decl == null, tem tipo ERRO e não gera mensagem nova. Assim uma única rodada
 * reporta os dois tipos de erro.
 *
 * Formato de saída (determinístico), uma mensagem por linha, em ordem de linha e coluna:
 *   linha 3: condição de 'if' deve ser bool, mas é int
 *   linha 9: aviso: 'x' pode ser usada sem valor
 * Sem erros nem avisos, a saída é a linha única "sem erros".
 *
 * Com qualquer erro (avisos não contam), ok() é false e o compilador não segue para a
 * geração de código.
 */
public class Verificador {

    public final List<ContextualError> erros;
    public final List<ContextualError> avisos;

    private Verificador(List<ContextualError> erros, List<ContextualError> avisos) {
        this.erros = erros;
        this.avisos = avisos;
    }

    public static Verificador verifica(Program prog) {
        List<ContextualError> erros = new ArrayList<>(IdentificationVisitor.run(prog));
        TypeChecker tc = TypeChecker.run(prog);
        erros.addAll(tc.getErros());
        ordena(erros);
        return new Verificador(erros, tc.getAvisos());
    }

    /** true quando não há erro (avisos são permitidos): pode seguir para a geração de código. */
    public boolean ok() {
        return erros.isEmpty();
    }

    /** Erros e avisos juntos, na ordem do texto, no formato documentado acima. */
    public String formata() {
        List<ContextualError> todas = new ArrayList<>(erros);
        todas.addAll(avisos);
        ordena(todas);
        if (todas.isEmpty()) {
            return "sem erros";
        }
        StringBuilder sb = new StringBuilder();
        for (ContextualError e : todas) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(e);
        }
        return sb.toString();
    }

    private static void ordena(List<ContextualError> l) {
        l.sort((a, b) -> {
            int c = Integer.compare(a.line, b.line);
            return c != 0 ? c : Integer.compare(a.col, b.col);
        });
    }
}
