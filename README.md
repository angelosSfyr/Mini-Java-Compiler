# Mini-Java-Compiler

I created a compiler for the MiniJava language, a simplified version of Java, as part of my coursework in the Compilers course at the Department of Informatics and Telecommunications.

# The Compilation Process
The compiler converts MiniJava code into LLVM's intermediate representation. It operates through the following steps:

Parsing MiniJava source files to create a parse tree and identify/report syntax errors.

Traversing the parse tree using the Visitor pattern, building a symbol table and identifying semantic errors.

Generating LLVM assembly language code using the Visitor pattern.

# Semantic Analysis and Type Checking
The compiler uses a symbol table to keep track of information about entities in the source code. Afterwards, the compiler performs type checking. If a MiniJava rule is violated, the compilation terminates and an error message is displayed.

# Intermediate Code Generation
The Visitor pattern and a selected set of LLVM instructions are utilized to produce LLVM's intermediate representation from the original MiniJava code.

# Technologies Used
Java

JavaCC

JTB

JFlex

Java CUP
