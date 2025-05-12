

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// AstNode.java


public class Parser {
    public abstract class AstNode {}
    public abstract class Expression extends AstNode {}
    public class NumberLiteral extends Expression {
        public final RLexer3.Token token;
        public NumberLiteral(RLexer3.Token token) {
            this.token = token;
        }
    }
    public class StringLiteral extends Expression {
        public final RLexer3.Token token;
    
        public StringLiteral(RLexer3.Token token) {
            this.token = token;
        }
    }
    public class Identifier extends Expression {
        public final RLexer3.Token token;
    
        public Identifier(RLexer3.Token token) {
            this.token = token;
        }
    }
// Binary Expression
    public class BinaryExpression extends Expression {
        public final Expression left;
        public final RLexer3.Token operator;
        public final Expression right;

        public BinaryExpression(Expression left, RLexer3.Token operator, Expression right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
    }
// Assignment
    public class Assignment extends Expression {
        public final Expression target;
        public final RLexer3.Token operator;
        public final Expression value;

        public Assignment(Expression target, RLexer3.Token operator, Expression value) {
            this.target = target;
            this.operator = operator;
            this.value = value;
        }
    }
    public class FunctionExpression extends Expression {
        public final List<RLexer3.Token> parameters;
        public final BlockExpression body;
    
        public FunctionExpression(List<RLexer3.Token> parameters, BlockExpression body) {
            this.parameters = parameters;
            this.body = body;
        }
    }
    public class FunctionCall extends Expression {
        public final Expression function;
        public final List<Expression> arguments;
    
        public FunctionCall(Expression function, List<Expression> arguments) {
            this.function = function;
            this.arguments = arguments;
        }
    }
    public class IfExpression extends Expression {
        public final Expression condition;
        public final Expression thenBranch;
        public final Expression elseBranch;
    
        public IfExpression(Expression condition, Expression thenBranch, Expression elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
    }
    public class WhileExpression extends Expression {
        public final Expression condition;
        public final Expression body;
    
        public WhileExpression(Expression condition, Expression body) {
            this.condition = condition;
            this.body = body;
        }
    }
    public class ForExpression extends Expression {
        public final RLexer3.Token variable;
        public final Expression iterable;
        public final Expression body;
    
        public ForExpression(RLexer3.Token variable, Expression iterable, Expression body) {
            this.variable = variable;
            this.iterable = iterable;
            this.body = body;
        }
    }

    public class BlockExpression extends Expression {
        public final List<Expression> expressions;
    
        public BlockExpression(List<Expression> expressions) {
            this.expressions = expressions;
        }
    }

    public class MemberAccess extends Expression {
        public final Expression object;
        public final RLexer3.Token member;

        public MemberAccess(Expression object, RLexer3.Token member) {
            this.object = object;
            this.member = member;
        }
    }

    public class IndexExpression extends Expression {
        public final Expression object;
        public final Expression index; // Or List<Expression> for multiple indices like m[1,2]
        public final boolean isDoubleBracket;

        public IndexExpression(Expression object, Expression index, boolean isDoubleBracket) {
            this.object = object;
            this.index = index;
            this.isDoubleBracket = isDoubleBracket;
        }
    }

    public class ReturnStatement extends Expression { // Or extends Statement if you have that base
        public final RLexer3.Token keyword;
        public final Expression value; // Can be null

        public ReturnStatement(RLexer3.Token keyword, Expression value) {
            this.keyword = keyword;
            this.value = value;
        }
    }



    private final List<RLexer3.Token> tokens;
    private int current = 0;

    public Parser(List<RLexer3.Token> tokens) {
        this.tokens = tokens;
    }

    public Expression parse() {
        try {
            List<Expression> programStatements = new ArrayList<>();
            while (!isAtEnd()) {
                try {
                    programStatements.add(statement()); // Parse one full statement
                    match(RLexer3.TokenType.SEMICOLON); // Optional semicolon after each top-level statement
                } catch (ParseError e) {
                    System.err.println(e.getMessage());
                    synchronize();
                }
            }
            return new BlockExpression(programStatements);
        } catch (ParseError e) {
            System.err.println(e.getMessage());
            synchronize();
            return null;
        }
    }


    private Expression expression() {
        return assignment();
    }

    private Expression assignment() {
        Expression expr = sequence();

        if (match(RLexer3.TokenType.ASSIGN_LEFT, RLexer3.TokenType.ASSIGN_EQUAL, RLexer3.TokenType.ASSIGN_RIGHT)) {
            RLexer3.Token operator = previous();
            Expression value = expression();
            return new Assignment(expr, operator, value);
        }

        return expr;
    }

    private Expression additive() {
        Expression expr = multiplicative();

        while (match(RLexer3.TokenType.PLUS, RLexer3.TokenType.MINUS)) {
            RLexer3.Token operator = previous();
            Expression right = multiplicative();
            expr = new BinaryExpression(expr, operator, right);
        }

        return expr;
    }

    private Expression postfix(Expression expr) {
        while (true) {
            if (match(RLexer3.TokenType.MEMBER)) {
                RLexer3.Token member = consume(RLexer3.TokenType.IDENTIFIER, "Expect member name after '$'.");
                expr = new MemberAccess(expr, member);
            } else if (match(RLexer3.TokenType.LEFT_PAREN)) {
                List<Expression> args = new ArrayList<>();
                if (!check(RLexer3.TokenType.RIGHT_PAREN)) {
                    args.add(expression()); // First argument
                    while (match(RLexer3.TokenType.COMMA)) {
                        args.add(expression()); // Subsequent arguments
                    }
                }
                consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
                expr = new FunctionCall(expr, args);
            } else if (match(RLexer3.TokenType.LEFT_BRACKET)) { // For single bracket '['
                // Check if it's actually a double bracket if your lexer doesn't distinguish
                if (check(RLexer3.TokenType.LEFT_BRACKET)) { // If next is also '['
                    advance(); // Consume the second '['
                    Expression indexExpr = expression(); // Parse index for '[[]]'
                    // Expect two RIGHT_BRACKETs for ']]'
                    consume(RLexer3.TokenType.RIGHT_BRACKET, "Expect ']' for inner ']]'.");
                    consume(RLexer3.TokenType.RIGHT_BRACKET, "Expect ']' for outer ']]'.");
                    expr = new IndexExpression(expr, indexExpr, true); // isDoubleBracket = true
                } else { // Single bracket
                    Expression indexExpr = expression(); // Parse index for '[]'
                    consume(RLexer3.TokenType.RIGHT_BRACKET, "Expect ']' after index.");
                    expr = new IndexExpression(expr, indexExpr, false); // isDoubleBracket = false
                }
            }
            // If your lexer has DBL_LEFT_BRACKET and DBL_RIGHT_BRACKET:
            // else if (match(RLexer3.TokenType.DBL_LEFT_BRACKET)) {
            //     Expression indexExpr = expression();
            //     consume(RLexer3.TokenType.DBL_RIGHT_BRACKET, "Expect ']]' after index.");
            //     expr = new IndexExpression(expr, indexExpr, true);
            // }
            else {
                break;
            }
        }
        return expr;
    }


    private Expression unary() {
        return primary();
    }


    private Expression multiplicative() {
        Expression expr = unary(); // Or whatever your next lower precedence level is
        while (match(RLexer3.TokenType.MULTIPLY, RLexer3.TokenType.DIVIDE, RLexer3.TokenType.MODULO)) { 
            RLexer3.Token operator = previous();
            Expression right = primary(); // Or next lower precedence level
            expr = new BinaryExpression(expr, operator, right);
        }
        return expr;
    }


    private Expression comparison() {
        Expression expr = additive();
        while (match(RLexer3.TokenType.GT, RLexer3.TokenType.LT, RLexer3.TokenType.GE, RLexer3.TokenType.LE, RLexer3.TokenType.EQ, RLexer3.TokenType.NE)) {
            RLexer3.Token operator = previous();
            Expression right = additive();
            expr = new BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    private Expression sequence() {
        Expression expr = comparison();
    
        while (match(RLexer3.TokenType.SEQUENCE)) {
            RLexer3.Token operator = previous();
            Expression right = comparison();
            expr = new BinaryExpression(expr, operator, right);
        }
    
        return expr;
    }

    private Expression statement() {
        if (check(RLexer3.TokenType.IF)) {
            match(RLexer3.TokenType.IF); // Consume IF
            return ifExpression();        // Let ifExpression handle the rest
        }
        if (check(RLexer3.TokenType.WHILE)) {
            match(RLexer3.TokenType.WHILE);
            return whileExpression();
        }
        if (check(RLexer3.TokenType.FOR)) {
            match(RLexer3.TokenType.FOR);
            return forExpression();
        }
        if (check(RLexer3.TokenType.LEFT_BRACE)) {
            // blockExpression will consume the LEFT_BRACE
            return blockExpression();
        }
        if (check(RLexer3.TokenType.RETURN)) { // Make sure RETURN token exists and lexer produces it
            match(RLexer3.TokenType.RETURN);
            RLexer3.Token keyword = previous();
            Expression value = null;
            // Check if there's a value to return, not just 'return;' or 'return }'
            if (!isAtEnd() && !check(RLexer3.TokenType.RIGHT_BRACE) && !check(RLexer3.TokenType.SEMICOLON) ) {
                value = expression();
            }
            return new ReturnStatement(keyword, value);
        }
        // Default: an expression statement (e.g., assignment, function call, bare value)
        Expression exprStmt = expression(); // Parse an expression
        // No explicit semicolon consumption here; top-level parse() or blockExpression() handles it.
        return exprStmt;
    }


    private Expression primary() {
        Expression exprNode;

        if (match(RLexer3.TokenType.TRUE, RLexer3.TokenType.FALSE,
                RLexer3.TokenType.NULL, RLexer3.TokenType.NA,
                RLexer3.TokenType.INF, RLexer3.TokenType.NAN)) {
            exprNode = new Identifier(previous()); // Or dedicated Literal nodes for these
        } else if (match(RLexer3.TokenType.NUMERIC_LITERAL)) {
            exprNode = new NumberLiteral(previous());
        } else if (match(RLexer3.TokenType.STRING_LITERAL)) {
            exprNode = new StringLiteral(previous());
        } else if (match(RLexer3.TokenType.IDENTIFIER)) {
            exprNode = new Identifier(previous());
        } else if (match(RLexer3.TokenType.LEFT_PAREN)) { // Grouping: (expression)
            Expression groupedExpression = expression();
            consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after grouped expression.");
            exprNode = groupedExpression;
        } else if (match(RLexer3.TokenType.FUNCTION)) { // function_definition
            consume(RLexer3.TokenType.LEFT_PAREN, "Expected '(' after 'function'.");
            List<RLexer3.Token> parameters = new ArrayList<>();
            if (!check(RLexer3.TokenType.RIGHT_PAREN)) {
                do {
                    RLexer3.Token param = consume(RLexer3.TokenType.IDENTIFIER, "Expected parameter name.");
                    parameters.add(param);
                } while (match(RLexer3.TokenType.COMMA));
            }
            consume(RLexer3.TokenType.RIGHT_PAREN, "Expected ')' after parameters.");

            BlockExpression bodyNode;
            if (check(RLexer3.TokenType.LEFT_BRACE)) {
                bodyNode = (BlockExpression) blockExpression(); // blockExpression consumes { and }
            } else {
                Expression singleExprBody = expression();
                List<Expression> exprList = new ArrayList<>();
                exprList.add(singleExprBody);
                bodyNode = new BlockExpression(exprList);
            }
            exprNode = new FunctionExpression(parameters, bodyNode);
        } else {
            throw error("Expected primary expression (e.g., number, string, identifier, '(', 'function', TRUE/FALSE/NULL).");
        }
        return postfix(exprNode); // Apply postfix operations
    }



    public class ParseError extends RuntimeException {
        public final RLexer3.Token token;
    
        public ParseError(RLexer3.Token token, String message) {
            super("Parse error at '" + token.value + "' on line " + token.lineNumber + ": " + message);
            this.token = token;
        }
    }
    private Expression ifExpression() {
           // IF token was already consumed by statement()
        consume(RLexer3.TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expression condition = expression(); // Condition is an expression
        consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        Expression thenBranch = statement(); // Then branch is a statement
        Expression elseBranch = null;

        if (match(RLexer3.TokenType.ELSE)) {
            elseBranch = statement(); // Else branch is also a statement
        }
        return new IfExpression(condition, thenBranch, elseBranch);
    }
    private Expression whileExpression() {
        // WHILE token consumed by statement()
        consume(RLexer3.TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expression condition = expression();
        consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after while condition.");
        Expression body = statement(); // Body is a statement
        return new WhileExpression(condition, body);
    }
   private Expression forExpression() {
        // FOR token consumed by statement()
        consume(RLexer3.TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        RLexer3.Token variable = consume(RLexer3.TokenType.IDENTIFIER, "Expect loop variable name.");
        consume(RLexer3.TokenType.IN, "Expect 'in' after loop variable.");
        Expression iterable = expression(); // Iterable is an expression
        consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after for loop iterable.");
        Expression body = statement(); // Body is a statement
        return new ForExpression(variable, iterable, body);
    }

    private Expression blockExpression() {
        consume(RLexer3.TokenType.LEFT_BRACE, "Expect '{' to start a block."); // Consume { here
        List<Expression> statementsInBlock = new ArrayList<>();
        while (!isAtEnd() && !check(RLexer3.TokenType.RIGHT_BRACE)) {
            statementsInBlock.add(statement()); // A block contains statements
            // Optional semicolons within a block. R is flexible.
            // Newlines often suffice. If your parser requires semicolons, this is it.
            match(RLexer3.TokenType.SEMICOLON);
        }
        consume(RLexer3.TokenType.RIGHT_BRACE, "Expect '}' to end a block.");
        return new BlockExpression(statementsInBlock);
    }

    // Utility functions
    private boolean match(RLexer3.TokenType... types) {
        for (RLexer3.TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(RLexer3.TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private RLexer3.Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == RLexer3.TokenType.EOF;
    }

    private RLexer3.Token peek() {
        return tokens.get(current);
    }

    private RLexer3.Token previous() {
        return tokens.get(current - 1);
    }

    private RLexer3.Token consume(RLexer3.TokenType type, String message) {
        if (check(type)) return advance();
        RLexer3.Token current = peek();
        throw new ParseError(current, message + " (Got: " + current.type + ")");
    }

    private RuntimeException error(String message) {
        return new ParseError(peek(), message);    
    }
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == RLexer3.TokenType.SEMICOLON) return;

            switch (peek().type) {
                case IF:
                case WHILE:
                case FOR:
                case FUNCTION:
                case LEFT_BRACE:
                    return;
            }

            advance();
        }
    }
}