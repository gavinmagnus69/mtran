import java.io.*;
import java.util.*;

public class RLexer {

    // Enum to represent different token types
    public enum TokenType {
        // Keywords
        IF, ELSE, WHILE, FOR, IN, REPEAT, BREAK, NEXT, FUNCTION, TRUE, FALSE, NULL, NA, INF, NAN,

        // Operators
        ASSIGN_LEFT, ASSIGN_RIGHT, ASSIGN_EQUAL,  // <-, ->, =
        LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET,
        COMMA, SEMICOLON, COLON,
        PLUS, MINUS, MULTIPLY, DIVIDE, POWER, MODULO,
        LT, GT, LE, GE, EQ, NE,
        LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT,
        SEQUENCE,  // .. (e.g., 1:5)
        MEMBER,     // $ (e.g., dataframe$column)
        AT,         // @ (used in S4 objects)
        TILDE,      // ~ (used in formulas)
        QUESTION_MARK, // ? (help operator)
        BACKQUOTE,  // ` (used for non-standard names)

        // Literals
        IDENTIFIER,
        NUMERIC_LITERAL,
        STRING_LITERAL,
        COMMENT,

        // Special
        ELLIPSIS,  // ... (used for variable arguments)
        EOF  // End of File
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


    public RLexer(String input) {
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
        addToken(TokenType.COMMENT, comment); // Optional: Keep comments as tokens
        // lineNumber and columnNumber are updated in skipWhitespace
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
            case "if":      addToken(TokenType.IF, identifier); break;
            case "else":    addToken(TokenType.ELSE, identifier); break;
            case "while":   addToken(TokenType.WHILE, identifier); break;
            case "for":     addToken(TokenType.FOR, identifier); break;
            case "in":      addToken(TokenType.IN, identifier); break;
            case "repeat":  addToken(TokenType.REPEAT, identifier); break;
            case "break":   addToken(TokenType.BREAK, identifier); break;
            case "next":    addToken(TokenType.NEXT, identifier); break;
            case "function":addToken(TokenType.FUNCTION, identifier); break;
            case "TRUE":    addToken(TokenType.TRUE, identifier); break;
            case "FALSE":   addToken(TokenType.FALSE, identifier); break;
            case "NULL":    addToken(TokenType.NULL, identifier); break;
            case "NA":      addToken(TokenType.NA, identifier); break;
            case "Inf":     addToken(TokenType.INF, identifier); break;
            case "NaN":     addToken(TokenType.NAN, identifier); break;
            default:        addToken(TokenType.IDENTIFIER, identifier);
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
                // Check for optional + or - after exponent
                if (currentPosition < input.length() && (input.charAt(currentPosition) == '+' || input.charAt(currentPosition) == '-')) {
                    currentPosition++;
                }
            } else {
                break;
            }
        }

        String numericLiteral = input.substring(start, currentPosition);
           columnNumber += numericLiteral.length();
        addToken(TokenType.NUMERIC_LITERAL, numericLiteral);
    }


    private void processStringLiteral() {
      char quoteType = input.charAt(currentPosition);
      currentPosition++; // Skip the opening quote
      columnNumber++;
      int start = currentPosition;
      while (currentPosition < input.length() && input.charAt(currentPosition) != quoteType) {
          if(input.charAt(currentPosition) == '\\') { //handle escape sequences
              currentPosition += 2; //skip escaped character
              columnNumber +=2;
          }
          else {
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
          // Handle unterminated string (error) - in a real lexer, you'd throw an exception
          System.err.println("Unterminated string literal at line " + lineNumber + ", column " + columnNumber);
          // For simplicity, we'll just add the remaining part as a string
          String stringLiteral = input.substring(start);
          addToken(TokenType.STRING_LITERAL, stringLiteral);
          currentPosition = input.length(); // Go to the end
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
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition+1) == '%') {
                    currentPosition += 2;
                    columnNumber += 2;

                    int modStart = currentPosition;
                    while(currentPosition < input.length() && input.charAt(currentPosition) != '%'){
                        currentPosition++;
                        columnNumber++;
                    }
                    if(currentPosition+1 < input.length() && input.charAt(currentPosition+1) == '%'){
                       String modString = input.substring(modStart, currentPosition);
                       switch(modString){
                           case "": addToken(TokenType.MODULO, "%%"); break; //regular modulo
                           case "/": addToken(TokenType.DIVIDE, "%/%"); break;  //integer division
                           case "*": addToken(TokenType.MULTIPLY, "%*%"); break; //matrix multiplication
                           //add more special modulo cases here if needed
                           default:
                               System.err.println("Invalid modulo operator: %" + modString + "% at line " + lineNumber + ", column " + columnNumber);
                               addToken(TokenType.MODULO, "%" + modString + "%"); //add it anyway for now
                       }
                       currentPosition +=2;
                       columnNumber += 2;
                    } else {
                        System.err.println("Unterminated modulo operator at line " + lineNumber + ", column " + columnNumber);
                        currentPosition = input.length(); //go to end of string
                    }


                } else{
                    System.err.println("Invalid operator % at line " + lineNumber + ", column " + columnNumber);
                    currentPosition++;
                    columnNumber++;
                }
                break;

            case '<':
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '=') {
                    addToken(TokenType.LE, "<=");
                    currentPosition += 2;
                    columnNumber += 2;
                } else {
                    addToken(TokenType.LT, "<");
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case '>':
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition + 1) == '=') {
                    addToken(TokenType.GE, ">=");
                    currentPosition += 2;
                    columnNumber += 2;
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
                    // Single & is bitwise AND, not usually used in high-level R code
                    // We'll still tokenize it, but you might want to handle it differently
                    addToken(TokenType.LOGICAL_AND, "&"); //Treat as logical AND for now
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
                   //Similar to single &, single | is bitwise OR
                    addToken(TokenType.LOGICAL_OR, "|"); //Treat as logical or for now
                    currentPosition++;
                    columnNumber++;
                }
                break;
            case ':': addToken(TokenType.COLON, ":"); currentPosition++; columnNumber++; break;
            case ',': addToken(TokenType.COMMA, ","); currentPosition++; columnNumber++; break;
            case ';': addToken(TokenType.SEMICOLON, ";"); currentPosition++; columnNumber++; break;
            case '(': addToken(TokenType.LEFT_PAREN, "("); currentPosition++; columnNumber++; break;
            case ')': addToken(TokenType.RIGHT_PAREN, ")"); currentPosition++; columnNumber++; break;
            case '{': addToken(TokenType.LEFT_BRACE, "{"); currentPosition++; columnNumber++; break;
            case '}': addToken(TokenType.RIGHT_BRACE, "}"); currentPosition++; columnNumber++; break;
            case '[': addToken(TokenType.LEFT_BRACKET, "["); currentPosition++; columnNumber++; break;
            case ']': addToken(TokenType.RIGHT_BRACKET, "]"); currentPosition++; columnNumber++; break;
            case '$': addToken(TokenType.MEMBER, "$"); currentPosition++; columnNumber++; break;
            case '@': addToken(TokenType.AT, "@"); currentPosition++; columnNumber++; break;
            case '~': addToken(TokenType.TILDE, "~"); currentPosition++; columnNumber++; break;
            case '?': addToken(TokenType.QUESTION_MARK, "?"); currentPosition++; columnNumber++; break;
            case '`': addToken(TokenType.BACKQUOTE, "`"); currentPosition++; columnNumber++; break;
            case '.':
                if (currentPosition + 1 < input.length() && input.charAt(currentPosition+1) == '.') {
                    if(currentPosition + 2 < input.length() && input.charAt(currentPosition+2) == '.'){
                        addToken(TokenType.ELLIPSIS, "...");
                        currentPosition += 3;
                        columnNumber += 3;
                    } else {
                        //Could be part of a number, or something invalid.  For now, treat as an invalid token
                        System.err.println("Invalid sequence .. at line " + lineNumber + ", column " + columnNumber);
                        currentPosition++;
                        columnNumber++;
                    }

                } else {
                   //Could be start of an identifier, or something invalid. Let processIdentifierOrKeyword handle it
                   processIdentifierOrKeyword();

                }
                break;

            default:
                // Handle unknown characters (error)
                System.err.println("Unknown character '" + currentChar + "' at line " + lineNumber + ", column " + columnNumber);
                currentPosition++;
                columnNumber++;
        }
    }


    private void addToken(TokenType type, String value) {
        tokens.add(new Token(type, value, lineNumber, columnNumber));
    }


    // Method to group and print tokens
    public void printTokensGrouped() {
        Map<TokenType, List<Token>> groupedTokens = new TreeMap<>(); // Use TreeMap for sorted output

        // Group tokens by type
        for (Token token : tokens) {
            groupedTokens.computeIfAbsent(token.type, k -> new ArrayList<>()).add(token);
        }

        // Print grouped tokens
        for (Map.Entry<TokenType, List<Token>> entry : groupedTokens.entrySet()) {
            System.out.println("Token Type: " + entry.getKey());
            for (Token token : entry.getValue()) {
                System.out.println("\t" + token);}
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

        public static void main(String[] args) {
            try {
                String rCode = readFile("./code.r");
                RLexer lexer = new RLexer(rCode);
                List<Token> tokens = lexer.lex();
    
                System.out.println("All Tokens:");
                // for (Token token : tokens) {
                //     System.out.println(token);
                // }
    
                System.out.println("\nGrouped Tokens:");
                lexer.printTokensGrouped();
            } catch (IOException e) {
                System.err.println("Error during lexical analysis: " + e.getMessage());
                e.printStackTrace(); // Print the stack trace for debugging
            }
        }
    }