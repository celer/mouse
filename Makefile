all: test

dist: clean
	(cd Mouse && ant && cp build/Mouse-1.6.1.jar ../Mouse.jar)

clean:
	(cd Mouse && ant clean)
	(cd examples/example2 && make clean)
	(cd examples/example3 && make clean)
	(cd examples/example4 && make clean)
	(cd examples/example5 && make clean)
	

test: dist
	(cd examples/example2 && make test)
	(cd examples/example3 && make test)
	(cd examples/example4 && make test)
	(cd examples/example5 && make test)
