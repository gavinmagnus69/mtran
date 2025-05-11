// Simplified Environment
import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public Environment getParent() {
        return enclosing;
    }

    public void define(String name, Object value) {
        values.put(name, value); // Defines in current scope only
    }

    public Object get(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        if (enclosing != null) {
            return enclosing.get(name); // Look in outer scopes
        }
        throw new RuntimeException("Undefined variable '" + name + "'.");
    }

    // Assigns to existing variable in current or enclosing scope, or defines in current if not found
        public void assign(String name, Object value) {
            if (values.containsKey(name)) {
    
                values.put(name, value);
            }

        }
       
        public void assignOrDefine(String name, Object value) {
            // This implementation will create in the current environment if not found,
            // or update in the current environment if found.
            // It does not search and update enclosing environments for standard assignment.
            // That behavior is typically for a different assignment operator like R's `<<-`.
            values.put(name, value);
        }

        public void assignGlobalOrEnclosing(String name, Object value) {
            Environment ancestor = this;
            while (ancestor.enclosing != null) { // Go up until just before global
                if (ancestor.values.containsKey(name)) {
                    ancestor.values.put(name, value);
                    return;
                }
                ancestor = ancestor.enclosing;
            }
            // If loop finishes, ancestor is global (or the outermost if no explicit global)
            // or it's the current env if there's no enclosing one.
            ancestor.values.put(name, value); // Assign in the global/outermost scope
        }

        @Override
        public String toString() {
            return "Env{vars=" + values.size() + ", enclosing=" + (enclosing != null) + "}";
        }
}
    