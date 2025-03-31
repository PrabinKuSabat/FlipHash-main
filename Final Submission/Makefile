# Java commands
JAVAC = javac
JAVA = java

# Directories
SRC = src
BIN = bin
LIB = lib

# Source directories
BACKEND_SRC = $(SRC)/fliphash/backend
LOADBALANCER_SRC = $(SRC)/fliphash/LoadBalancer
CLIENT_SRC = $(SRC)
UTILITIES_SRC = $(SRC)
XXH3_SRC = $(SRC)/fliphash/xxh3Java

# Required JAR files
JNA_JAR = $(LIB)/jna-5.13.0.jar
JNA_PLATFORM_JAR = $(LIB)/jna-platform-5.13.0.jar
MATRIX_JAR = $(LIB)/MatrixMultiplication.jar
OSHI_JAR = $(LIB)/oshi-core-6.3.0.jar

# Classpath
CLASSPATH = $(BIN):$(JNA_JAR):$(JNA_PLATFORM_JAR):$(MATRIX_JAR):$(OSHI_JAR)

# Source files for each component
BACKEND_FILES = $(wildcard $(BACKEND_SRC)/*.java)
LOADBALANCER_FILES = $(wildcard $(LOADBALANCER_SRC)/*.java)
CLIENT_FILES = $(CLIENT_SRC)/FlipHashClient.java $(CLIENT_SRC)/FlipHash.java $(CLIENT_SRC)/FlipHashQueue.java
UTILITIES_FILES = $(UTILITIES_SRC)/TerminalDisplayManager.java $(UTILITIES_SRC)/Validator.java
XXH3_FILES = $(wildcard $(XXH3_SRC)/*.java)

.PHONY: all clean backend loadbalancer client utilities xxh3

all: backend loadbalancer client utilities xxh3

backend: $(BACKEND_FILES) | $(BIN)
	$(JAVAC) -cp $(CLASSPATH) -d $(BIN) $(BACKEND_FILES)

loadbalancer: $(LOADBALANCER_FILES) | $(BIN)
	$(JAVAC) -cp $(BIN) -d $(BIN) $(LOADBALANCER_FILES)

client: $(CLIENT_FILES) | $(BIN)
	$(JAVAC) -cp $(BIN) -d $(BIN) $(CLIENT_FILES)

utilities: $(UTILITIES_FILES) | $(BIN)
	$(JAVAC) -cp $(BIN) -d $(BIN) $(UTILITIES_FILES)

xxh3: $(XXH3_FILES) | $(BIN)
	$(JAVAC) -cp $(BIN) -d $(BIN) $(XXH3_FILES)

$(BIN):
	mkdir -p $(BIN)

clean:
	rm -rf $(BIN)

# Check for required JAR files
$(JNA_JAR) $(JNA_PLATFORM_JAR) $(MATRIX_JAR) $(OSHI_JAR):
	@test -f $@ || (echo "Missing required JAR file: $@" && exit 1)

# Make backend depend on JAR files
backend: $(JNA_JAR) $(JNA_PLATFORM_JAR) $(MATRIX_JAR) $(OSHI_JAR)

