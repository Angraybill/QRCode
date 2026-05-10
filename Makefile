.DEFAULT_GOAL := generator

# Compile all necessary files
generator: Generator.java UglyStuff.java Output.java ImageGenerator.java
	javac Generator.java
        
# Remove all compiled files
clean:
	rm *.class