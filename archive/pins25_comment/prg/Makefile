JAVA = java --enable-preview
#JAVAC = javac --enable-preview --release 24
JAVAC = javac --release 21
SRC = ../src
BIN = ../bin

.PHONY: all clean %

# Compile all Java sources
all:
	mkdir -p $(BIN)
	$(JAVAC) -d $(BIN) $(shell find $(SRC) -name "*.java")

# Pattern rule for running Lexical 
lex-%: lex/%.pins25
	$(JAVA) -classpath ../bin pins25.phase.LexAn lex/$*.pins25
# Pattern rule for running SynAn
syn-%: syn/%.pins25
	$(JAVA) -classpath ../bin pins25.phase.SynAn syn/$*.pins25
# Pattern rule for running Abs
abs-%: abs/%.pins25
	$(JAVA) -classpath ../bin pins25.phase.Abstr abs/$*.pins25
# Pattern rule for running SemAn
sem-%: sem/%.pins25
	$(JAVA) -classpath ../bin pins25.phase.SemAn sem/$*.pins25
# Pattern rule for running Memory
mem-%: mem/%.pins25
	$(JAVA) -classpath ../bin pins25.phase.Memory mem/$*.pins25
# Pattern rule for running CodeGen
code-%: code/%.pins25
	$(JAVA) -classpath ../bin pins25.phase.CodeGen code/$*.pins25
# Pattern rule for running Machine in code
run-%: code/%.pins25
	$(JAVA) -classpath ../bin pins25.phase.Machine code/$*.pins25
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