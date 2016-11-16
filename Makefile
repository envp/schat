# Clean binaries
SOURCES=src
SRC_PATH=$(SOURCES)/schat
DOC_PATH=doc
BUILD_PATH=build

# Executables
JC=javac
JI=java
JD=javadoc

# Target FQN
TARGET=schat.SChat



all: build

._: clean

clean:
	@echo Cleaning up binaries...
	@rm -r -v $(BUILD_PATH)/

compile:
	@echo Building sources...
	@mkdir -p build
	@$(JC) -g -d $(BUILD_PATH) -sourcepath $(SOURCES) $(SRC_PATH)/*.java
	@echo Sources built!

run_client:
	@echo 
	@$(JI) -cp $(BUILD_PATH) $(TARGET) c 9912

run_server:
	@echo 
	$(JI) -cp $(BUILD_PATH) $(TARGET) s 9912

build: clean compile run
	
server: compile run_server