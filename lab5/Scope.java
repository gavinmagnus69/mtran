
import java.util.*;
public class Scope {
    private final Scope parent;
    private final Map<String, Type> variables = new HashMap<>();
    private final Map<String, List<String>> functionParameters = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    public void defineVariable(String name, Type type) {
        variables.put(name, type);
    }

    public Type lookupVariable(String name) {
        if (variables.containsKey(name)) return variables.get(name);
        if (parent != null) return parent.lookupVariable(name);
        return null;
    }

    public boolean isVariableDefined(String name) {
        return lookupVariable(name) != null;
    }

    public void defineFunctionMetadata(String name, List<String> paramNames) {
        functionParameters.put(name, paramNames);
    }

    public List<String> getFunctionParameters(String name) {
        if (functionParameters.containsKey(name)) return functionParameters.get(name);
        if (parent != null) return parent.getFunctionParameters(name);
        return null;
    }

    public Scope getParent() {
        return parent;
    }
}