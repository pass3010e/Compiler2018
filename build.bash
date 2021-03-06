# this script is called when the judge is building your compiler.
# no argument will be passed in.

set -e
cd "$(dirname "$0")"
mkdir -p bin
touch empty
g++ inputbuilder.cpp -o inputbuilder
g++ -o objconv ./obj/objconv-master/src/*.cpp -std=c++03
chmod +x c2nasm.sh
find ./src -name *.java | javac -d bin -classpath "lib/antlr-4.7.1-complete.jar" @/dev/stdin
