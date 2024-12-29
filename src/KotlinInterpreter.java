import java.util.*;
import java.util.Scanner;

// Token types for lexical analysis
enum TokenType {
    NUMBER, IDENTIFIER, PLUS, MINUS, MULTIPLY, DIVIDE, MOD,
    ASSIGN, EQUALS, LESS, GREATER, LPAREN, RPAREN,
    IF, ELSE, WHILE, VAR, PRINT, EOF,
    LBRACE, RBRACE, SEMICOLON
}

class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;

    Token(TokenType type, String lexeme, Object literal) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
    }
}

// Lexer implementation
class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private final Map<String, TokenType> keywords;

    Lexer(String source) {
        this.source = source;
        keywords = new HashMap<>();
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("while", TokenType.WHILE);
        keywords.put("var", TokenType.VAR);
        keywords.put("print", TokenType.PRINT);
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(TokenType.LPAREN);
            case ')' -> addToken(TokenType.RPAREN);
            case '{' -> addToken(TokenType.LBRACE);
            case '}' -> addToken(TokenType.RBRACE);
            case '+' -> addToken(TokenType.PLUS);
            case '-' -> addToken(TokenType.MINUS);
            case '*' -> addToken(TokenType.MULTIPLY);
            case '/' -> addToken(TokenType.DIVIDE);
            case '%' -> addToken(TokenType.MOD);
            case '=' -> addToken(match('=') ? TokenType.EQUALS : TokenType.ASSIGN);
            case '<' -> addToken(TokenType.LESS);
            case '>' -> addToken(TokenType.GREATER);
            case ';' -> addToken(TokenType.SEMICOLON);
            case ' ', '\r', '\t', '\n' -> {}
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new RuntimeException("Unexpected character: " + c);
                }
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal));
    }
}

// Parser implementation
class Parser {
    public static class Expr {
        static class Binary extends Expr {
            final Expr left;
            final Token operator;
            final Expr right;

            Binary(Expr left, Token operator, Expr right) {
                this.left = left;
                this.operator = operator;
                this.right = right;
            }
        }

        static class Literal extends Expr {
            final Object value;

            Literal(Object value) {
                this.value = value;
            }
        }

        static class Variable extends Expr {
            final Token name;

            Variable(Token name) {
                this.name = name;
            }
        }
    }

    static class Stmt {
        static class Expression extends Stmt {
            final Expr expression;

            Expression(Expr expression) {
                this.expression = expression;
            }
        }

        static class Var extends Stmt {
            final Token name;
            final Expr initializer;

            Var(Token name, Expr initializer) {
                this.name = name;
                this.initializer = initializer;
            }
        }

        static class Print extends Stmt {
            final Expr expression;

            Print(Expr expression) {
                this.expression = expression;
            }
        }

        static class If extends Stmt {
            final Expr condition;
            final Stmt thenBranch;
            final Stmt elseBranch;

            If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
                this.condition = condition;
                this.thenBranch = thenBranch;
                this.elseBranch = elseBranch;
            }
        }

        static class While extends Stmt {
            final Expr condition;
            final Stmt body;

            While(Expr condition, Stmt body) {
                this.condition = condition;
                this.body = body;
            }
        }

        static class Block extends Stmt {
            final List<Stmt> statements;

            Block(List<Stmt> statements) {
                this.statements = statements;
            }
        }
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.VAR)) return varDeclaration();
            return statement();
        } catch (RuntimeException error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.LBRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt ifStatement() {
        consume(TokenType.LPAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(TokenType.LPAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RBRACE, "Expect '}' after block.");
        return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = equality();

        if (match(TokenType.ASSIGN)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Binary(new Expr.Variable(name), equals, value);
            }

            throw new RuntimeException("Invalid assignment target.");
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.EQUALS)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(TokenType.LESS, TokenType.GREATER)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = primary();

        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MOD)) {
            Token operator = previous();
            Expr right = primary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr primary() {
        if (match(TokenType.NUMBER)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LPAREN)) {
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return expr;
        }

        throw new RuntimeException("Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException(message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case IF, WHILE, VAR, PRINT -> {
                    return;
                }
            }

            advance();
        }
    }
}

// Interpreter implementation
class Interpreter {
    private final Map<String, Object> environment = new HashMap<>();

    void interpret(List<Parser.Stmt> statements) {
        try {
            for (Parser.Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeException error) {
            throw new RuntimeException("Runtime error: " + error.getMessage());
        }
    }

    private void execute(Parser.Stmt stmt) {
        if (stmt instanceof Parser.Stmt.Expression) {
            evaluate(((Parser.Stmt.Expression) stmt).expression);
        } else if (stmt instanceof Parser.Stmt.Print) {
            Object value = evaluate(((Parser.Stmt.Print) stmt).expression);
            System.out.println(stringify(value));
        } else if (stmt instanceof Parser.Stmt.Var) {
            Parser.Stmt.Var var = (Parser.Stmt.Var) stmt;
            Object value = null;
            if (var.initializer != null) {
                value = evaluate(var.initializer);
            }
            environment.put(var.name.lexeme, value);
        } else if (stmt instanceof Parser.Stmt.Block) {
            executeBlock(((Parser.Stmt.Block) stmt).statements);
        } else if (stmt instanceof Parser.Stmt.If) {
            Parser.Stmt.If ifStmt = (Parser.Stmt.If) stmt;
            if (isTruthy(evaluate(ifStmt.condition))) {
                execute(ifStmt.thenBranch);
            } else if (ifStmt.elseBranch != null) {
                execute(ifStmt.elseBranch);
            }
        } else if (stmt instanceof Parser.Stmt.While) {
            Parser.Stmt.While whileStmt = (Parser.Stmt.While) stmt;
            while (isTruthy(evaluate(whileStmt.condition))) {
                execute(whileStmt.body);
            }
        }
    }

    private void executeBlock(List<Parser.Stmt> statements) {
        for (Parser.Stmt statement : statements) {
            execute(statement);
        }
    }

    private Object evaluate(Parser.Expr expr) {
        if (expr instanceof Parser.Expr.Literal) {
            return ((Parser.Expr.Literal) expr).value;
        } else if (expr instanceof Parser.Expr.Binary) {
            Parser.Expr.Binary binary = (Parser.Expr.Binary) expr;
            Object left = evaluate(binary.left);
            Object right = evaluate(binary.right);

            switch (binary.operator.type) {
                case PLUS -> {
                    checkNumberOperands(binary.operator, left, right);
                    return (double) left + (double) right;
                }
                case MINUS -> {
                    checkNumberOperands(binary.operator, left, right);
                    return (double) left - (double) right;
                }
                case MULTIPLY -> {
                    checkNumberOperands(binary.operator, left, right);
                    return (double) left * (double) right;
                }
                case DIVIDE -> {
                    checkNumberOperands(binary.operator, left, right);
                    if ((double) right == 0) throw new RuntimeException("Division by zero.");
                    return (double) left / (double) right;
                }
                case MOD -> {
                    checkNumberOperands(binary.operator, left, right);
                    if ((double) right == 0) throw new RuntimeException("Modulo by zero.");
                    return (double) left % (double) right;
                }
                case EQUALS -> {
                    return isEqual(left, right);
                }
                case LESS -> {
                    checkNumberOperands(binary.operator, left, right);
                    return (double) left < (double) right;
                }
                case GREATER -> {
                    checkNumberOperands(binary.operator, left, right);
                    return (double) left > (double) right;
                }
                case ASSIGN -> {
                    if (binary.left instanceof Parser.Expr.Variable) {
                        String name = ((Parser.Expr.Variable) binary.left).name.lexeme;
                        environment.put(name, right);
                        return right;
                    }
                    throw new RuntimeException("Invalid assignment target.");
                }
            }
        } else if (expr instanceof Parser.Expr.Variable) {
            return lookupVariable(((Parser.Expr.Variable) expr).name);
        }
        throw new RuntimeException("Unknown expression type.");
    }

    private Object lookupVariable(Token name) {
        if (environment.containsKey(name.lexeme)) {
            return environment.get(name.lexeme);
        }
        throw new RuntimeException("Undefined variable '" + name.lexeme + "'.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeException("Operands must be numbers.");
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private String stringify(Object object) {
        if (object == null) return "null";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}

// Main class to run the interpreter



// All previous code remains the same until the main class
public class KotlinInterpreter {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("\nKotlin Interpreter - Algorithm Menu");
            System.out.println("1. Sum of First N Numbers");
            System.out.println("2. Factorial of N");
            System.out.println("3. GCD of Two Numbers");
            System.out.println("4. Reverse a Number");
            System.out.println("5. Check if a Number is Prime");
            System.out.println("6. Check if a Number is Palindrome");
            System.out.println("7. Find the Largest Digit in a Number");
            System.out.println("8. Sum of Digits");
            System.out.println("9. Multiplication Table");
            System.out.println("10. Nth Fibonacci Number");
            System.out.println("0. Exit");
            System.out.print("\nEnter your choice (0-10): ");

            int choice = scanner.nextInt();
            if (choice == 0) {
                break;
            }

            System.out.print("Enter input number: ");
            int input = scanner.nextInt();

            String program = switch (choice) {
                case 1 -> String.format("""
                    var n = %d;
                    var sum = 0;
                    while (n > 0) {
                        sum = sum + n;
                        n = n - 1;
                    }
                    print sum;
                    """, input);

                case 2 -> String.format("""
                    var n = %d;
                    var result = 1;
                    var i = 1;
                    while (i <= n) {
                        result = result * i;
                        i = i + 1;
                    }
                    print result;
                    """, input);

                case 3 -> {
                    System.out.print("Enter second number: ");
                    int b = scanner.nextInt();
                    yield String.format("""
                        var a = %d;
                        var b = %d;
                        while (b != 0) {
                            var temp = b;
                            b = a %% b;
                            a = temp;
                        }
                        print a;
                        """, input, b);
                }

                case 4 -> String.format("""
                    var num = %d;
                    var reversed = 0;
                    var temp = num;
                    while (temp > 0) {
                        var digit = temp %% 10;
                        reversed = reversed * 10 + digit;
                        temp = (temp - digit) / 10;
                    }
                    print reversed;
                    """, input);

                case 5 -> String.format("""
                    var n = %d;
                    var isPrime = 1;
                    if (n <= 1) {
                        isPrime = 0;
                    }
                    var i = 2;
                    while (i * i <= n) {
                        if ((n %% i) == 0) {
                            isPrime = 0;
                        }
                        i = i + 1;
                    }
                    print isPrime;
                    """, input);

                case 6 -> String.format("""
                    var num = %d;
                    var original = num;
                    var reversed = 0;
                    var temp = num;
                    while (temp > 0) {
                        var digit = temp %% 10;
                        reversed = reversed * 10 + digit;
                        temp = (temp - digit) / 10;
                    }
                    if (original == reversed) {
                        print 1;
                    } else {
                        print 0;
                    }
                    """, input);

                case 7 -> String.format("""
                    var num = %d;
                    var largest = 0;
                    var temp = num;
                    while (temp > 0) {
                        var digit = temp %% 10;
                        if (digit > largest) {
                            largest = digit;
                        }
                        temp = (temp - digit) / 10;
                    }
                    print largest;
                    """, input);

                case 8 -> String.format("""
                    var num = %d;
                    var sum = 0;
                    var temp = num;
                    while (temp > 0) {
                        var digit = temp %% 10;
                        sum = sum + digit;
                        temp = (temp - digit) / 10;
                    }
                    print sum;
                    """, input);

                case 9 -> String.format("""
                    var n = %d;
                    var i = 1;
                    while (i <= 10) {
                        var result = n * i;
                        print result;
                        i = i + 1;
                    }
                    """, input);

                case 10 -> String.format("""
                    var n = %d;
                    if (n <= 1) {
                        print n;
                    } else {
                        var prev = 0;
                        var current = 1;
                        var i = 2;
                        while (i <= n) {
                            var next = prev + current;
                            prev = current;
                            current = next;
                            i = i + 1;
                        }
                        print current;
                    }
                    """, input);

                default -> {
                    System.out.println("Invalid choice!");
                    yield null;
                }
            };

            if (program != null) {
                System.out.println("\nOutput:");
                runProgram(program);
            }

            System.out.println("\nPress Enter to continue...");
            scanner.nextLine(); // Consume the remaining newline
            scanner.nextLine(); // Wait for Enter
        }

        scanner.close();
        System.out.println("Goodbye!");
    }

    public static void runProgram(String source) {
        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();
            Parser parser = new Parser(tokens);
            List<Parser.Stmt> statements = parser.parse();
            Interpreter interpreter = new Interpreter();
            interpreter.interpret(statements);
        } catch (RuntimeException error) {
            System.err.println("Error: " + error.getMessage());
        }
    }
}