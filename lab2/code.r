# x <- "this string has no quote"
# # Read the CSV file into a data frame
# com <- "not clodes"
# data <- read_csv("data.csv")
# 13.123ee
# %op%
# # Display the first few rows of the data frame
# print(head(data))

# # Summary statistics for numeric columns

# # Print summary statistics
# print(summary_stats)

# # Basic data cleaning: Remove rows with missing values
# # clean_data <- data %>%

# drop_na()

# # Check for any remaining missing values
# print(summary(clean_data))

# # Convert date columns to Date type (if any)
# data$date_column <- ymd(data$date_column)  # Replace 'date_column' with your actual date column name

# # Create a new column with the year extracted from the date column
# # data <- data %>%
#   mutate(year = year(date_column))

# # Basic visualization: Histogram of a numeric column (e.g., age)
# ggplot(data, aes(x = age)) +
#   geom_histogram(binwidth = 5, fill = "blue", color = "black") +
#   labs(title = "Histogram of Age", x = "Age", y = "Frequency")

# # Save the cleaned data to a new CSV file
# write_csv(clean_data, "clean_data.csv")

# # Print a message indicating the process is complete
# print("Data processing and cleaning complete. Cleaned data saved to 'clean_data.csv'.")



# This is a comment
x <- 5
y = 3.14e10
z <- `special name`
if (x > 0) {
  print("Positive")
} else {
  print("Negative")
}
for (i in 1:10) {
  print(i)
}
matrix %*% vector
a $ b
invalid %op% syntax # Error: invalid modulo operator
"unterminated string # Error: unterminated string
1e # Error: incomplete exponent
& single # Error: single & not supported