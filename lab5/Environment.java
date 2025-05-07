import java.util.*;

public class Environment {
    private final Environment parent;
    private final Map<String, Object> variables = new HashMap<>();
    private final Map<String, FunctionValue> functions = new HashMap<>();




    public Environment(Environment parent) {
        this.parent = parent;
    }

    public Environment getParent() {
        return parent;
    }
    
    public void define(String name, Object value) {
        variables.put(name, value);
    }

    public void assign(String name, Object value) {
        if (variables.containsKey(name)) {
            variables.put(name, value);
        } else if (parent != null) {
            parent.assign(name, value);
        } else {
            throw new RuntimeException("Undefined variable '" + name + "'");
        }
    }

    public Object get(String name) {
        if (variables.containsKey(name)) return variables.get(name);
        if (parent != null) return parent.get(name);
        throw new RuntimeException("Undefined variable '" + name + "'");
    }

    public void defineFunction(String name, FunctionValue func) {
        functions.put(name, func);
    }

    public FunctionValue getFunction(String name) {
        if (functions.containsKey(name)) return functions.get(name);
        if (parent != null) return parent.getFunction(name);
        throw new RuntimeException("Function '" + name + "' is not defined.");
    }

    public boolean isDefined(String name) {
        return variables.containsKey(name) || (parent != null && parent.isDefined(name));
    }
}