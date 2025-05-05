import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OperatorMatcher {
    public static void main(String[] args) {
        // Define the regular expression pattern
        String regex = "\\+|-|\\*|/|\\^|%%|&|\\||!|&&|\\|\\||<|<=|>=|!=|<-|<<-|=|->|->>|%in%|%\\*%";

        // Compile the pattern
        Pattern pattern = Pattern.compile(regex);

        // Test string
        String testString = "a + b - c * d / e ^ f %% g & h | i ! j && k || l < m <= n >= o != p <- q <<- r = s -> t ->> u %in% v %*% w";

        // Create a matcher for the test string
        Matcher matcher = pattern.matcher(testString);

        // Find and print all matches
        while (matcher.find()) {
            System.out.println("Found operator: " + matcher.group());
        }
    }
}
