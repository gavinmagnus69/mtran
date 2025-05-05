// import java.beans.Expression;
import java.util.List;
import java.io.*;
import java.util.*;


public class Main {


    public static class AstPrinter {

        public void print(Parser.Expression node) {
            print(node, 0);
        }
    
        private void print(Parser.Expression expr, int indent) {
            if (expr == null) {
                println(indent, "null");
                return;
            }
    
            if (expr instanceof Parser.NumberLiteral num) {
                println(indent, "Number: " + num.token.value);
            } else if (expr instanceof Parser.StringLiteral str) {
                println(indent, "String: " + str.token.value);
            } else if (expr instanceof Parser.Identifier id) {
                println(indent, "Identifier: " + id.token.value);
            } else if (expr instanceof Parser.BinaryExpression bin) {
                println(indent, "BinaryExpression: " + bin.operator.value);
                print(bin.left, indent + 2);
                print(bin.right, indent + 2);
            } else if (expr instanceof Parser.Assignment assign) {
                println(indent, "Assignment: " + assign.operator.value);
                print(assign.target, indent + 2);
                print(assign.value, indent + 2);
            } else if (expr instanceof Parser.FunctionCall call) {
                println(indent, "FunctionCall:");
                print(call.function, indent + 2);
                for (Parser.Expression arg : call.arguments) {
                    print(arg, indent + 4);
                }
            } else if (expr instanceof Parser.IfExpression ifExpr) {
                println(indent, "IfExpression:");
                println(indent + 2, "Condition:");
                print(ifExpr.condition, indent + 4);
                println(indent + 2, "Then:");
                print(ifExpr.thenBranch, indent + 4);
                if (ifExpr.elseBranch != null) {
                    println(indent + 2, "Else:");
                    print(ifExpr.elseBranch, indent + 4);
                }
            } else if (expr instanceof Parser.WhileExpression whileExpr) {
                println(indent, "WhileExpression:");
                println(indent + 2, "Condition:");
                print(whileExpr.condition, indent + 4);
                println(indent + 2, "Body:");
                print(whileExpr.body, indent + 4);
            } else if (expr instanceof Parser.ForExpression forExpr) {
                println(indent, "ForExpression:");
                println(indent + 2, "Variable: " + forExpr.variable.value);
                println(indent + 2, "Iterable:");
                print(forExpr.iterable, indent + 4);
                println(indent + 2, "Body:");
                print(forExpr.body, indent + 4);
            } else if (expr instanceof Parser.BlockExpression block) {
                println(indent, "Block:");
                for (Parser.Expression inner : block.expressions) {
                    print(inner, indent + 2);
                }
            } else {
                println(indent, "Unknown expression type: " + expr.getClass().getSimpleName());
            }
        }
    
        private void println(int indent, String message) {
            System.out.println(" ".repeat(indent) + message);
        }
    }
    public static void main(String[] args) {
        try {
            /////////////////////////////////////
            /// LEXER
            String rCode = RLexer3.readFile("./lex.r");
            RLexer3 lexer = new RLexer3(rCode);
            List<RLexer3.Token> tokens = lexer.lex();
            RLexer3.printTokenLineByLine(tokens);
            lexer.printGroupedTokens();

            //////////////////////////////////
            /// PARSER
            Parser parser = new Parser(tokens);
            Parser.Expression ast = parser.parse();
            if (ast == null) {
                System.err.println("Syntax error: AST could not be built.");
                return;
            }
            System.out.println("Parsed AST: " + ast.getClass().getSimpleName());
            AstPrinter printer = new AstPrinter();
            printer.print(ast);
            // System.out.println(ast);
        } catch (IOException e) {
            System.err.println("Error during lexical analysis: " + e.getMessage());
            e.printStackTrace(); // Print the stack trace for debugging
        }
        catch (Parser.ParseError error) {
            System.err.println(error.getMessage());
        }
       
    }
}
