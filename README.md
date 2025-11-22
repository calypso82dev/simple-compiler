# Simple Compiler

A complete compiler implementation for a simple programming language, written in Java. This project implements a full compilation pipeline from source code to executable code running on a custom 32-bit stack machine.

## Overview

Compiler implements all phases of compilation:

1. **Lexical Analysis** - Tokenization of source code
2. **Syntax Analysis** - Parsing and grammar validation  
3. **Abstract Syntax Tree** - Tree representation of program structure
4. **Semantic Analysis** - Name resolution and validation
5. **Memory Layout** - Frame and variable allocation
6. **Code Generation** - Stack machine instruction generation
7. **Execution** - Interpretation on a virtual stack machine

## Language Features

### Data Types
- Single data type: 32-bit signed integers (two's complement)
- Used both as values and memory addresses
- Character constants (ASCII encoding)
- String constants (stored as null-terminated sequences)

### Lexical Elements
- **Integer constants**: Decimal numbers with optional sign (`+`/`-`), no leading zeros
- **Character constants**: Single quotes with escape sequences (`'\n'`, `'\xx'` hex)
- **String constants**: Double quotes with escape sequences
- **Identifiers**: Letters, digits, underscores (must start with letter or underscore)
- **Keywords**: `fun`, `var`, `if`, `then`, `else`, `while`, `do`, `let`, `in`, `end`
- **Operators**: `=`, `,`, `&&`, `||`, `!`, `==`, `!=`, `>`, `<`, `>=`, `<=`, `+`, `-`, `*`, `/`, `%`, `^`, `(`, `)`, `[`, `]`, `++`, `--`
- **Comments**: Single-line (`//`) and multi-line (`/* */`)

### Operator Precedence (highest to lowest)
| Priority | Operators | Description |
|----------|-----------|-------------|
| 1 | `^` `++` `--` `[i]` | Postfix (dereference, increment, decrement, index) |
| 2 | `!` `+` `-` `^` `++` `--` | Prefix operators |
| 3 | `*` `/` `%` | Multiplicative (left-associative) |
| 4 | `+` `-` | Additive (left-associative) |
| 5 | `==` `!=` `<` `>` `<=` `>=` | Relational (non-associative) |
| 6 | `&&` | Logical AND |
| 7 | `\|\|` | Logical OR |

### Program Structure
```
// Function declaration (no body)
fun functionName(param1, param2)

// Function definition
fun functionName(param1, param2) = statements

// Variable definition with initializers
var variableName = initialValue
```

### Statements
- **Expression statement**: Any expression
- **Assignment**: `lvalue = expression`
- **Conditional**: `if expression then statements else statements end`
- **While loop**: `while expression do statements end`
- **Let block**: `let definitions in statements end`

### Extended Features (Modified Language)

#### Array Indexing
```
var arr = 10 * 0      // array of 10 elements
arr[0] = 42           // write to first element
x = arr[5]            // read from sixth element
```

#### Increment/Decrement Operators
```
x++                   // post-increment (returns old value)
++x                   // pre-increment (returns new value)
x--                   // post-decrement
--x                   // pre-decrement
```

#### Multi-line Comments
```
/* This is a
   multi-line comment */
fun main() = 0
```

### Built-in Functions
| Function | Description |
|----------|-------------|
| `exit(code)` | Exit program with code |
| `getint()` | Read integer from stdin |
| `putint(n)` | Write integer to stdout |
| `getstr(addr)` | Read string to address |
| `putstr(addr)` | Write string from address |
| `new(size)` | Allocate heap memory |
| `del(addr)` | Free heap memory |

## Project Structure

```
simple-compiler/
├── docs/
│   └── language-specification.pdf   # Language specification
├── src/
│   └── pins25/
│       ├── common/           # Shared utilities
│       │   ├── AST.java      # Abstract Syntax Tree nodes
│       │   ├── Mem.java      # Memory structures (frames, access)
│       │   ├── PDM.java      # Stack machine instructions
│       │   ├── Report.java   # Error/warning reporting
│       │   └── Token.java    # Lexical tokens
│       └── phase/            # Compiler phases
│           ├── LexAn.java    # Lexical analyzer
│           ├── SynAn.java    # Syntax analyzer (parser)
│           ├── Abstr.java    # Abstract syntax construction
│           ├── SemAn.java    # Semantic analyzer
│           ├── Memory.java   # Memory layout computation
│           ├── CodeGen.java  # Code generator
│           └── Machine.java  # Stack machine interpreter
├── prg/                      # Test cases
|   |── Makefile
│   ├── lex/                  # Lexical analysis tests
│   ├── syn/                  # Syntax analysis tests
│   ├── sem/                  # Semantic analysis tests
│   ├── mem/                  # Memory layout tests
│   └── code/                 # Code generation tests   
└── README.md
```

## Building & Running

### Prerequisites
- Java 21 or later

### Compilation
```bash
cd tests
make all
```

### Running Individual Phases
```bash
# Lexical analysis
make lex-example    # Runs LexAn on lex/example.pins25

# Syntax analysis  
make syn-example    # Runs SynAn on syn/example.pins25

# Abstract syntax
make abs-example    # Runs Abstr on abs/example.pins25

# Semantic analysis
make sem-example    # Runs SemAn on sem/example.pins25

# Memory layout
make mem-example    # Runs Memory on mem/example.pins25

# Code generation
make code-example   # Runs CodeGen on code/example.pins25

# Execute program
make run-example    # Runs Machine on code/example.pins25
```

### Direct Execution
```bash
java -classpath bin pins25.phase.LexAn source.pins25
java -classpath bin pins25.phase.SynAn source.pins25
java -classpath bin pins25.phase.Abstr source.pins25
java -classpath bin pins25.phase.SemAn source.pins25
java -classpath bin pins25.phase.Memory source.pins25
java -classpath bin pins25.phase.CodeGen source.pins25
java -classpath bin pins25.phase.Machine source.pins25
```

## Example Program

```
// Factorial function
fun factorial(n) =
    if n <= 1 then
        1
    else
        n * factorial(n - 1)
    end

// Main entry point
fun main() =
    let
        var result = 0
    in
        result = factorial(5),
        putint(result)
    end
```

## Stack Machine Architecture

The target is a 32-bit stack machine with three registers:
- **IP** (Instruction Pointer) - Program counter
- **SP** (Stack Pointer) - Points to top of stack
- **FP** (Frame Pointer) - Points to current activation record

### Instruction Set

| Instruction | Description |
|-------------|-------------|
| `LOAD` | Load value from address on stack |
| `SAVE` | Store value to address on stack |
| `POPN` | Adjust stack pointer |
| `PUSH n` | Push constant onto stack |
| `NAME label` | Push label address onto stack |
| `REGN reg` | Push register value onto stack |
| `OPER op` | Perform arithmetic/logic operation |
| `UJMP` | Unconditional jump |
| `CJMP` | Conditional jump |
| `CALL` | Call subroutine |
| `RETN` | Return from subroutine |
| `INIT` | Initialize variable |

### Pseudo-Instructions (Data Segment)
| Instruction | Description |
|-------------|-------------|
| `LABEL name` | Define label |
| `SIZE n` | Allocate n bytes |
| `DATA value` | 32-bit constant |

## Development History

### Core Compiler Phases
- Base compiler structure with lexical analysis
- Syntax analysis with recursive descent parser
- Abstract syntax tree construction
- Semantic analysis with name resolution and l-value checking
- Memory layout and frame computation
- Code generation for stack machine
- Stack machine interpreter with built-in functions

### Language Extensions
The following extensions have been implemented beyond the base language specification:

| Feature | Description |
|---------|-------------|
| **Multi-line comments** | Block comments with `/* ... */` syntax |
| **Array indexing** | Access array elements with `arr[index]` syntax |
| **Increment/Decrement** | Pre/post `++` and `--` operators |
| **Consecutive operator check** | Syntax error on invalid operator sequences |
| **AST Analysis** | Node counting and tree analysis visitor |
| **Constant folding** | Compile-time evaluation of constant expressions |
| **Stack analysis** | Visitor for analyzing stack usage |
| **Scoped label generation** | Unique labels for nested scopes |

## Testing

The `prg/` directory contains test files for each compiler phase:

```bash
# Run a specific test
make lex-test1    # Runs LexAn on lex/test1.pins25
make syn-pass1    # Runs SynAn on syn/pass1.pins25
make abs-pass1    # Runs Abstr on abs/pass1.pins25
make sem-test1    # Runs SemAn on sem/test1.pins25
make mem-test1    # Runs Memory on mem/test1.pins25
make code-test-01   # Runs CodeGen on code/test-01.pins25
make run-test-01    # Runs Machine on code/test-01.pins25
```

## Implementation Notes

### Visitor Pattern
The compiler uses the visitor pattern extensively for AST traversal. Each phase implements an `AST.Visitor` interface to process tree nodes.

### AST Analysis
The compiler includes an AST analyzer that provides statistics about the parsed program:
- Node count by type (functions, variables, expressions, statements)
- Total node count
- Run with `Abstr` phase to see analysis output

### Error Handling
Errors are reported with source location information using `Report.Error`. The compiler provides:
- Lexical errors (invalid characters, unterminated strings)
- Syntax errors (unexpected tokens, malformed constructs)
- Semantic errors (undefined names, invalid l-values)
- Consecutive operator detection (prevents `++x++` style errors)

### Memory Model
- Global variables: Absolute addressing in data segment
- Local variables: Relative addressing from frame pointer
- Parameters: Passed on stack with static link for nested functions

## License

Educational project.

## References

- [Language Specification](docs/language-specification.pdf)
- [Grammophone](https://mdaines.github.io/grammophone/#/) - Tool for analyzing and transforming context-free grammars
- Based on a compilers course from Faculty of Computer and Information Science, University of Ljubljana
