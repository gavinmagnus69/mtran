import java.util.List;

public class FunctionValue {
    public final List<RLexer3.Token> parameters;
    public final Parser.BlockExpression body;
    public final Environment closure;

    public FunctionValue(List<RLexer3.Token> parameters, Parser.BlockExpression body, Environment closure) {
        this.parameters = parameters;
        this.body = body;
        this.closure = closure;
    }
}