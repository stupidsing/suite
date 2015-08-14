### What is suite?

Library with all sorts of useful code pieces for myself. In particular it has a logic interpreter and a functional code compiler.

### Any pre-requisite?

- Linux, unix, or cygwin
- JDK 1.8
- Apache Maven 3
- rlwrap if you want to use the convenient interactive prompt

### How to build and run the project?

Execute run.sh. It executes maven to build the java code, and performs pre-compilation as well.


### How to in set up in eclipse?

Execute command
mvn eclipse:clean eclipse:eclipse
and imports the project into any workspace.


### I get stack overflow error when running org.instructionexecutor test cases. What can I do?

Eclipse -> Window -> Preferences -> Java -> Installed JREs -> (Your JRE)
-> Edit -> Default VM Arguments -> Enter "-Xss2m"

or use a 64-bit JVM


### How to evaluate logical programs?

printf '? dump something, nl' | ./run.sh --quiet


### How to evaluate functional programs?

./run.sh evaluate 'true'


### How to evaluate functional filters?

printf 'inputs...\ninputs...\n' | ./run.sh filter reverse


### How to evaluate monadic functional filters?

./run.sh --using MONAD filter --do 'concatm . map {sh/ {}} . split {10}'
