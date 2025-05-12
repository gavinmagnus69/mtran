import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // For Objects.equals


public class Interpreter {

    private final Environment globals = new Environment(null);
    private Environment environment = globals; // Renamed 'current' to 'environment' for clarity

    // --- Helper to get current environment (useful if 'environment' is passed around) ---
    public Environment getCurrentEnvironment() {
        return this.environment;
    }
    public void setCurrentEnvironment(Environment env) {
        this.environment = env;
    }


    private static Double coerceToDouble(Object value) {
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof Boolean b) return b ? 1.0 : 0.0;
        return Double.NaN;
    }

    public Interpreter() {
        // Define R's built-in constants
        globals.define("TRUE", true);
        globals.define("FALSE", false);
        globals.define("NULL", null); // Representing R NULL with Java null
        // globals.define("NA", NaObject.INSTANCE); // For more R-like NA
        globals.define("Inf", Double.POSITIVE_INFINITY);
        globals.define("NaN", Double.NaN);
        globals.define("as.numeric", new NativeFunction("as.numeric", 1, (interpreter, args) -> {
            Object input = args.get(0);

            if (input instanceof List<?> list) {
                List<Object> result = new ArrayList<>();
                for (Object item : list) {
                    result.add(coerceToDouble(item));
                }
                return result;
            } else {
                return coerceToDouble(input);
            }
        }));
        globals.define("sapply", new NativeFunction("sapply", 2, (interpreter, args) -> {
            Object input = args.get(0);
            Object func = args.get(1);
            
            // System.out.println(">>> sapply: input is of type = " + (input == null ? "null" : input.getClass().getSimpleName()));            
            
            if (!(input instanceof List<?> iterable)) {
                throw new RuntimeException("sapply: first argument must be a list or vector.");
            }

            if (!(func instanceof Callable callable)) {
                throw new RuntimeException("sapply: second argument must be a function.");
            }

            List<Object> results = new ArrayList<>();
            for (Object item : iterable) {
                List<Object> callArgs = new ArrayList<>();
                callArgs.add(item); // Pass item as x
                Object result = callable.call(interpreter, callArgs);
                results.add(result);
            }

            return results; // Simplified: returns a list. R tries to "simplify" too (to vector/matrix), which you can skip for now.
        }));

        globals.define("cat", new NativeFunction("cat", -1, (interpreter, args) -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                Object arg = args.get(i);

                // Use raw string without quotes
                String str = interpreter.stringifyRaw(arg);
                sb.append(str);
                if (i < args.size() - 1) sb.append(" ");
            }

            // Handle newline
            String output = sb.toString();
            if (output.contains("\n")) {
                // At least one new line already present: just print as-is
                System.out.print(output);
            } else {
                System.out.print(output);
            }
            return null;
        }));



        globals.define("print", new NativeFunction("print", 1, (interpreter, args) -> {
            if (args.isEmpty()) throw new RuntimeException("print expects 1 argument.");
            System.out.println(interpreter.stringify(args.get(0)));
            return args.get(0); // print returns its argument invisibly
        }));

        globals.define("list", new NativeFunction("list", -1, (interpreter, args) -> {
            // R's list can have named arguments. This is simplified.
            // For now, just return a Java List of the evaluated arguments.
            return new ArrayList<>(args); // The arguments are already evaluated objects
        }));

        // `sapply` and `as.numeric` are more complex and would require more infrastructure
        // (e.g., proper vector/list handling, type coercion logic).
        // Placeholder for as.numeric:
        
        // Placeholder for paste (simplified, no 'sep' or 'collapse' handling here)
        globals.define("paste", new NativeFunction("paste", -1, (interpreter, args) -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                sb.append(interpreter.stringify(args.get(i)));
                if (i < args.size() - 1) {
                    sb.append(" "); // Default separator
                }
            }
            return sb.toString();
        }));

    }

    public void interpret(Parser.Expression program) { // program is likely a BlockExpression
        try {
            evaluate(program);
        } catch (RuntimeException e) {
            // Avoid printing stack trace for known ReturnValue, BreakException, NextException
            if (!(e instanceof ReturnValue || e instanceof BreakException || e instanceof NextException)) {
                 System.err.println("[Runtime Error] " + e.getMessage());
                 // e.printStackTrace(); // Optionally print stack trace for unexpected errors
            } else if (e instanceof ReturnValue && environment != globals) {
                 // A ReturnValue escaped outside a function call to the top level
                 System.err.println("[Runtime Error] 'return' used outside of a function.");
            }
        }
    }

    public Object evaluate(Parser.Expression expr) {
        if (expr == null) return null; // Should not happen if parser is correct

        if (expr instanceof Parser.NumberLiteral num) {
            return Double.parseDouble(num.token.value);
        }

        if (expr instanceof Parser.StringLiteral str) {
            return str.token.value;
        }

        if (expr instanceof Parser.Identifier id) {
            return environment.get(id.token.value);
        }

        if (expr instanceof Parser.Assignment assign) {
            // For now, only simple identifier assignment is supported by this logic
            if (!(assign.target instanceof Parser.Identifier id)) {
                // To support x[1] <- value or x$a <- value, assign.target needs different handling
                throw new RuntimeException("Left-hand side of assignment must be a simple identifier (complex assignments not yet supported).");
            }
            Object value = evaluate(assign.value);
            String name = id.token.value;

            // In R, `<-` and `=` generally assign in the current environment.
            // `<<-` assigns in an enclosing environment.
            // Your ASSIGN_LEFT vs other assign ops distinction might be for this, or just syntactic.
            // For now, both define/assign in current.
            // `define` is for new variables, `assign` for existing.
            // R's `=` and `<-` typically can create new variables.
            environment.assignOrDefine(name, value); // Environment needs this method
            return value;
        }

        if (expr instanceof Parser.BinaryExpression bin) {


            
            
            
            Object left = evaluate(bin.left);
            Object right = evaluate(bin.right);
    
    
            // System.out.println("DEBUG: evaluating binary expression:");
            // System.out.println("  OPERATOR = " + bin.operator.type);
            // System.out.println("  LEFT     = " + stringify(left));
            // System.out.println("  RIGHT    = " + stringify(right));

            // Basic type checking for arithmetic
            if (bin.operator.type == RLexer3.TokenType.PLUS ||
                bin.operator.type == RLexer3.TokenType.MINUS ||
                bin.operator.type == RLexer3.TokenType.MULTIPLY ||
                bin.operator.type == RLexer3.TokenType.DIVIDE ||
                bin.operator.type == RLexer3.TokenType.MODULO ||
                bin.operator.type == RLexer3.TokenType.POWER ||
                // Comparisons also often expect numeric in simple R, though it coerces
                bin.operator.type == RLexer3.TokenType.LT ||
                bin.operator.type == RLexer3.TokenType.GT ||
                bin.operator.type == RLexer3.TokenType.LE ||
                bin.operator.type == RLexer3.TokenType.GE) {
                if (!(left instanceof Double) || !(right instanceof Double)) {
                    // R would try to coerce. For now, error.
                    throw new RuntimeException("Operands must be numeric for operator '" + bin.operator.value + "'. Got: " + stringify(left) + " and " + stringify(right));
                }
            }


            return switch (bin.operator.type) {
                case PLUS -> (Double) left + (Double) right;
                case SEQUENCE -> {
                    if (!(left instanceof Double) || !(right instanceof Double)) {
                        throw new RuntimeException("Operands for : must be numeric");
                    }
                    double start = (Double) left;
                    double end = (Double) right;
                    List<Double> result = new ArrayList<>();
                    if (start <= end) {
                        for (double i = start; i <= end; i++) result.add(i);
                    } else {
                        for (double i = start; i >= end; i--) result.add(i);
                    }
                    yield result;
                }
                case MINUS -> (Double) left - (Double) right;
                case MULTIPLY -> (Double) left * (Double) right;
                case DIVIDE -> {
                    if ((Double) right == 0.0) throw new RuntimeException("Division by zero.");
                    yield (Double) left / (Double) right;
                }
                case MODULO -> (Double) left % (Double) right;
                case POWER -> Math.pow((Double) left, (Double) right);
                case LT -> (Double) left < (Double) right;
                case GT -> (Double) left > (Double) right;
                case LE -> (Double) left <= (Double) right;
                case GE -> (Double) left >= (Double) right;
                case EQ -> isEqual(left, right); // R's == is element-wise. This is scalar.
                case NE -> !isEqual(left, right);
                // SEQUENCE for 'for' loop is handled in ForExpression eval for now
                // If ':' can be a general binary op resulting in a vector, needs more here.
                default -> throw new RuntimeException("Unsupported binary operator: " + bin.operator.type + " (" + bin.operator.value + ")");
            };
        }

        if (expr instanceof Parser.BlockExpression block) {
            Object result = null; // R blocks evaluate to their last expression
            // Block does not create a new scope unless it's a function body
            // Environment currentEnv = this.environment; // Use the current environment for the block
            // If blocks *should* create scopes:
            // Environment blockEnv = new Environment(this.environment);
            // Environment previousEnv = this.environment;
            // this.environment = blockEnv;
            for (Parser.Expression innerExpr : block.expressions) {
                result = evaluate(innerExpr);
            }
            // if (previousEnv != null) this.environment = previousEnv; // Restore if scope was created
            return result;
        }

        if (expr instanceof Parser.IfExpression ifExpr) {
            Object conditionValue = evaluate(ifExpr.condition);
            if (asBoolean(conditionValue)) {
                return evaluate(ifExpr.thenBranch);
            } else if (ifExpr.elseBranch != null) {
                return evaluate(ifExpr.elseBranch);
            } else {
                return null; // R if without else and false condition returns NULL
            }
        }

        if (expr instanceof Parser.ReturnStatement retStmt) {
            Object value = null;
            if (retStmt.value != null) {
                value = evaluate(retStmt.value);
            }
            throw new ReturnValue(value);
        }

        if (expr instanceof Parser.FunctionExpression funcDef) {
            // When a function definition is encountered, create a FunctionValue (closure)
            return new FunctionValue(funcDef.parameters, funcDef.body, this.environment);
        }

        if (expr instanceof Parser.FunctionCall call) {
            
            Object callee = evaluate(call.function); // Evaluate the expression that should yield a function
            // System.out.println(">>> Evaluating function call: " + stringify(call.function));
            List<Object> arguments = new ArrayList<>();
            for (Parser.Expression argExpr : call.arguments) {
                Object arg = evaluate(argExpr);
                arguments.add(arg);
                // System.out.println(">>> Argument evaluated to: " + stringify(arg) + " (" +
                //              (arg == null ? "null" : arg.getClass().getSimpleName()) + ")");
            
            }

            if (!(callee instanceof Callable functionToCall)) {
                throw new RuntimeException("Can only call functions and callables. Tried to call: " + stringify(callee));
            }

            // Arity check (simplified, R has complex argument matching)
            if (functionToCall.arity() != -1 && functionToCall.arity() != arguments.size()) {
                // Get function name if possible for better error
                String funcName = (call.function instanceof Parser.Identifier id) ? id.token.value : "anonymous function";
                if (callee instanceof NativeFunction nf) funcName = nf.getName(); // If NativeFunction has a getName()

                throw new RuntimeException("Function '" + funcName + "' expected " + functionToCall.arity() +
                                           " arguments but got " + arguments.size());
            }
            return functionToCall.call(this, arguments); // Pass interpreter for context if needed by Callable
        }
        
        if (expr instanceof Parser.ForExpression forExpr) {
            Object iterableEvaluated = evaluate(forExpr.iterable);
            List<Object> sequenceToIterate = new ArrayList<>();

            if (forExpr.iterable instanceof Parser.BinaryExpression seqBin && seqBin.operator.type == RLexer3.TokenType.SEQUENCE) {
                // Already evaluated left and right when seqBin was constructed, but we need the values
                // This assumes sequence is like 1:10. Real R sequence can be more complex.
                Object startObj = evaluate(seqBin.left); // Re-evaluate in current context
                Object endObj = evaluate(seqBin.right);

                if (!(startObj instanceof Double) || !(endObj instanceof Double)) {
                    throw new RuntimeException("Sequence operands for 'for' loop must be numeric for ':' operator.");
                }
                double start = (Double)startObj;
                double end = (Double)endObj;
                // R's seq_along, seq_len behavior might be more appropriate for general iterables
                if (start <= end) {
                    for (double val = start; val <= end; val++) { sequenceToIterate.add(val); }
                } else {
                    for (double val = start; val >= end; val--) { sequenceToIterate.add(val); }
                }
            } else if (iterableEvaluated instanceof List) { // If the iterable is already a Java List
                sequenceToIterate.addAll((List<?>) iterableEvaluated);
            }
            // Add other iterable types: e.g. R vectors if you implement them
            else {
                throw new RuntimeException("For loop iterable must be a sequence (e.g., 1:10) or a list/vector. Got: " + stringify(iterableEvaluated));
            }

            Environment outerEnvForLoop = this.environment; // Environment before the loop starts
            for (Object item : sequenceToIterate) {
                Environment iterationScope = new Environment(outerEnvForLoop); // New scope for each iteration, enclosing the outer one
                iterationScope.define(forExpr.variable.value, item);

                this.environment = iterationScope; // Set current environment for the body execution
                try {
                    evaluate(forExpr.body);
                } catch (ReturnValue ret) { this.environment = outerEnvForLoop; throw ret; } // Propagate returns
                catch (BreakException breakEx) { this.environment = outerEnvForLoop; break; }
                catch (NextException nextEx) { this.environment = outerEnvForLoop; continue; /* Still need to restore env for this iteration before continuing */ }
                finally { // Ensure this.environment is restored even if 'next' happens
                    this.environment = outerEnvForLoop; // Restore for the *next* iteration's new scope or after loop
                }
            }
            return null; // For loop itself returns NULL
        }
        
        if (expr instanceof Parser.WhileExpression whileExpr) {
            while (asBoolean(evaluate(whileExpr.condition))) {
                try {
                    evaluate(whileExpr.body);
                } catch (ReturnValue ret) { throw ret; }
                catch (BreakException breakEx) { break; }
                catch (NextException nextEx) { continue; }
            }
            return null; // While loop itself returns NULL
        }

        // MemberAccess and IndexExpression (Placeholders - requires R-like objects)
        if (expr instanceof Parser.MemberAccess memberAccess) {
            Object object = evaluate(memberAccess.object);
            // R uses environments, lists, data.frames for $
            // This is highly dependent on your object system.
            if (object instanceof Environment env) { // If accessing member of an environment
                return env.get(memberAccess.member.value);
            }
            if (object instanceof List<?> list && memberAccess.member.value.matches("\\d+")) { // list$1 - not standard R but for example
                 // R list member access by $ expects names, not numeric indices.
                 // This is a placeholder.
                 throw new RuntimeException("Accessing list by $ with numeric index not standard R. Use [[index]].");
            }
            throw new RuntimeException("Object does not support $ operator or member '" + memberAccess.member.value + "': " + stringify(object));
        }

        if (expr instanceof Parser.IndexExpression indexAccess) {
            Object object = evaluate(indexAccess.object);
            Object index = evaluate(indexAccess.index); // R indexing can be complex (numeric, logical, character vectors)

            if (object instanceof List<?> list) {
                if (index instanceof List<?> indices) {
                    List<Object> result = new ArrayList<>();

                    if (!indices.isEmpty() && indices.get(0) instanceof Boolean) {
                        if (indices.size() != list.size()) {
                            throw new RuntimeException("Logical index length does not match list size.");
                        }
                        for (int i = 0; i < indices.size(); i++) {
                            if (Boolean.TRUE.equals(indices.get(i))) {
                                result.add(list.get(i)); // Keep the element
                            }
                        }
                        return result;
                    }

                    for (Object idxObj : indices) {
                        if (!(idxObj instanceof Double d)) {
                            throw new RuntimeException("Indices must be numeric or logical");
                        }
                        int idx = d.intValue();
                        if (idx < 1 || idx > list.size()) continue;
                        result.add(list.get(idx - 1));
                    }

                    return result;
                }
                else if (index instanceof Double dIndex) {
                    int idx = dIndex.intValue();
                    return list.get(idx - 1);
                } else {
                    throw new RuntimeException("List index must be numeric or list of numeric.");
                }
            }
            // Add support for other indexable types (vectors, matrices)
            throw new RuntimeException("Object does not support [] or [[]] operator: " + stringify(object));
        }


        throw new RuntimeException("Unknown or unhandled expression type: " + expr.getClass().getSimpleName());
    }

    // --- Helper methods ---
    private boolean asBoolean(Object value) {
        if (value == null) return false; // R's NULL is not FALSE in conditions, often error or length 0 logical
        if (value instanceof Boolean b) return b;
        if (value instanceof Double d) {
            if (d.isNaN()) return false; // Or handle NA propagation
            return d != 0.0;
        }
        if (value instanceof String s) return !s.isEmpty(); // R string to logical is different
        // R's truthiness: empty vector is FALSE, non-empty is TRUE (if logical/numeric)
        // This is a simplification.
        return true; // Default to true for other non-null objects (like functions, lists)
    }

    private boolean isEqual(Object a, Object b) {
        // R's `==` is element-wise and has specific NA/NULL handling.
        // `identical()` is closer to Java's `equals` or `==` for object identity.
        // This is a very simplified scalar comparison.
        if (a == null && b == null) return true; // R: identical(NULL, NULL) is TRUE
        if (a == null || b == null) return false; // R: NULL == anything is logical(0)
        // Add NA handling: if a or b is NA, result is NA (a distinct logical value)
        return Objects.equals(a, b);
    }

    public String stringify(Object object) {
        if (object == null) return "NULL";
        if (object instanceof Double) {
            Double d = (Double) object;
            if (d.isNaN()) return "NaN";
            if (d.isInfinite()) return d > 0 ? "Inf" : "-Inf";
            // R prints 1.0 as 1 (if it's effectively an integer)
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return Integer.toString(d.intValue());
            }
            return d.toString();
        }
        if (object instanceof Boolean b) return b ? "TRUE" : "FALSE";
        if (object instanceof String s) return "\"" + s + "\""; // R prints strings with quotes in some contexts
        if (object instanceof FunctionValue) return "<user_function>";
        if (object instanceof NativeFunction nf) return "<native_function: " + nf.getName() + ">";
        if (object instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(stringify(list.get(i)));
                if (i < list.size() - 1) sb.append(", ");
            }
            return sb.toString();
        }
        return Objects.toString(object);
    }


    public String stringifyRaw(Object object) {
        if (object == null) return "";
        if (object instanceof String s) {
            return s.replace("\\n", "\n"); // If you still get literal \n, fix here
        }
        return stringify(object); // fallback
    }


    // Environment class should be an inner class or a separate file
    // Ensure Environment has assignOrDefine(String name, Object value)
    // and getParent()
}
