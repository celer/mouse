all: main

test: main
	./main -input input1.txt -result 89.0
	./main -input input2.txt -result -13.0
	./main -input input3.txt -result 6.9
	./main -input input4.txt -result 4.0
	./main -input bad.txt -error "After '33+': expected Space or Product"
	./main -input bad1.txt -error "After '44+4': expected [0-9] or '.' or Space or MultOp or Factor or AddOp or end of text"
	./main -input bad2.txt -error "After '44-44/34': expected [0-9] or '.' or Space or MultOp or Factor or AddOp or end of text"
	./main -input bad3.txt -error "After '33.1-23.': expected Digits"

main: main.go MySemantics.go MyParser.go
	go build main.go MyParser.go MySemantics.go

MyParser.go:
	java -cp ../../Mouse.jar mouse.GoGenerate -G myGrammar.txt -P MyParser -S MySemantics -M -r "../../src/runtime"

clean:
	rm -rf MyParser.go main
	
