# Initialize the sum variable
migga <-0 
# Loop through the first 10 natural numbers
for (i in 1:10) {
  # Add the current number to the sum
  sum <- sum + i
}

# Print the result
print(paste("The sum of the first 10 natural numbers is:", sum))

# Conditional statement to check if the sum is greater than 50
if (sum > 50) {
  print("The sum is greater than 50.")
} else {
  print("The sum is not greater than 50.")
}
