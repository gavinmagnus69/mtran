// package pac1;


import java.io.*;
import java.util.*;

public class RLexer3 {

    // Enum to represent different token types
    public enum TokenType {
        // Keywords
        IF, ELSE, WHILE, FOR, IN, REPEAT, BREAK, NEXT, FUNCTION, TRUE, FALSE, NULL, NA, INF, NAN,

        // Operators
        ASSIGN_LEFT, ASSIGN_RIGHT, ASSIGN_EQUAL,  // <-, ->, =
        PLUS, MINUS, MULTIPLY, DIVIDE, POWER, MODULO, // +, -, *, /, ^, %%
        INTEGER_DIVIDE, MATRIX_MULTIPLY, // %/%, %*%
        LT, GT, LE, GE, EQ, NE, // <, >, <=, >=, ==, !=
        LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT, // &&, ||, !
        SEQUENCE, // : (sequence operator)
        NAMESPACE, // :: (namespace operator)
        MEMBER, // $ (member access)
        AT, // @ (slot access)
        TILDE, // ~ (formula)
        QUESTION_MARK, // ? (help)
         

        // Symbols
        LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET,
        COMMA, SEMICOLON, COLON,

        // Literals
        IDENTIFIER, NUMERIC_LITERAL, STRING_LITERAL, COMMENT,

        // Special
        ELLIPSIS, // ... (variable arguments)
        EOF, // End of File

        // Error
        ERROR // For lexical errors
    }

    // Token class to store token information
    public static class Token {
        public TokenType type;
        public String value;
        public int lineNumber;
        public int columnNumber;
        public int id; // Unique ID for each token
        private static int nextId = 1; // Static counter for generating IDs

        public Token(TokenType type, String value, int lineNumber, int columnNumber) {
            this.type = type;
            this.value = value;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.id = nextId++; // Assign and increment the ID
        }

        @Override
        public String toString() {
            return String.format("(%s, %s, id=%d, line=%d, col=%d)", type, value, id, lineNumber, columnNumber);
        }
    }

    // List to store the generated tokens
    private List<Token> tokens = new ArrayList<>();
    private String[] lines; // Array of input lines
    private int lineNumber = 0; // Current line number (0-based index for array access)
    private int columnNumber = 1; // Current column number (1-based)

    public RLexer3(String input) {
        // Split input into lines, preserving line breaks
        this.lines = input.split("\n", -1); // -1 to keep trailing empty lines
    }

    // Main method to perform lexical analysis
    public List<Token> lex() {
        while (lineNumber < lines.length) {
            String line = lines[lineNumber];
            processLine(line);
            lineNumber++;
            columnNumber = 1; // Reset column number for the next line
        }
        addToken(TokenType.EOF, ""); // Add EOF token
        return tokens;
    }

    // Process a single line of input
    private void processLine(String line) {
        int currentPosition = 0;

        while (currentPosition < line.length()) {
            char currentChar = line.charAt(currentPosition);

            if (Character.isWhitespace(currentChar)) {
                currentPosition++;
                columnNumber++;
            } else if (currentChar == '#') {
                processComment(line, currentPosition);
                break; // Comments extend to the end of the line
            } else if (Character.isLetter(currentChar) || currentChar == '.') {
                currentPosition = processIdentifierOrKeyword(line, currentPosition);
            } else if (Character.isDigit(currentChar)) {
                currentPosition = processNumericLiteral(line, currentPosition);
            } else if (currentChar == '"' || currentChar == '\'') {
                currentPosition = processStringLiteral(line, currentPosition);
            } else if (currentChar == '<' && currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == '-') {
                addToken(TokenType.ASSIGN_LEFT, "<-");
                currentPosition += 2;
                columnNumber += 2;
            } else if (currentChar == '-' && currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == '>') {
                addToken(TokenType.ASSIGN_RIGHT, "->");
                currentPosition += 2;
                columnNumber += 2;
            } else {
                currentPosition = processOperatorOrSymbol(line, currentPosition);
            }
        }
    }

    private void processComment(String line, int start) {
        String comment = line.substring(start);
        addToken(TokenType.COMMENT, comment);
    }

    private int processIdentifierOrKeyword(String line, int start) {
        int currentPosition = start;
        while (currentPosition < line.length() && (Character.isLetterOrDigit(line.charAt(currentPosition)) || line.charAt(currentPosition) == '.' || line.charAt(currentPosition) == '_')) {
            currentPosition++;
        }
        String identifier = line.substring(start, currentPosition);
        columnNumber += identifier.length();

        // Check if it's a keyword
        switch (identifier) {
            case "if": addToken(TokenType.IF, identifier); break;
            case "else": addToken(TokenType.ELSE, identifier); break;
            case "while": addToken(TokenType.WHILE, identifier); break;
            case "for": addToken(TokenType.FOR, identifier); break;
            case "in": addToken(TokenType.IN, identifier); break;
            case "repeat": addToken(TokenType.REPEAT, identifier); break;
            case "break": addToken(TokenType.BREAK, identifier); break;
            case "next": addToken(TokenType.NEXT, identifier); break;
            case "function": addToken(TokenType.FUNCTION, identifier); break;
            case "TRUE": addToken(TokenType.TRUE, identifier); break;
            case "FALSE": addToken(TokenType.FALSE, identifier); break;
            case "NULL": addToken(TokenType.NULL, identifier); break;
            case "NA": addToken(TokenType.NA, identifier); break;
            case "Inf": addToken(TokenType.INF, identifier); break;
            case "NaN": addToken(TokenType.NAN, identifier); break;
            default: addToken(TokenType.IDENTIFIER, identifier);
        }
        return currentPosition;
    }

    private int processNumericLiteral(String line, int start) {
        int currentPosition = start;
        boolean hasDecimal = false;
        boolean hasExponent = false;

        while (currentPosition < line.length()) {
            char currentChar = line.charAt(currentPosition);
            if (Character.isDigit(currentChar)) {
                currentPosition++;
            } else if (currentChar == '.' && !hasDecimal) {
                hasDecimal = true;
                currentPosition++;
            } else if ((currentChar == 'e' || currentChar == 'E') && !hasExponent) {
                hasExponent = true;
                currentPosition++;
                if (currentPosition < line.length() && (line.charAt(currentPosition) == '+' || line.charAt(currentPosition) == '-')) {
                    currentPosition++;
                }
            } else {
                break;
            }
        }

        String numericLiteral = line.substring(start, currentPosition);
        columnNumber += numericLiteral.length();

        // Validate numeric literal
        if (hasExponent && numericLiteral.matches(".*[eE][+-]?$")) {
            addErrorToken("Invalid numeric literal (incomplete exponent): " + numericLiteral);
        } else {
            addToken(TokenType.NUMERIC_LITERAL, numericLiteral);
        }
        return currentPosition;
    }

    private int processStringLiteral(String line, int start) {
        char quoteType = line.charAt(start);
        int currentPosition = start + 1; // Skip the opening quote
        columnNumber++;
        int stringStart = currentPosition;
        while (currentPosition < line.length() && line.charAt(currentPosition) != quoteType) {
            if (line.charAt(currentPosition) == '\\') { // Handle escape sequences
                currentPosition += 2; // Skip escaped character
                columnNumber += 2;
            } else {
                currentPosition++;
                columnNumber++;
            }
        }

        if (currentPosition < line.length() && line.charAt(currentPosition) == quoteType) {
            String stringLiteral = line.substring(stringStart, currentPosition);
            columnNumber++;
            addToken(TokenType.STRING_LITERAL, stringLiteral);
            currentPosition++; // Skip the closing quote
        } else {
            String unterminatedString = line.substring(stringStart, line.length());
            addErrorToken("Unterminated string literal: " + quoteType + unterminatedString);
        }
        return currentPosition;
    }

    private int processOperatorOrSymbol(String line, int start) {
        int currentPosition = start;
        char currentChar = line.charAt(currentPosition);
        switch (currentChar) {
            case '=':
                if (currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == '=') {
                    addToken(TokenType.EQ, "==");
                    currentPosition += 2;
                    columnNumber += 2;
                } else {
                    addToken(TokenType.ASSIGN_EQUAL, "=");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '+': addToken(TokenType.PLUS, "+"); currentPosition++; columnNumber++; break;
            case '-': addToken(TokenType.MINUS, "-"); currentPosition++; columnNumber++; break;
            case '*': addToken(TokenType.MULTIPLY, "*"); currentPosition++; columnNumber++; break;
            case '/': addToken(TokenType.DIVIDE, "/"); currentPosition++; columnNumber++; break;
            case '^': addToken(TokenType.POWER, "^"); currentPosition++; columnNumber++; break;
            case '%':
                if (currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == '%') {
                    currentPosition += 2;
                    columnNumber += 2;

                    int modStart = currentPosition;
                    while (currentPosition < line.length() && line.charAt(currentPosition) != '%') {
                        currentPosition++;
                        columnNumber++;
                    }
                    if (currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == '%') {
                        String modString = line.substring(modStart, currentPosition);
                        switch (modString) {
                            case "": addToken(TokenType.MODULO, "%%"); break;
                            case "/": addToken(TokenType.INTEGER_DIVIDE, "%/%"); break;
                            case "*": addToken(TokenType.MATRIX_MULTIPLY, "%*%"); break;
                            default:
                                addErrorToken("Invalid modulo operator: %" + modString + "%");
                        }
                        currentPosition += 2;
                        columnNumber += 2;
                    } else {
                        addErrorToken("Unterminated modulo operator: %%" + line.substring(modStart, line.length()));
                    }
                } else {
                    addErrorToken("Invalid operator: %");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '<':
                if (currentPosition + 1 < line.length()) {
                    char nextChar = line.charAt(currentPosition + 1);
                    if (nextChar == '=') {
                        addToken(TokenType.LE, "<=");
                        currentPosition += 2;
                        columnNumber += 2;
                    } else if (nextChar == '-') {
                        addToken(TokenType.ASSIGN_LEFT, "<-");
                        currentPosition += 2;
                        columnNumber += 2;
                    } else {
                        addToken(TokenType.LT, "<");
                        currentPosition++;
                        columnNumber++;
                    }
                } else {
                    addToken(TokenType.LT, "<");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '>':
                if (currentPosition + 1 < line.length()) {
                    char nextChar = line.charAt(currentPosition + 1);
                    if (nextChar == '=') {
                        addToken(TokenType.GE, ">=");
                        currentPosition += 2;
                        columnNumber += 2;
                    } else {
                        addToken(TokenType.GT, ">");
                        currentPosition++;
                        columnNumber++;
                    }
                } else {
                    addToken(TokenType.GT, ">");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '!':
                if (currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == '=') {
                    addToken(TokenType.NE, "!=");
                    currentPosition += 2;
                    columnNumber += 2;
                } else {
                    addToken(TokenType.LOGICAL_NOT, "!");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '&':
                if (currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == '&') {
                    addToken(TokenType.LOGICAL_AND, "&&");
                    currentPosition += 2;
                    columnNumber += 2;
                } else {
                    addErrorToken("Single & is not supported in R");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '|':
                if (currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == '|') {
                    addToken(TokenType.LOGICAL_OR, "||");
                    currentPosition += 2;
                    columnNumber += 2;
                } else {
                    addErrorToken("Single | is not supported in R");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '(':
                addToken(TokenType.LEFT_PAREN, "(");
                currentPosition++;
                columnNumber++;
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN, ")");
                currentPosition++;
                columnNumber++;
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE, "{");
                currentPosition++;
                columnNumber++;
                break;
            case '}':
                addToken(TokenType.RIGHT_BRACE, "}");
                currentPosition++;
                columnNumber++;
                break;
            case '[':
                addToken(TokenType.LEFT_BRACKET, "[");
                currentPosition++;
                columnNumber++;
                break;
            case ']':
                addToken(TokenType.RIGHT_BRACKET, "]");
                currentPosition++;
                columnNumber++;
                break;
            case ',':
                addToken(TokenType.COMMA, ",");
                currentPosition++;
                columnNumber++;
                break;
            case ';':
                addToken(TokenType.SEMICOLON, ";");
                currentPosition++;
                columnNumber++;
                break;
            case ':':
                if (currentPosition + 1 < line.length() && line.charAt(currentPosition + 1) == ':') {
                    addToken(TokenType.NAMESPACE, "::");
                    currentPosition += 2;
                    columnNumber += 2;
                } else {
                    addToken(TokenType.SEQUENCE, ":");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '$':
                addToken(TokenType.MEMBER, "$");
                currentPosition++;
                columnNumber++;
                break;
            case '@':
                addToken(TokenType.AT, "@");
                currentPosition++;
                columnNumber++;
                break;
            case '~':
                addToken(TokenType.TILDE, "~");
                currentPosition++;
                columnNumber++;
                break;
            case '?':
                addToken(TokenType.QUESTION_MARK, "?");
                currentPosition++;
                columnNumber++;
                break;
            case '`':
                currentPosition = processBackquotedIdentifier(line, currentPosition);
                break;
            case '.':
                if (currentPosition + 2 < line.length() && line.charAt(currentPosition + 1) == '.' && line.charAt(currentPosition + 2) == '.') {
                    addToken(TokenType.ELLIPSIS, "...");
                    currentPosition += 3;
                    columnNumber += 3;
                } else {
                    currentPosition = processIdentifierOrKeyword(line, currentPosition);
                }
                break;
            default:
                addErrorToken("Unexpected character: " + currentChar);
                currentPosition++;
                columnNumber++;
        }
        return currentPosition;
    }

    private int processBackquotedIdentifier(String line, int start) {
        int currentPosition = start + 1; // Skip opening backquote
        columnNumber++;
        int identStart = currentPosition;
        while (currentPosition < line.length() && line.charAt(currentPosition) != '`') {
            currentPosition++;
            columnNumber++;
        }
        if (currentPosition < line.length() && line.charAt(currentPosition) == '`') {
            String identifier = line.substring(identStart, currentPosition);
            addToken(TokenType.IDENTIFIER, "`" + identifier + "`");
            currentPosition++; // Skip closing backquote
            columnNumber++;
        } else {
            addErrorToken("Unterminated backquoted identifier: `" + line.substring(identStart, line.length()));
        }
        return currentPosition;
    }

    private void addToken(TokenType type, String value) {
        // Adjust lineNumber to be 1-based for reporting
        tokens.add(new Token(type, value, lineNumber + 1, columnNumber));
    }

    private void addErrorToken(String message) {
        // Adjust lineNumber to be 1-based for reporting
        tokens.add(new Token(TokenType.ERROR, message, lineNumber + 1, columnNumber));
    }

    // Method to print tokens grouped by category
    public void printGroupedTokens() {
        Map<String, List<Token>> groupedTokens = new TreeMap<>();

        // Initialize groups
        groupedTokens.put("Keywords", new ArrayList<>());
        groupedTokens.put("Operators", new ArrayList<>());
        groupedTokens.put("Symbols", new ArrayList<>());
        groupedTokens.put("Literals", new ArrayList<>());
        groupedTokens.put("Comments", new ArrayList<>());
        groupedTokens.put("Special", new ArrayList<>());
        groupedTokens.put("Errors", new ArrayList<>());

        // Categorize tokens
        for (Token token : tokens) {
            switch (token.type) {
                case IF: case ELSE: case WHILE: case FOR: case IN: case REPEAT: case BREAK: case NEXT:
                case FUNCTION: case TRUE: case FALSE: case NULL: case NA: case INF: case NAN:
                    groupedTokens.get("Keywords").add(token);
                    break;
                case ASSIGN_LEFT: case ASSIGN_RIGHT: case ASSIGN_EQUAL: case PLUS: case MINUS: case MULTIPLY:
                case DIVIDE: case POWER: case MODULO: case INTEGER_DIVIDE: case MATRIX_MULTIPLY: case LT: case GT: case LE: case GE: case EQ: case NE: case LOGICAL_AND:
                case LOGICAL_OR: case LOGICAL_NOT: case SEQUENCE: case NAMESPACE: case MEMBER: case AT:
                case TILDE: case QUESTION_MARK:
                    groupedTokens.get("Operators").add(token);
                    break;
                case LEFT_PAREN: case RIGHT_PAREN: case LEFT_BRACE: case RIGHT_BRACE: case LEFT_BRACKET:
                case RIGHT_BRACKET: case COMMA: case SEMICOLON: case COLON:
                    groupedTokens.get("Symbols").add(token);
                    break;
                case IDENTIFIER: case NUMERIC_LITERAL: case STRING_LITERAL:
                    groupedTokens.get("Literals").add(token);
                    break;
                case COMMENT:
                    groupedTokens.get("Comments").add(token);
                    break;
                case ELLIPSIS: case EOF:
                    groupedTokens.get("Special").add(token);
                    break;
                case ERROR:
                    groupedTokens.get("Errors").add(token);
                    break;
            }
        }

        // Print grouped tokens
        for (Map.Entry<String, List<Token>> entry : groupedTokens.entrySet()) {
            String groupName = entry.getKey();
            List<Token> groupTokens = entry.getValue();
            if (!groupTokens.isEmpty()) {
                System.out.println("\n=== " + groupName + " ===");
                for (Token token : groupTokens) {
                    System.out.println(token);
                }
            }
        }
    }



    public static String readFile(String filePath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                contentBuilder.append(currentLine).append("\n");
            }
        }
        return contentBuilder.toString();
    }

    public static void printTokenLineByLine(List<Token> tokens) {
        for(Token token : tokens){
            System.out.println(token);
        }
    }


    public static class ASTNode {
        public final String type;
        public final String value;
        public final List<ASTNode> children;
    
        public ASTNode(String type, String value) {
            this.type = type;
            this.value = value;
            this.children = new ArrayList<>();
        }
    
        public void addChild(ASTNode child) {
            children.add(child);
        }
    
        @Override
        public String toString() {
            return toString(0);
        }
    
        private String toString(int indent) {
            StringBuilder sb = new StringBuilder();
            sb.append(" ".repeat(indent)).append(type);
            if (value != null) {
                sb.append(": ").append(value);
            }
            sb.append("\n");
            for (ASTNode child : children) {
                sb.append(child.toString(indent + 2));
            }
            return sb.toString();
        }
    }


    public static class Parser {
        private final List<Token> tokens;
        private int position;
    
        public Parser(List<Token> tokens) {
            this.tokens = tokens;
            this.position = 0;
        }
    
        public ASTNode parse() {
            ASTNode root = new ASTNode("Program", null);
            while (position < tokens.size() && tokens.get(position).type != TokenType.EOF) {
                root.addChild(parseStatement());
            }
            return root;
        }
    
        private ASTNode parseStatement() {
            Token currentToken = tokens.get(position);
            switch (currentToken.type) {
                case IF:
                    return parseIfStatement();
                case WHILE:
                    return parseWhileStatement();
                case FOR:
                    return parseForStatement();
                case FUNCTION:
                    return parseFunctionDeclaration();
                case IDENTIFIER:
                    return parseAssignment();
                default:
                    return parseExpression();
            }
        }
    
        private ASTNode parseIfStatement() {
            ASTNode node = new ASTNode("IfStatement", null);
            match(TokenType.IF);
            match(TokenType.LEFT_PAREN);
            node.addChild(parseExpression());
            match(TokenType.RIGHT_PAREN);
            node.addChild(parseBlock());
            if (tokens.get(position).type == TokenType.ELSE) {
                match(TokenType.ELSE);
                node.addChild(parseBlock());
            }
            return node;
        }
    
        private ASTNode parseWhileStatement() {
            ASTNode node = new ASTNode("WhileStatement", null);
            match(TokenType.WHILE);
            match(TokenType.LEFT_PAREN);
            node.addChild(parseExpression());
            match(TokenType.RIGHT_PAREN);
            node.addChild(parseBlock());
            return node;
        }
    
        private ASTNode parseForStatement() {
            ASTNode node = new ASTNode("ForStatement", null);
            match(TokenType.FOR);
            match(TokenType.LEFT_PAREN);
            node.addChild(parseAssignment());
            match(TokenType.SEMICOLON);
            node.addChild(parseExpression());
            match(TokenType.SEMICOLON);
            node.addChild(parseAssignment());
            match(TokenType.RIGHT_PAREN);
            node.addChild(parseBlock());
            return node;
        }
    
        private ASTNode parseFunctionDeclaration() {
            ASTNode node = new ASTNode("FunctionDeclaration", null);
            match(TokenType.FUNCTION);
            Token identifier = tokens.get(position++);
            node.addChild(new ASTNode("Identifier", identifier.value));
            match(TokenType.LEFT_PAREN);
            while (tokens.get(position).type != TokenType.RIGHT_PAREN) {
                node.addChild(new ASTNode("Parameter", tokens.get(position++).value));
                if (tokens.get(position).type == TokenType.COMMA) {
                    match(TokenType.COMMA);
                }
            }
            match(TokenType.RIGHT_PAREN);
            node.addChild(parseBlock());
            return node;
        }
    
        private ASTNode parseAssignment() {
            ASTNode node = new ASTNode("Assignment", null);
            Token identifier = tokens.get(position++);
            node.addChild(new ASTNode("Identifier", identifier.value));
            match(TokenType.ASSIGN_EQUAL);
            node.addChild(parseExpression());
            return node;
        }
    
        private ASTNode parseExpression() {
            ASTNode node = new ASTNode("Expression", null);
            node.addChild(parseLogicalOr());
            return node;
        }
    
        private ASTNode parseLogicalOr() {
            ASTNode node = new ASTNode("LogicalOr", null);
            node.addChild(parseLogicalAnd());
            while (position < tokens.size() && tokens.get(position).type == TokenType.LOGICAL_OR) {
                Token operator = tokens.get(position++);
                node.addChild(new ASTNode("Operator", operator.value));
                node.addChild(parseLogicalAnd());
            }
            return node;
        }
    
        private ASTNode parseLogicalAnd() {
            ASTNode node = new ASTNode("LogicalAnd", null);
            node.addChild(parseEquality());
            while (position < tokens.size() && tokens.get(position).type == TokenType.LOGICAL_AND) {
                Token operator = tokens.get(position++);
                node.addChild(new ASTNode("Operator", operator.value));
                node.addChild(parseEquality());
            }
            return node;
        }
    
        private ASTNode parseEquality() {
            ASTNode node = new ASTNode("Equality", null);
            node.addChild(parseRelational());
            while (position < tokens.size() && (tokens.get(position).type == TokenType.EQ || tokens.get(position).type == TokenType.NE)) {
                Token operator = tokens.get(position++);
                node.addChild(new ASTNode("Operator", operator.value));
                node.addChild(parseRelational());
            }
            return node;
        }
    
        private ASTNode parseRelational() {
            ASTNode node = new ASTNode("Relational", null);
            node.addChild(parseAdditive());
            while (position < tokens.size() && (tokens.get(position).type == TokenType.LT || tokens.get(position).type == TokenType.GT || tokens.get(position).type == TokenType.LE || tokens.get(position).type == TokenType.GE)) {
                Token operator = tokens.get(position++);
                node.addChild(new ASTNode("Operator", operator.value));
                node.addChild(parseAdditive());
            }
            return node;
        }
    
        private ASTNode parseAdditive() {
            ASTNode node = new ASTNode("Additive", null);
            node.addChild(parseMultiplicative());
            while (position < tokens.size() && (tokens.get(position).type == TokenType.PLUS || tokens.get(position).type == TokenType.MINUS)) {
                Token operator = tokens.get(position++);
                node.addChild(new ASTNode("Operator", operator.value));
                node.addChild(parseMultiplicative());
            }
            return node;
        }
    
        private ASTNode parseMultiplicative() {
            ASTNode node = new ASTNode("Multiplicative", null);
            node.addChild(parseUnary());
            while (position < tokens.size() && (tokens.get(position).type == TokenType.MULTIPLY || tokens.get(position).type == TokenType.DIVIDE || tokens.get(position).type == TokenType.MODULO || tokens.get(position).type == TokenType.INTEGER_DIVIDE || tokens.get(position).type == TokenType.MATRIX_MULTIPLY)) {
                Token operator = tokens.get(position++);
                node.addChild(new ASTNode("Operator", operator.value));
                node.addChild(parseUnary());
            }
            return node;
        }
    
        private ASTNode parseUnary() {
            if (position < tokens.size() && (tokens.get(position).type == TokenType.PLUS || tokens.get(position).type == TokenType.MINUS || tokens.get(position).type == TokenType.LOGICAL_NOT)) {
                Token operator = tokens.get(position++);
                ASTNode node = new ASTNode("Unary", null);
                node.addChild(new ASTNode("Operator", operator.value));
                node.addChild(parseUnary());
                return node;
            } else {
                return parsePrimary();
            }
        }
    
        private ASTNode parsePrimary() {
            Token currentToken = tokens.get(position++);
            switch (currentToken.type) {
                case IDENTIFIER:
                    return new ASTNode("Identifier", currentToken.value);
                case NUMERIC_LITERAL:
                    return new ASTNode("NumericLiteral", currentToken.value);
                case STRING_LITERAL:
                    return new ASTNode("StringLiteral", currentToken.value);
                case TRUE:
                case FALSE:
                case NULL:
                case NA:
                case INF:
                case NAN:
                    return new ASTNode("Literal", currentToken.value);
                case LEFT_PAREN:
                    ASTNode node = parseExpression();
                    match(TokenType.RIGHT_PAREN);
                    return node;
                default:
                    throw new IllegalArgumentException("Unexpected token: " + currentToken);
            }
        }
    
        private ASTNode parseBlock() {
            ASTNode node = new ASTNode("Block", null);
            match(TokenType.LEFT_BRACE);
            while (position < tokens.size() && tokens.get(position).type != TokenType.RIGHT_BRACE) {
                node.addChild(parseStatement());
            }
            match(TokenType.RIGHT_BRACE);
            return node;
        }
    
        private void match(TokenType expectedType) {
            if (tokens.get(position).type != expectedType) {
                throw new IllegalArgumentException("Expected token: " + expectedType + ", but found: " + tokens.get(position).type);
            }
            position++;
        }
    }

    
    // Main method for testing 
    // public static void main(String[] args) {
    //     // Example R code with various elements, including potential errors
    //     try {
    //         String rCode = readFile("./lex.r");
    //         RLexer3 lexer = new RLexer3(rCode);
    //         List<Token> tokens = lexer.lex();
    //         printTokenLineByLine(tokens);
    //         lexer.printGroupedTokens();


    //         Parser parser = new Parser(tokens);
    //         ASTNode ast = parser.parse();

    //         System.out.println(ast);
    //     } catch (IOException e) {
    //         System.err.println("Error during lexical analysis: " + e.getMessage());
    //         e.printStackTrace(); // Print the stack trace for debugging
    //     }
    // }
}