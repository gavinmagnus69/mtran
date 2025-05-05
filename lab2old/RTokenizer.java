import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RTokenizer {
    // Enum for token types
    public enum TokenType {
        KEYWORD, IDENTIFIER, NUMBER, STRING, OPERATOR, 
        COMMENT, WHITESPACE, PUNCTUATION, UNKNOWN
    }

    // Class to represent a token
    public static class Token {
        public TokenType type;
        public String value;
        public int line;
        public int column;
        public int id = 0;

        public Token(TokenType type, String value, int line, int column) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
        }

        @Override
        public String toString() {
            return String.format("Token(id=%d, type=%s, value='%s')",
                    id, type, value);
        }
    }

    // List of R keywords
    private static final String[] R_KEYWORDS = {
        "if", "else", "for", "while", "repeat", "break", "next",
        "function", "return", "in", "TRUE", "FALSE", "NULL", "NA",
        "Inf", "NaN"
    };

    // Common R operators
    private static final String[] R_OPERATORS = {
        "+", "-", "*", "/", "^", "%%", "%/%", "=", "<-", "<<-",
        "==", "!=", ">", "<", ">=", "<=", "&", "|", "!", "&&", "||",
        "%*%", "%in%"
    };

    // Punctuation
    private static final String[] R_PUNCTUATION = {
        "(", ")", "[", "]", "{", "}", ",", ";", ":"
    };


    List<Token> keywordList = new ArrayList<>();
    List<Token> idList = new ArrayList<>();
    List<Token> numList = new ArrayList<>();
    List<Token> strList = new ArrayList<>();
    List<Token> opList = new ArrayList<>();
    List<Token> comList = new ArrayList<>();
    List<Token> wList = new ArrayList<>();
    List<Token> punList = new ArrayList<>();
    List<Token> unkList = new ArrayList<>();


    public void printByGroups(
        List<Token> tokens
    ) {
        System.out.println("Keywords:");
        for(Token token : keywordList) {
            System.out.println(token);
        }
        System.out.println("Identifiers:");
        for(Token token : idList) {
            System.out.println(token);
        }
        System.out.println("Numeric constants:");
        for(Token token : numList) {
            System.out.println(token);
        }
        System.out.println("Strings:");
        for(Token token : strList) {
            System.out.println(token);
        }
        System.out.println("Operators:");
        for(Token token : opList) {
            System.out.println(token);
        }
        System.out.println("Comments:");
        for(Token token : comList) {
            System.out.println(token);
        }
        System.out.println("Punctuators:");
        for(Token token : punList) {
            System.out.println(token);
        }
        System.out.println("Unknown:");
        for(Token token : unkList) {
            System.out.println(token);
        }
    }

    public void identifyTokens(List<Token> tokens) {
        int cntKey = 0;
        int cntId = 0;
        int cntNum = 0;
        int cntStr = 0;
        int cntOp = 0;
        int cntCom = 0;
        int cntWs = 0;
        int cntPun = 0;
        int cntUnk = 0;
        for( Token token : tokens) {
            if(token.type == TokenType.COMMENT) {
                token.id = cntCom;
                ++cntCom;
                comList.add(token);
            }
            if(token.type == TokenType.IDENTIFIER) {
                token.id = cntId;
                ++cntId;
                idList.add(token);
            }
            if(token.type == TokenType.KEYWORD) {
                token.id = cntKey;
                ++cntKey;
                keywordList.add(token);
            }
            if(token.type == TokenType.NUMBER) {
                token.id = cntNum;
                ++cntNum;
                numList.add(token);
            }
            if(token.type == TokenType.OPERATOR) {
                token.id = cntOp;
                ++cntOp;
                opList.add(token);

            }
            if(token.type == TokenType.PUNCTUATION) {
                token.id = cntPun;
                ++cntPun;
                punList.add(token);

            }
            if(token.type == TokenType.STRING) {
                token.id = cntStr;
                ++cntStr;
                strList.add(token);

            }
            if(token.type == TokenType.UNKNOWN) {
                token.id = cntUnk;
                ++cntUnk;
                unkList.add(token);

            }
            if(token.type == TokenType.WHITESPACE) {
                token.id = cntWs;
                ++cntWs;
                wList.add(token);
            }
        }
    }  


    public List<Token> tokenizeFile(String filePath) throws IOException {
        List<Token> tokens = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            tokens.addAll(tokenizeLine(line, lineNumber));
        }

        reader.close();
        return tokens;
    }

    private List<Token> tokenizeLine(String line, int lineNumber) {
        List<Token> tokens = new ArrayList<>();
        int column = 0;
        String remaining = line;

        while (!remaining.isEmpty()) {
            // Skip whitespace
            Pattern whitespacePattern = Pattern.compile("^\\s+");
            Matcher whitespaceMatcher = whitespacePattern.matcher(remaining);
            if (whitespaceMatcher.find()) {
                String whitespace = whitespaceMatcher.group();
                tokens.add(new Token(TokenType.WHITESPACE, whitespace, lineNumber, column));
                column += whitespace.length();
                remaining = remaining.substring(whitespace.length());
                continue;
            }

            // Check for comments
            if (remaining.startsWith("#")) {
                tokens.add(new Token(TokenType.COMMENT, remaining, lineNumber, column));
                break; // Rest of the line is a comment
            }

            // Check for strings (both single and double quotes)
            Pattern stringPattern = Pattern.compile("^(\"[^\"]*\"|'[^']*')");
            Matcher stringMatcher = stringPattern.matcher(remaining);
            if (stringMatcher.find()) {
                String string = stringMatcher.group();
                tokens.add(new Token(TokenType.STRING, string, lineNumber, column));
                column += string.length();
                remaining = remaining.substring(string.length());
                continue;
            }

            // Check for numbers (including scientific notation)
            Pattern numberPattern = Pattern.compile("^\\d*\\.?\\d+(e[-+]?\\d+)?");
            Matcher numberMatcher = numberPattern.matcher(remaining);
            if (numberMatcher.find()) {
                String number = numberMatcher.group();
                tokens.add(new Token(TokenType.NUMBER, number, lineNumber, column));
                column += number.length();
                remaining = remaining.substring(number.length());
                continue;
            }

            // Check for operators
            boolean foundOperator = false;
            for (String op : R_OPERATORS) {
                if (remaining.startsWith(op)) {
                    tokens.add(new Token(TokenType.OPERATOR, op, lineNumber, column));
                    column += op.length();
                    remaining = remaining.substring(op.length());
                    foundOperator = true;
                    break;
                }
            }
            if (foundOperator) continue;

            // Check for punctuation
            boolean foundPunctuation = false;
            for (String punc : R_PUNCTUATION) {
                if (remaining.startsWith(punc)) {
                    tokens.add(new Token(TokenType.PUNCTUATION, punc, lineNumber, column));
                    column += punc.length();
                    remaining = remaining.substring(punc.length());
                    foundPunctuation = true;
                    break;
                }
            }
            if (foundPunctuation) continue;

            // Check for identifiers and keywords
            Pattern identifierPattern = Pattern.compile("^[a-zA-Z._][a-zA-Z0-9._]*");
            Matcher identifierMatcher = identifierPattern.matcher(remaining);
            if (identifierMatcher.find()) {
                String identifier = identifierMatcher.group();
                TokenType type = TokenType.IDENTIFIER;
                
                // Check if it's a keyword
                for (String keyword : R_KEYWORDS) {
                    if (identifier.equals(keyword)) {
                        type = TokenType.KEYWORD;
                        break;
                    }
                }
                
                tokens.add(new Token(type, identifier, lineNumber, column));
                column += identifier.length();
                remaining = remaining.substring(identifier.length());
                continue;
            }

            // If nothing matches, add as unknown token
            tokens.add(new Token(TokenType.UNKNOWN, remaining.substring(0, 1), lineNumber, column));
            column++;
            remaining = remaining.substring(1);
        }

        return tokens;
    }
    
    public static void checkLexicalErrors(String code) {
        checkUnmatchedParentheses(code);
        checkInvalidIdentifiers(code);
        // Add more checks as needed
    }

    private static void checkUnmatchedParentheses(String code) {
        Stack<Character> stack = new Stack<>();
        for (char ch : code.toCharArray()) {
            if (ch == '(') {
                stack.push(ch);
            } else if (ch == ')') {
                if (stack.isEmpty()) {
                    System.out.println("Error: Unmatched closing parenthesis ')'");
                    return;
                }
                stack.pop();
            }
        }
        if (!stack.isEmpty()) {
            System.out.println("Error: Unmatched opening parenthesis '('");
        }
    }

    private static void checkInvalidIdentifiers(String code) {
        // R identifiers must start with a letter or a dot (not followed by a number)
        // and can contain letters, numbers, dots, and underscores.
        Pattern pattern = Pattern.compile("\\b\\d+\\b|\\s+");
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            String token = matcher.group();
            if (token.trim().matches("^[0-9].*")) { // Starts with number
                System.out.println("Error: Invalid identifier starting with a number: " + token);
            }
        }

        // Check for invalid sequences like two identifiers together without an operator
        pattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_.]*\\s+[a-zA-Z_][a-zA-Z0-9_.]*");
        matcher = pattern.matcher(code);
        while (matcher.find()) {
            System.out.println("Error: Invalid identifier usage without operator: " + matcher.group());
        }
    }
    
    public static void main(String[] args) {
        RTokenizer tokenizer = new RTokenizer();
        String filePath = "file.r"; 
        try {
            List<Token> tokens = tokenizer.tokenizeFile(filePath);
            tokenizer.identifyTokens(tokens);
            tokenizer.printByGroups(tokens);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}