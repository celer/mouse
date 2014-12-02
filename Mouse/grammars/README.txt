
This directory contains three sample grammars.

'Java.1.6.peg' is a Java 1.6 grammar based on chapters 3 and 18
of Java Language Specification, Third Edition, with corrections
based on other chapters and test results.

'Java.1.7.peg' is a Java 1.7 grammar based on chapters 3 and 18 
of Java Language Specification, Java SE7 Edition dated 2012-07-27, 
with corrections based on other chapters and test results.

'C.peg' is a C grammar based on ISO/IEC 9899.1999:TC2 standard 
without preprocessor directives, meaning that it applies to 
preprocessed C source.
The 'typedef' feature makes the syntax of C context-dependent,
which cannot be expressed in PEG formalism. The problem is solved 
by semantic actions provided in 'C.java'.
The parser generated from 'C.peg' is supposed to use semantics
class obtained by compiling that file.
