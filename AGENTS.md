# AGENTS.md

# AI Coding Instructions

Follow these instructions for every code change unless explicitly told otherwise.

---

# Core Principles

- Keep the code lean, simple, and maintainable.
- Prefer readability over cleverness.
- Match the existing architecture and coding style.
- Write production-quality code.
- Avoid unnecessary abstractions.
- Do not introduce new dependencies unless requested.

---

# Scope of Changes

- Modify only the functions you were instructed to modify.
- Do not change unrelated code.
- Do not rename existing variables, functions, or classes unless explicitly requested.
- Do not move code between files unless required.
- Preserve the existing project structure.

---

# Code Reuse

- Reuse existing functions whenever possible.
- Do not create new helper methods if existing logic can be reused.
- Do not duplicate code.
- Before adding new functionality, check whether an existing method already performs the task.


# Naming

## Variables

Use descriptive names.

Good

```java
remainingRetryAttempts
customerOrderList
totalInvoiceAmount
```

Bad

```java
tmp
cnt
arr2
x
```

Short loop variables (`i`, `j`) are acceptable.

---

## Functions

Use descriptive verb-first names.

Good

```java
calculateTotalPrice()
fetchCustomerById()
createInvoice()
```

Avoid vague names such as

```java
calculate()
process()
run()
doStuff()
```

---

## Classes

Use PascalCase.

Each class should have a single responsibility.

Avoid generic names such as

- Manager
- Helper
- Utils

unless they genuinely represent that responsibility.

---

## Booleans

Boolean variables should begin with

- is
- has
- should
- can
- did

Examples

```java
isValid
hasPermission
shouldRetry
canDelete
```

---

## Constants

Use

```java
UPPER_SNAKE_CASE
```

Example

```java
MAX_CONNECTION_RETRIES
DEFAULT_TIMEOUT_MS
```

---

# Java Style

## Braces

Always use Allman style.

```java
if (condition)
{
    ...
}
else
{
    ...
}
```

---

## Variable declarations

Declare variables close to where they are first used unless grouping them improves readability.

---

## Return Statements

Prefer a single return statement in non-trivial methods.

Guard clauses and early returns are encouraged when they significantly improve readability and reduce nesting.

---

# Functions

Functions should

- perform one responsibility
- remain cohesive
- avoid unnecessary complexity

Prefer modifying existing functions over creating new ones.

---

# Error Handling

- Handle expected failures explicitly.
- Never silently ignore exceptions.
- Preserve stack traces.
- Use meaningful exception messages.

---

# Comments

Do not add comments unless explicitly requested.

Avoid generating

- inline comments
- block comments
- TODO comments
- commented-out code

The code should be self-explanatory.

---


# Minimal Changes

Prefer the smallest correct implementation.

Avoid large refactors when a localized change solves the problem.


---

# Formatting

- Keep lines under 120 characters.
- Use consistent indentation.
- Remove unused imports.
- Remove unused variables.
- Avoid dead code.

---

# AI Behavior

When generating code:

1. Follow this style guide exactly.
2. Match the surrounding project's style.
3. Keep changes as small as possible.
4. Reuse existing functions before creating new ones.
5. Modify only the requested functions.
6. Do not refactor unrelated code.
7. Do not add comments.
8. Do not create helper functions unless absolutely necessary.
9. If requirements are ambiguous, ask for clarification instead of guessing.
10. Explain architectural changes before making them.