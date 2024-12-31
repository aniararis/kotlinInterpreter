Kotlin Interpreter
=================
Anastasia Khevtsuriani: Team leader, contributor

Elene Molashvili: Contributor

Lizi Zhvania: Contributor

!!!
===
In the Kotlin Interpreter, boolean values are represented numerically:
True is displayed as 1.
False is displayed as 0.

How the Kotlin Interpreter Was Made
===================================

The Kotlin Interpreter was developed using the following steps:

1. Defining Token Types

Token types such as NUMBER, IDENTIFIER, PLUS, MINUS, etc., were defined in an enumeration (TokenType) to represent the building blocks of the language.

2. Building the Lexer

The Lexer class was created to perform lexical analysis. It reads the input source code character by character and groups them into meaningful tokens.

The implementation includes handling of reserved keywords (e.g., if, while, var) and recognizing numbers, identifiers, and symbols.

3. Developing the Parser

The Parser class was implemented to construct a syntax tree from the tokens. It uses recursive descent parsing to process expressions, statements, and control structures.

Support for variable declarations, conditional statements, loops, and block structures was added.

4. Creating the Interpreter

The Interpreter class evaluates the syntax tree and executes the represented code.

It maintains an environment (a map of variable names to their values) and handles operations such as arithmetic, variable assignments, and control flow.

Built-in functions like print are executed to provide output.

5. Implementing the Interactive Menu

A menu system was created in the main method to allow users to execute predefined algorithms interactively.

Each algorithm is represented as a Kotlin-like program that is passed to the interpreter for execution.

6. Error Handling

Errors during lexical analysis, parsing, or execution are captured and reported with meaningful messages to help users debug their code.

7. Modular Design and Testing

The interpreter was designed with modularity in mind, separating concerns into Lexer, Parser, and Interpreter components.

Each module was individually tested to ensure correctness and robustness.


Interactive Menu and Algorithm Selection
========================================

After running the Kotlin Interpreter, you will be presented with an interactive menu featuring a list of algorithms that you can execute.
Each menu item corresponds to a specific algorithm implemented as a Kotlin-like program. The program will prompt you for input values (e.g., the number N) and then execute the algorithm, displaying the result.


Known Issues
============
Due to the limited time available for development, some algorithms may not function as expected:

Algorithm 2 (Factorial of N)

Algorithm 9 (Multiplication Table)

Algorithm 10 (Nth Fibonacci Number)

These issues are recognized and will be addressed in future updates to improve reliability and accuracy.