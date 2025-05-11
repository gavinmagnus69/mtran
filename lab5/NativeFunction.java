import java.util.List;
import java.util.function.BiFunction; // Changed to BiFunction

public class NativeFunction implements Callable {
    private final String name;
    private final int arity;
    // Takes Interpreter and List<Object>, returns Object
    private final BiFunction<Interpreter, List<Object>, Object> body;


    public NativeFunction(String name, int arity, BiFunction<Interpreter, List<Object>, Object> body) {
        this.name = name;
        this.arity = arity;
        this.body = body;
    }

    public String getName() { return name; } // Useful for stringify

    @Override
    public int arity() {
        return arity;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return body.apply(interpreter, arguments);
    }

    @Override
    public String toString() {
        return "<native_function: " + name + ">";
    }
}
