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

run:
	@echo =================================================================
	@echo 
	@$(JI) -cp $(BUILD_PATH) $(TARGET)

build: clean compile run