outer <- function(a) {
  inner <- function(b) {
    return(a + b)
  }
  return(inner(5))
}
print(outer(10))
x <- 10
if (x > 5) {
  print("greater")
} else {
  print("less or equal")
}
check <- function(x) {
  if (x == 0) {
    return("zero")
  }
  return("non-zero")
}

print(check(0))
print(check(5))
fact <- function(n) {
  if (n == 1) {
    return(1)
  }
  return(n * fact(n - 1))
}
print(fact(5))
square <- function(x) {
  return(x * x)
}

apply <- function(f, v) {
  return(f(v))
}

print(apply(square, 4))