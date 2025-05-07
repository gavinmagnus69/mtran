
import java.util.*;

public class Scope {
    private final Scope parent;
    private final Map<String, Type> variables = new HashMap<>();
    private final Map<String, FunctionSymbol> functions = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    public void defineVariable(String name, Type type) {
        variables.put(name, type);
    }

    public boolean isVariableDefined(String name) {
        return lookupVariable(name) != null;
    }

    public Type lookupVariable(String name) {
        if (variables.containsKey(name)) return variables.get(name);
        if (parent != null) return parent.lookupVariable(name);
        return null;
    }

    public void defineFunction(FunctionSymbol symbol) {
        functions.put(symbol.name, symbol);
    }

    public FunctionSymbol lookupFunction(String name) {
        if (functions.containsKey(name)) return functions.get(name);
        if (parent != null) return parent.lookupFunction(name);
        return null;
    }

    public Scope getParent() {
        return parent;
    }
}