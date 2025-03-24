JAVA = java --enable-preview
#JAVAC = javac --enable-preview --release 22
JAVAC = javac --enable-preview --release 21
SRC = ../src
BIN = ../bin

.PHONY: all clean %

# Compile all Java sources
all:
	mkdir -p $(BIN)
	$(JAVAC) -d $(BIN) $(shell find $(SRC) -name "*.java")

# Pattern rule for running LexAn
%: %.pins25
	$(JAVA) -classpath ../bin pins25.phase.SynAn $<

# Clean compiled files
clean:
	rm -rf $(BIN)

# # Compile all Java sources
# all:
# 	mkdir -p $(BIN)
# 	$(JAVAC) -d $(BIN) --module-source-path $(SRC) $(shell find $(SRC) -name "*.java")

# # Pattern rule for running LexAn
# %: %.pins25
# 	$(JAVA) -p ../bin -m pins25/pins25.phase.LexAn $<

# # Clean compiled files
# clean:
# 	rm -rf $(BIN)