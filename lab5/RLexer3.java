import java.io.*;
import java.util.*;
import java.lang.Thread;

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
        , RETURN
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
        removeCommentTokens();
        return tokens;
    }

    private void removeCommentTokens() {
        List<Token> newTokenList = new ArrayList<>();
        for(Token token : tokens) {
            if(token.type != TokenType.COMMENT) {
                newTokenList.add(token);
            }
        }
        tokens = newTokenList;
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
            case "return": addToken(TokenType.RETURN, identifier); break;
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
    public void PrintLexicalErrors() {
        for(Token token : tokens) {
            if(token.type == TokenType.ERROR) {
                System.out.println(token);
            }
        }
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
                    addToken(TokenType.MODULO, "%%");
                    currentPosition += 2;
                    columnNumber += 2;
                } else if (currentPosition + 2 < line.length() && line.charAt(currentPosition + 1) == '/' && line.charAt(currentPosition + 2) == '%') {
                    addToken(TokenType.INTEGER_DIVIDE, "%/%");
                    currentPosition += 3;
                    columnNumber += 3;
                } else if (currentPosition + 2 < line.length() && line.charAt(currentPosition + 1) == '*' && line.charAt(currentPosition + 2) == '%') {
                    addToken(TokenType.MATRIX_MULTIPLY, "%*%");
                    currentPosition += 3;
                    columnNumber += 3;
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

    public static void checkAllTockens() {
        try {
            ProcessBuilder pb = new ProcessBuilder("Rscript", "test.r");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                Thread.sleep(5);
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            // System.out.println("Exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

}