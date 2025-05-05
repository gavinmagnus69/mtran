import java.io.*;
import java.util.*;

public class RLexer2 {

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
    private int lineNumber = 1;
    private int columnNumber = 1;
    private String input;
    private int currentPosition = 0;

    public RLexer2(String input) {
        this.input = input;
    }

    // Main method to perform lexical analysis
    public List<Token> lex() {
        while (currentPosition < input.length()) {
            char currentChar = input.charAt(currentPosition);

            if (Character.isWhitespace(currentChar)) {
                skipWhitespace();
            } else if (currentChar == '#') {
                processComment();
            } else if (Character.isLetter(currentChar) || currentChar == '.') {
                processIdentifierOrKeyword();
            } else if (Character.isDigit(currentChar)) {
                processNumericLiteral();
            } else if (currentChar == '"' || currentChar == '\'') {
                processStringLiteral();
            } else if (currentChar == '<' && currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '-') {
                addToken(TokenType.ASSIGN_LEFT, "<-");
                currentPosition += 2;
                columnNumber += 2;
            } else if (currentChar == '-' && currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '>') {
                addToken(TokenType.ASSIGN_RIGHT, "->");
                currentPosition += 2;
                columnNumber += 2;
            } else {
                processOperatorOrSymbol();
            }
        }
        addToken(TokenType.EOF, ""); // Add EOF token
        return tokens;
    }

    // Helper methods for processing different parts of the input
    private void skipWhitespace() {
        while (currentPosition < input.length() && Character.isWhitespace(input.charAt(currentPosition))) {
            if (input.charAt(currentPosition) == '\n') {
                lineNumber++;
                columnNumber = 1;
            } else {
                columnNumber++;
            }
            currentPosition++;
        }
    }

    private void processComment() {
        int start = currentPosition;
        while (currentPosition < input.length() && input.charAt(currentPosition) != '\n') {
            currentPosition++;
        }
        String comment = input.substring(start, currentPosition);
        addToken(TokenType.COMMENT, comment);
    }

    private void processIdentifierOrKeyword() {
        int start = currentPosition;
        while (currentPosition < input.length() && (Character.isLetterOrDigit(input.charAt(currentPosition)) || input.charAt(currentPosition) == '.' || input.charAt(currentPosition) == '_')) {
            currentPosition++;
        }
        String identifier = input.substring(start, currentPosition);
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
    }

    private void processNumericLiteral() {
        int start = currentPosition;
        boolean hasDecimal = false;
        boolean hasExponent = false;

        while (currentPosition < input.length()) {
            char currentChar = input.charAt(currentPosition);
            if (Character.isDigit(currentChar)) {
                currentPosition++;
            } else if (currentChar == '.' && !hasDecimal) {
                hasDecimal = true;
                currentPosition++;
            } else if ((currentChar == 'e' || currentChar == 'E') && !hasExponent) {
                hasExponent = true;
                currentPosition++;
                if (currentPosition < input.length() && (input.charAt(currentPosition) == '+' || input.charAt(currentPosition) == '-')) {
                    currentPosition++;
                }
            } else {
                break;
            }
        }

        String numericLiteral = input.substring(start, currentPosition);
        columnNumber += numericLiteral.length();

        // Validate numeric literal
        if (hasExponent && numericLiteral.matches(".*[eE][+-]?$")) {
            addErrorToken("Invalid numeric literal (incomplete exponent): " + numericLiteral);
        } else {
            addToken(TokenType.NUMERIC_LITERAL, numericLiteral);
        }
    }

    private void processStringLiteral() {
        char quoteType = input.charAt(currentPosition);
        currentPosition++; // Skip the opening quote
        columnNumber++;
        int start = currentPosition;
        while (currentPosition < input.length() && input.charAt(currentPosition) != quoteType) {
            if (input.charAt(currentPosition) == '\\') { // Handle escape sequences
                currentPosition += 2; // Skip escaped character
                columnNumber += 2;
            } else {
                currentPosition++;
                columnNumber++;
            }
        }

        if (currentPosition < input.length() && input.charAt(currentPosition) == quoteType) {
            String stringLiteral = input.substring(start, currentPosition);
            columnNumber++;
            addToken(TokenType.STRING_LITERAL, stringLiteral);
            currentPosition++; // Skip the closing quote
        } else {
            String unterminatedString = input.substring(start, currentPosition);
            addErrorToken("Unterminated string literal: " + quoteType + unterminatedString);
            currentPosition = input.length(); // Skip to end to avoid further errors
        }
    }

    private void processOperatorOrSymbol() {
        char currentChar = input.charAt(currentPosition);
        switch (currentChar) {
            case '=':
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '=') {
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
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '%') {
                    currentPosition += 2;
                    columnNumber += 2;

                    int modStart = currentPosition;
                    while (currentPosition < input.length() && input.charAt(currentPosition) != '%') {
                        currentPosition++;
                        columnNumber++;
                    }
                    if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '%') {
                        String modString = input.substring(modStart, currentPosition);
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
                        addErrorToken("Unterminated modulo operator: %%" + input.substring(modStart, currentPosition));
                        currentPosition = input.length(); // Skip to end
                    }
                } else {
                    addErrorToken("Invalid operator: %");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '<':
                if (currentPosition + 1 < input.length()) {
                    char nextChar = input.charAt(currentPosition + 1);
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
                if (currentPosition + 1 < input.length()) {
                    char nextChar = input.charAt(currentPosition + 1);
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
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '=') {
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
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '&') {
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
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '|') {
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
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == ':') {
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
                processBackquotedIdentifier();
                break;
            case '.':
                if (currentPosition + 2 < input.length() && input.charAt(currentPosition + 1) == '.' && input.charAt(currentPosition + 2) == '.') {
                    addToken(TokenType.ELLIPSIS, "...");
                    currentPosition += 3;
                    columnNumber += 3;
                } else {
                    processIdentifierOrKeyword();
                }
                break;
            default:
                addErrorToken("Unexpected character: " + currentChar);
                currentPosition++;
                columnNumber++;
        }
    }

    private void processBackquotedIdentifier() {
        currentPosition++; // Skip opening backquote
        columnNumber++;
        int start = currentPosition;
        while (currentPosition < input.length() && input.charAt(currentPosition) != '`') {
            currentPosition++;
            columnNumber++;
        }
        if (currentPosition < input.length() && input.charAt(currentPosition) == '`') {
            String identifier = input.substring(start, currentPosition);
            addToken(TokenType.IDENTIFIER, "`" + identifier + "`");
            currentPosition++; // Skip closing backquote
            columnNumber++;
        } else {
            addErrorToken("Unterminated backquoted identifier: `" + input.substring(start, currentPosition));
            currentPosition = input.length(); // Skip to end
        }
    }

    private void addToken(TokenType type, String value) {
        tokens.add(new Token(type, value, lineNumber, columnNumber));
    }

    private void addErrorToken(String message) {
        tokens.add(new Token(TokenType.ERROR, message, lineNumber, columnNumber));
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
                case DIVIDE: case POWER: case MODULO: case INTEGER_DIVIDE: case MATRIX_MULTIPLY: case LT:
                case GT: case LE: case GE: case EQ: case NE: case LOGICAL_AND: case LOGICAL_OR: case LOGICAL_NOT:
                case SEQUENCE: case NAMESPACE: case MEMBER: case AT: case TILDE: case QUESTION_MARK:
                    groupedTokens.get("Operators").add(token);
                    break;
                case LEFT_PAREN: case RIGHT_PAREN: case LEFT_BRACE: case RIGHT_BRACE: case LEFT_BRACKET:
                case RIGHT_BRACKET: case COMMA: case SEMICOLON: case COLON:
                    groupedTokens.get("Symbols").add(token);
                    break;
                case IDENTIFIER: case NUMERIC_LITERAL:case STRING_LITERAL:
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
// Main method for testing
public static void main(String[] args) {
    try {
        String rCode = readFile("./code.r");
        RLexer2 lexer = new RLexer2(rCode);
        List<Token> tokens = lexer.lex();
        lexer.printGroupedTokens();
    } catch (IOException e) {
        System.err.println("Error during lexical analysis: " + e.getMessage());
        e.printStackTrace(); // Print the stack trace for debugging
    }
    
}
}