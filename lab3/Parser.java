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

    private final List<RLexer3.Token> tokens;
    private int current = 0;

    public Parser(List<RLexer3.Token> tokens) {
        this.tokens = tokens;
    }

    public Expression parse() {
        try {
            return expression();
        } catch (ParseError e) {
        
            System.err.println(e.getMessage());
            synchronize();
            return null;
        }
    }

    private Expression expression() {
        if (match(RLexer3.TokenType.IF)) return ifExpression();
        if (match(RLexer3.TokenType.WHILE)) return whileExpression();
        if (match(RLexer3.TokenType.FOR)) return forExpression();
        if (match(RLexer3.TokenType.LEFT_BRACE)) return blockExpression();

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

    private Expression multiplicative() {
        Expression expr = primary();

        while (match(RLexer3.TokenType.MULTIPLY, RLexer3.TokenType.DIVIDE)) {
            RLexer3.Token operator = previous();
            Expression right = primary();
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

    
    private Expression primary() {
        if (match(RLexer3.TokenType.FUNCTION)) {
            consume(RLexer3.TokenType.LEFT_PAREN, "Expected '(' after 'function'.");
    
            List<RLexer3.Token> parameters = new ArrayList<>();
    
            if (!check(RLexer3.TokenType.RIGHT_PAREN)) {
                do {
                    RLexer3.Token param = consume(RLexer3.TokenType.IDENTIFIER, "Expected parameter name.");
                    parameters.add(param);
                } while (match(RLexer3.TokenType.COMMA));
            }
    
            consume(RLexer3.TokenType.RIGHT_PAREN, "Expected ')' after parameters.");
    
            // The function body should be a block expression
            Expression bodyExpr = expression();
    
            if (!(bodyExpr instanceof BlockExpression)) {
                throw error("Expected block '{...}' as function body.");
            }
    
            return new FunctionExpression(parameters, (BlockExpression) bodyExpr);
        }
        if (match(RLexer3.TokenType.NUMERIC_LITERAL)) {
            return new NumberLiteral(previous());
        }

        if (match(RLexer3.TokenType.STRING_LITERAL)) {
            return new StringLiteral(previous());
        }

        if (match(RLexer3.TokenType.IDENTIFIER)) {
            RLexer3.Token name = previous();

            if (match(RLexer3.TokenType.LEFT_PAREN)) {
                List<Expression> args = new ArrayList<>();
                if (!check(RLexer3.TokenType.RIGHT_PAREN)) {
                    do {
                        args.add(expression());
                    } while (match(RLexer3.TokenType.COMMA));
                }
                consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
                return new FunctionCall(new Identifier(name), args);
            }

            return new Identifier(name);
        }

        if (match(RLexer3.TokenType.LEFT_PAREN)) {
            Expression expr = expression();
            consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return expr;
        }

        throw error("Expected expression.");
    }


    public class ParseError extends RuntimeException {
        public final RLexer3.Token token;
    
        public ParseError(RLexer3.Token token, String message) {
            super("Parse error at '" + token.value + "' on line " + token.lineNumber + ": " + message);
            this.token = token;
        }
    }
    private Expression ifExpression() {
        consume(RLexer3.TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expression condition = expression();
        consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after condition.");

        Expression thenBranch = expression();
        Expression elseBranch = null;

        if (match(RLexer3.TokenType.ELSE)) {
            elseBranch = expression();
        }

        return new IfExpression(condition, thenBranch, elseBranch);
    }
    private Expression whileExpression() {
        consume(RLexer3.TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expression condition = expression();
        consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        Expression body = expression();
        return new WhileExpression(condition, body);
    }
    private Expression forExpression() {
        consume(RLexer3.TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        RLexer3.Token variable = consume(RLexer3.TokenType.IDENTIFIER, "Expect loop variable.");
        consume(RLexer3.TokenType.IN, "Expect 'in' after variable.");
        Expression iterable = expression();
        consume(RLexer3.TokenType.RIGHT_PAREN, "Expect ')' after iterable.");
        Expression body = expression();
        return new ForExpression(variable, iterable, body);
    }
    private Expression blockExpression() {
        List<Expression> expressions = new ArrayList<>();
        while (!check(RLexer3.TokenType.RIGHT_BRACE) && !isAtEnd()) {
            expressions.add(expression());
            match(RLexer3.TokenType.SEMICOLON); // optional
        }
        consume(RLexer3.TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return new BlockExpression(expressions);
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