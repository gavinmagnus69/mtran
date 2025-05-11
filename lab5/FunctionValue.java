import java.util.List;
// Assuming RLexer3.Token and Parser classes are accessible

public class FunctionValue implements Callable {
    final List<RLexer3.Token> parameters;
    final Parser.BlockExpression body;
    final Environment closure; // Environment where the function was defined

    public FunctionValue(List<RLexer3.Token> parameters, Parser.BlockExpression body, Environment closure) {
        this.parameters = parameters;
        this.body = body;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment functionScope = new Environment(this.closure); // New scope, enclosing the definition scope

        // R has complex argument matching (by position, by name, partial matching, ...)
        // This is simplified positional binding.
        if (arguments.size() != parameters.size() && arity() != -1 /* not variadic */) {
             throw new RuntimeException("User function expected " + parameters.size() +
                                       " arguments but got " + arguments.size());
        }

        for (int i = 0; i < parameters.size(); i++) {
            functionScope.define(parameters.get(i).value, arguments.get(i));
        }

        Environment previousEnv = interpreter.getCurrentEnvironment();
        interpreter.setCurrentEnvironment(functionScope);
        try {
            // Execute the function body within the new scope
            return interpreter.evaluate(this.body); // Assuming evaluate(BlockExpr) uses current env and returns last expr val
        } catch (ReturnValue returnValue) {
            // ReturnValue was caught, this is the explicit return from the function
            return returnValue.value;
        } finally {
            interpreter.setCurrentEnvironment(previousEnv); // Always restore the previous environment
        }
        // If no explicit ReturnValue was thrown, R functions implicitly return the value
        // of the last evaluated expression in their body.
        // The 'evaluate(this.body)' call above already returns this if it's a BlockExpression.
        // If the body was empty or didn't result in a value (e.g. only assignments),
        // an implicit NULL might be more R-like, but evaluate(BlockExpression) returning last value is fine.
    }

    @Override
    public String toString() {
        // Could be more descriptive, e.g., print parameter names
        return "<user_function_with_" + parameters.size() + "_params>";
    }
}
