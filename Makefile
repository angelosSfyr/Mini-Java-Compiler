all: compile

compile:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj
	javac Main.java

run:
	java Main Example.java
	
runll:
	clang  -o out1 Example.ll
	./out1	
	
clean:
	rm -f *.class *~
	rm JavaCharStream.java
	rm MiniJavaParser*
	rm Token*
	rm ParseException.java
	rm visitor/*
	rm syntaxtree/*
	rmdir visitor
	rmdir syntaxtree