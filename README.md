### What is this?

Library with all sorts of useful code pieces and silly attempts by myself (stupidsing). In particular it has...

- a functional interpreter
- a logic interpreter
- B-tree implementations
- some persistent (immutable) data structures


### Any pre-requisites?

- Linux, unix, or cygwin
- JDK 1.8
- Apache Maven 3
- rlwrap if you want more command-line convenience


### How to build and run the project?

Execute run.sh. It builds Java code, and performs pre-compilation.


### How to set up in eclipse?

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
