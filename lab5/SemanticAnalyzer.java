import java.util.*;
 // Import your AST classes

public class SemanticAnalyzer {
    private Scope currentScope = new Scope(null);
    public SemanticAnalyzer() {
        // Global scope
        currentScope.defineVariable("return", Type.FUNCTION);
        currentScope.defineVariable("print", Type.FUNCTION);
        currentScope.defineVariable("length", Type.FUNCTION);
    }

    public void analyze(Parser.Expression expr) {
        analyzeExpression(expr);
    }

    private Type analyzeExpression(Parser.Expression expr) {
        if (expr instanceof Parser.BlockExpression block) {
            enterScope();
            for (Parser.Expression inner : block.expressions) {
                analyzeExpression(inner);
            }
            exitScope();
            return Type.UNKNOWN;
        }
        if (expr instanceof Parser.NumberLiteral) {
            return Type.NUMBER;
        }

        if (expr instanceof Parser.StringLiteral) {
            return Type.STRING;
        }

        if (expr instanceof Parser.Identifier id) {
            String name = id.token.value;

            // Handle R constants
            switch (name) {
                case "TRUE", "FALSE" -> { return Type.BOOLEAN; }
                case "NULL", "NA", "NAN", "INF" -> { return Type.UNKNOWN; }
            }

            Type type = currentScope.lookupVariable(name);
            if (type == null) {
                Errors.report("Variable '" + name + "' is not defined.");
                return Type.UNKNOWN;
            }
            return type;
        }

        if (expr instanceof Parser.Assignment assign) {
            if (!(assign.target instanceof Parser.Identifier id)) {
                Errors.report("Left-hand side of assignment must be an identifier.");
                return Type.UNKNOWN;
            }

            String varName = id.token.value;


            if (assign.value instanceof Parser.FunctionExpression funcExpr) {
                Type valueType = Type.FUNCTION;
                currentScope.defineVariable(varName, valueType);            
                List<String> paramNames = funcExpr.parameters.stream()
                    .map(t -> t.value)
                    .toList();
                currentScope.defineFunctionMetadata(varName, paramNames);
                // Analyze body
                enterScope();
                for (RLexer3.Token param : funcExpr.parameters) {
                    currentScope.defineVariable(param.value, Type.UNKNOWN);
                }
                analyzeExpression(funcExpr.body);
                exitScope();
                return Type.FUNCTION;
            }
            Type valueType = analyzeExpression(assign.value);
            // Improve type inference
            if (assign.value instanceof Parser.FunctionExpression) {
                valueType = Type.FUNCTION;
            }
            if (assign.value instanceof Parser.FunctionCall) {
                // Optional: smarter inference
                valueType = Type.FUNCTION;
            }
            currentScope.defineVariable(varName, valueType);
            return valueType;
        }

        if (expr instanceof Parser.BinaryExpression bin) {
            Type left = analyzeExpression(bin.left);
            Type right = analyzeExpression(bin.right);

            if (left != right) {
                Errors.report("Type mismatch in binary expression: " + left + " " + bin.operator.value + " " + right);
                return Type.UNKNOWN;
            }

            return left;
        }

        if (expr instanceof Parser.FunctionCall call) {
            if (!(call.function instanceof Parser.Identifier id)) {
                Errors.report("Cannot call non-identifier as function.");
                return Type.UNKNOWN;
            }
        
            String funcName = id.token.value;
            // Handle built-ins
            if (funcName.equals("return")) {
                if (call.arguments.size() != 1) {
                    Errors.report("return() expects exactly one argument.");
                } else {
                    analyzeExpression(call.arguments.get(0));
                }
                return Type.UNKNOWN;
            }
        
            if (funcName.equals("print") || funcName.equals("length")) {
                for (Parser.Expression arg : call.arguments) {
                    analyzeExpression(arg);
                }
                return Type.UNKNOWN;
            }
            Type variableType = currentScope.lookupVariable(funcName);
            if (variableType == null || variableType != Type.FUNCTION) {
                Errors.report("Function '" + funcName + "' is not defined.");
                return Type.UNKNOWN;
            }

            List<String> paramNames = currentScope.getFunctionParameters(funcName);
            if (paramNames != null) {
                int expected = paramNames.size();
                int actual = call.arguments.size();
                if (expected != actual) {
                    Errors.report("Function '" + funcName + "' expects " + expected + " arguments, got " + actual);
                }
            }
        
            for (Parser.Expression arg : call.arguments) {
                analyzeExpression(arg);
            }
        
            return Type.UNKNOWN;
        }
        if (expr instanceof Parser.FunctionExpression funcExpr) {
            enterScope();
            for (RLexer3.Token param : funcExpr.parameters) {
                currentScope.defineVariable(param.value, Type.UNKNOWN);
            }
            analyzeExpression(funcExpr.body);
            exitScope();
            return Type.FUNCTION;
        }

   

        if (expr instanceof Parser.IfExpression ifExpr) {
            analyzeExpression(ifExpr.condition);
            analyzeExpression(ifExpr.thenBranch);
            if (ifExpr.elseBranch != null) {
                analyzeExpression(ifExpr.elseBranch);
            }
            return Type.UNKNOWN;
        }

        if (expr instanceof Parser.WhileExpression whileExpr) {
            analyzeExpression(whileExpr.condition);
            analyzeExpression(whileExpr.body);
            return Type.UNKNOWN;
        }

        if (expr instanceof Parser.ForExpression forExpr) {
            analyzeExpression(forExpr.iterable);
            enterScope();
            currentScope.defineVariable(forExpr.variable.value, Type.UNKNOWN);
            analyzeExpression(forExpr.body);
            exitScope();
            return Type.UNKNOWN;
        }

        Errors.report("Unhandled expression type: " + expr.getClass().getSimpleName());
        return Type.UNKNOWN;
    }

    private void enterScope() {
        currentScope = new Scope(currentScope);
    }

    private void exitScope() {
        currentScope = currentScope.getParent();
    }
}