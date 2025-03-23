# Load necessary libraries
library(tidyverse)  # For data manipulation and visualization
library(lubridate)  # For date manipulation

# Read the CSV file into a data frame
data <- read_csv("data.csv")

# Display the first few rows of the data frame
print(head(data))

# Summary statistics for numeric columns
# summary_stats <- data %>%
  # select(where(is.numeric)) %>%
  summarise_all(list(min = min, first_quartile = ~ quantile(., 0.25),
                     median, mean, third_quartile = ~ quantile(., 0.75),
                     max = max))

# Print summary statistics
print(summary_stats)

# Basic data cleaning: Remove rows with missing values
# clean_data <- data %>%
  drop_na()

# Check for any remaining missing values
print(summary(clean_data))

# Convert date columns to Date type (if any)
data$date_column <- ymd(data$date_column)  # Replace 'date_column' with your actual date column name

# Create a new column with the year extracted from the date column
# data <- data %>%
  mutate(year = year(date_column))

# Basic visualization: Histogram of a numeric column (e.g., age)
ggplot(data, aes(x = age)) +
  geom_histogram(binwidth = 5, fill = "blue", color = "black") +
  labs(title = "Histogram of Age", x = "Age", y = "Frequency")

# Save the cleaned data to a new CSV file
write_csv(clean_data, "clean_data.csv")

# Print a message indicating the process is complete
print("Data processing and cleaning complete. Cleaned data saved to 'clean_data.csv'.")