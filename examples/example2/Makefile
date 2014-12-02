all: main

test: main
	./main -input input1.txt -result 89
	./main -input bad.txt -error "After '33+': expected Number"
	./main -input bad1.txt -error "After '44+4': expected [0-9] or '+' or end of text"
	./main -input bad2.txt -error "After '44': expected [0-9] or '+' or end of text"

main: main.go MySemantics.go MyParser.go
	go build main.go MyParser.go MySemantics.go

MyParser.go:
	java -cp ../../Mouse.jar mouse.GoGenerate -G myGrammar.txt -P MyParser -S MySemantics -r "../../src/runtime"

clean:
	rm -rf MyParser.go main
	
