# Пример работы с различными типами переменных
x <- 10
y <- 3
z <- 1.0 + 2.0
name <- "Alice"
flag <- TRUE

# Обычная функция
square <- function(x) {
  return(x * x)
}

# Рекурсивная функция для вычисления факториала
factorial <- function(n) {
  if (n <= 1) {
	return(1)
  } else {
	return(n * factorial(n - 1))
  }
}

# Использование цикла for
printSquares <- function(n) {
  for (i in 1:n) {
	cat("Square of", i, ":", square(i), "\n")
  }
}

# Основная функция
main <- function() {
  cat("Hello,", name,  ".\n")
  cat("Factorial of 5:", factorial(5), "\n")
  cat("Squares from 1 to 10:\n")
  printSquares(10)
}

# Вызов основной функции
main()


sumCoordinates <- function(x, y) {
  return(x + y)
}

main <- function() {
  # Пример работы с кортежами
  tupleExample <- list(10, 20)
  cat("Sum of tuple elements:", sumCoordinates(tupleExample[[1]], tupleExample[[2]]), "\n")

  # Пример работы с массивами
  arrayExample <- sapply(0:4, function(i) i * i)
  cat("Squares in array:", paste(arrayExample, collapse = ", "), "\n")
}

# Вызов основной функции
main()

# Определение функции для фильтрации множества
isEven <- function(x) {
  return(x %% 2 == 0)
}

# Работа с множествами
numbers <- as.numeric(1:20)
evenNumbers <- numbers[sapply(numbers, isEven)]

# Использование условных операторов
classifyNumber <- function(x) {
  if (x %% 2 == 0) {
	  return("Even")
  } else {
	  return("Odd")
  }
}

# Функция высшего порядка
applyToAll <- function(f, set) {
  return(sapply(set, f))
}

main <- function() {
  cat("Original set:", paste(numbers, collapse = ", "), "\n")
  cat("Filtered even numbers:", paste(evenNumbers, collapse = ", "), "\n")

  cat("Classifying numbers from 1 to 10:\n")
  for (n in 1:10) {
	cat(n, "is", classifyNumber(n), "\n")
  }

  doubledNumbers <- applyToAll(function(x) x * 2, numbers)
  cat("Doubled set:", paste(doubledNumbers, collapse = ", "), "\n")
}

# Вызов основной функции
main()