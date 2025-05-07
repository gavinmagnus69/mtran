

import java.util.List;

public class FunctionSymbol {
    public final String name; // can be null for anonymous
    public final List<String> parameters;

    public FunctionSymbol(String name, List<String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }
}