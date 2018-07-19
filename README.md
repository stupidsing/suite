### What is this?

Library, mostly Java, with all sorts of useful code pieces and courageous attempts by myself. In particular it has

- some [persistent data structures] (https://github.com/stupidsing/suite/tree/master/src/main/java/suite/immutable/): B-trees, sets, maps, ropes, priority queues

- primitive data types, including primitive sets or maps, which is actually generated by a script.
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/primitive/

- a pair type for generic use, and a 'tuple type' called Fixie...
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/adt/pair/

- an re-implementation of streams.
This 'streamlet' implementation, unlike Java streams, do not drain up upon the first retrieval; and it provide a 'key-value' based stream (Streamlet2) which is more suitable in map processing.

- a generic lexer and parser for an operator-based grammar, which is adapted around the repository
https://github.com/stupidsing/suite/blob/master/src/main/java/suite/node/parser/Lexer.java
https://github.com/stupidsing/suite/blob/master/src/main/java/suite/node/parser/IterativeParser.java

- some other parser generators
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/ebnf/lr/
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/ebnf/topdown/

- some functional programming language interpreters and compilers
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/fp/
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/funp/

- some logic interpreter and compilers
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/lp/doer/
https://github.com/stupidsing/suite/blob/master/src/main/java/suite/lp/sewing/impl/SewingProverImpl.java

- B-tree implementations

- some numerical algorithms, BFGS or what...
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/math/linalg/

- stock quote readers, some stock indicator calculations (and some dangerous quantitative strategies that could not earn me money)
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/trade/data/
https://github.com/stupidsing/suite/tree/master/src/main/java/suite/trade/analysis/

some code come with simple JUnit test cases, in src/test/java folder.

Most often I come to a new book in library, tried to grasp some ideas from it, and write something new into this repo. You can see I tend to write the same thing again and again, and I hope I learnt something through the process.

Recently I was thinking about a new name for this muddle of stuff... maybe call it 'pointless', or call it 'dangerous' :-) lots of rename refactoring need to be done though.


### Who am I?

An enthusiastic programmer, with some computer science, maths background. I write Java for living (curriculum vitae https://rawgit.com/stupidsing/pieces/master/resume.html).


### Any pre-requisites to build this mess?

Get following reasonable things ready:
- Linux, unix, or cygwin
- git
- JDK 10
- Apache Maven 3
- eclipse
- rlwrap if you want more command-line convenience

Check-out and execute run.sh. It builds Java code, and performs some pre-compilation.


### How to set up the project in eclipse?

Execute command
mvn eclipse:clean eclipse:eclipse
and imports the project into any workspace.


### How to evaluate logical programs?

printf '? dump something, nl' | ./run.sh --quiet


### How to evaluate functional programs?

./run.sh evaluate 'true'


### How to evaluate functional filters?

printf 'inputs...\ninputs...\n' | ./run.sh filter reverse


### How to evaluate monadic functional filters?

./run.sh --use MONAD filter --do 'concatm . map {sh/ {}} . split {10}'
