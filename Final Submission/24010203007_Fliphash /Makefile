# Makefile

# Classpath for compilation.
CP = .:jars/jna-5.13.0.jar:jars/jna-platform-5.13.0.jar:jars/oshi-core-6.3.0.jar:jars/slf4j-api-2.0.9.jar:jars/slf4j-simple-2.0.9.jar:xxh3Java/lib/annotations-26.0.2.jar

# Output directory for compiled classes.
BIN = bin

# Find all .java files except those in the bin directory.
SRC := $(shell find . -name "*.java" ! -path "./$(BIN)/*")

all: clean compile jars

clean:
	@echo "Cleaning up..."
	rm -rf $(BIN) *.jar BackendServer.mf

compile:
	@echo "Compiling sources..."
	mkdir -p $(BIN)
	javac -cp "$(CP)" -d $(BIN) $(SRC)

jars: manifest-backend
	@echo "Packaging jars..."
	# Package the LoadBalancer jar (no special manifest needed)
	jar cvfe LoadBalancer.jar LoadBalancer.LoadBalancerServer -C $(BIN) .
	# Package the BackendServer jar with the generated manifest file.
	jar cvfm BackendServer.jar BackendServer.mf -C $(BIN) .
	# Package the FlipHashClient jar (no special manifest needed)
	jar cvfe FlipHashClient.jar client.FlipHashClient -C $(BIN) .

manifest-backend:
	@echo "Creating manifest for BackendServer..."
	@echo "Manifest-Version: 1.0" > BackendServer.mf
	@echo "Main-Class: backend.BackendServer" >> BackendServer.mf
	@echo "Class-Path: jars/jna-5.13.0.jar jars/jna-platform-5.13.0.jar jars/oshi-core-6.3.0.jar jars/slf4j-api-2.0.9.jar jars/slf4j-simple-2.0.9.jar xxh3Java/lib/annotations-26.0.2.jar" >> BackendServer.mf

.PHONY: all clean compile jars manifest-backend
