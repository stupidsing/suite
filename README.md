# What is suite?

Library with all sorts of tools or useful code pieces. In particular it has a logic interpreter and a functional code compiler.


# How to build and set up the project?

Execute run.sh. It executes maven to build the java code, and performs pre-compilation as well.


# I get stack overflow error when running org.instructionexecutor test cases. What can I do?

Eclipse -> Window -> Preferences -> Java -> Installed JREs -> (Your JRE)
-> Edit -> Default VM Arguments -> Enter "-Xss2m"


# How to evaluate logical programs?

printf '? dump something, nl' | ./run.sh --quiet


# How to evaluate functional programs?

./run.sh evaluate 'true'


# How to evaluate functional filters?

printf 'inputs...\ninputs...\n' | ./run.sh filter id
