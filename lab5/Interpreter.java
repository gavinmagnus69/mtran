import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class Interpreter {

    private final Environment globals = new Environment(null);
    private Environment current = globals;

    public void interpret(Parser.Expression root) {
        try {
            if(evaluate(root) == null) {
                return;
            }
        } catch (RuntimeException e) {
            System.err.println("[Runtime Error] " + e.getMessage());
        }
    }

    private Object evaluate(Parser.Expression expr) {
        if(!checkParseErrors()){
            return null;
        }
        if (expr instanceof Parser.NumberLiteral num) {
            return Double.parseDouble(num.token.value);
        }

        if (expr instanceof Parser.StringLiteral str) {
            return str.token.value;
        }

        if (expr instanceof Parser.Identifier id) {
            return current.get(id.token.value);
        }

        if (expr instanceof Parser.Assignment assign) {
            if (!(assign.target instanceof Parser.Identifier id)) {
                throw new RuntimeException("Left-hand side of assignment must be identifier.");
            }
            Object value = evaluate(assign.value);
            String name = id.token.value;
            if (assign.operator.type == RLexer3.TokenType.ASSIGN_LEFT) {
                current.define(name, value);
            } else {
                current.assign(name, value);
            }
            return value;
        }

        if (expr instanceof Parser.BinaryExpression bin) {
            Object left = evaluate(bin.left);
            Object right = evaluate(bin.right);

            return switch (bin.operator.type) {
                case PLUS -> (Double) left + (Double) right;
                case MINUS -> (Double) left - (Double) right;
                case MULTIPLY -> (Double) left * (Double) right;
                case DIVIDE -> (Double) left / (Double) right;
                case LT -> (Double) left < (Double) right;
                case GT -> (Double) left > (Double) right;
                case EQ -> left.equals(right);
                default -> throw new RuntimeException("Unsupported binary operator: " + bin.operator.value);
            };
        }

        if (expr instanceof Parser.BlockExpression block) {
            Object result = null;
            for (Parser.Expression inner : block.expressions) {
                result = evaluate(inner);
            }
            return result;
        }

        if (expr instanceof Parser.IfExpression ifExpr) {
            Object condition = evaluate(ifExpr.condition);
            if (asBoolean(condition)) {
                return evaluate(ifExpr.thenBranch);
            } else if (ifExpr.elseBranch != null) {
                return evaluate(ifExpr.elseBranch);
            } else {
                return null;
            }
        }

        if (expr instanceof Parser.WhileExpression whileExpr) {
            while (asBoolean(evaluate(whileExpr.condition))) {
                evaluate(whileExpr.body);
            }
            return null;
        }

        if (expr instanceof Parser.ForExpression forExpr) {
            Object iterable = evaluate(forExpr.iterable);
            if (!(iterable instanceof Double)) {
                throw new RuntimeException("Only numeric for-loops are supported.");
            }
            int max = ((Double) iterable).intValue();
            for (int i = 1; i <= max; i++) {
                Environment loopScope = new Environment(current);
                loopScope.define(forExpr.variable.value, (double) i);
                current = loopScope;
                evaluate(forExpr.body);
                current = loopScope.getParent();
            }
            return null;
        }

        if (expr instanceof Parser.FunctionExpression funcExpr) {
            return new FunctionValue(funcExpr.parameters, funcExpr.body, current);
        }

        if (expr instanceof Parser.FunctionCall call) {
            if (!(call.function instanceof Parser.Identifier id)) {
                throw new RuntimeException("Can only call named functions.");
            }
        
            String funcName = id.token.value;
        
            if (funcName.equals("return")) {
                if (call.arguments.size() != 1) {
                    throw new RuntimeException("return() expects exactly 1 argument.");
                }
                Object value = evaluate(call.arguments.get(0));
                throw new ReturnValue(value);
            }

            if (funcName.equals("print")) {
                for (Parser.Expression arg : call.arguments) {
                    Object value = evaluate(arg);
                    if (value instanceof FunctionValue) {
                        System.out.println("<function>");
                    } else {
                        System.out.println(value);
                    }
                }
                return null;
            }

            Object funcObj = current.get(funcName);
            if (!(funcObj instanceof FunctionValue function)) {
                throw new RuntimeException("'" + funcName + "' is not a function.");
            }
        
            if (call.arguments.size() != function.parameters.size()) {
                throw new RuntimeException("Function '" + funcName + "' expects " + function.parameters.size() + " arguments.");
            }
        
            Environment functionEnv = new Environment(function.closure);
            for (int i = 0; i < function.parameters.size(); i++) {
                String paramName = function.parameters.get(i).value;
                Object argValue = evaluate(call.arguments.get(i));
                functionEnv.define(paramName, argValue);
            }
        
            Environment previous = current;
            current = functionEnv;
        
            try {
                evaluate(function.body);
            } catch (ReturnValue ret) {
                current = previous;
                return ret.value;
            }
        
            current = previous;
            return null;
        }

        throw new RuntimeException("Unknown expression type: " + expr.getClass().getSimpleName());
    }

    private boolean checkParseErrors() {
         try {
            RLexer3.checkAllTockens();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean asBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Double d) return d != 0;
        return value != null;
    }
}