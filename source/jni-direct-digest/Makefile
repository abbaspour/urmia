#!/usr/bin/make -f
#
# Makefile for native stuff
#

# c files to compile
C_SOURCES	:= io_urmia_dd_DirectDigest.c md5.c

# the name of the library to build
LIBNAME		:= direct-digest

C_SOURCE_DIR    := src/main/c
C_GENSOURCE_DIR := src/main/c

TARGET_DIR	:= target
C_BUILD_DIR    = $(TARGET_DIR)/native
JAVA_BUILD_DIR = $(TARGET_DIR)/classes

# the name of the file we build
#TARGET		= $(JAVA_BUILD_DIR)/META-INF/native/$(OSID)$(BIT_MODE)/$(LIB_PREFIX)$(LIBNAME)$(LIB_EXTN)
TARGET		= $(JAVA_BUILD_DIR)/META-INF/native/$(LIB_PREFIX)$(LIBNAME)$(LIB_EXTN)

#TARGET_dylib= $(JAVA_BUILD_DIR)/META-INF/native/$(OSID)$(BIT_MODE)/$(LIB_PREFIX)$(LIBNAME).dylib
TARGET_dylib= $(JAVA_BUILD_DIR)/META-INF/native/$(LIB_PREFIX)$(LIBNAME).dylib

# find the jdk. if this doesn't work for you, define JAVA_HOME in your
# environment or on the make command line
JAVA_HOME ?= /usr/lib/jvm/java-7-openjdk-amd64/
#JAVA_HOME = /Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home


# classpath for javah
JAVAH_CLASSPATH = $(TARGET_DIR)/classes

# tools and options
CFLAGS = -Wall -fpic
CPPFLAGS = -I$(C_SOURCE_DIR) -I$(C_GENSOURCE_DIR) -Iinclude -I$(JAVA_HOME)/include
JAVAH = $(JAVA_HOME)/bin/javah
JAVAH_FLAGS += -classpath $(JAVAH_CLASSPATH)
JAVAH_CMD = $(JAVAH) $(JAVAH_FLAGS) $(OUTPUT_OPTION)
LDFLAGS = -shared
LINK.so = $(CC) $(LDFLAGS) $(LD_LIBS)

ifdef DEBUG
CFLAGS += -g
LDFLAGS += -g
endif

# os-dependent bits
UNAME := $(shell uname)

ifeq ($(UNAME),Linux)
LIB_PREFIX = lib
LIB_EXTN = .so
CPPFLAGS += -I$(JAVA_HOME)/include/linux
OSID=linux
else
ifeq ($(UNAME),Darwin)
LIB_PREFIX = lib
LIB_EXTN = .jnilib       # dylib for jdk7 and jnilib for jdk6
CPPFLAGS += -I$(JAVA_HOME)/include/darwin
OSID=osx
else
ifeq ($(findstring CYGWIN,$(UNAME)),CYGWIN)
LIB_PREFIX =
LIB_EXTN = .dll
CPPFLAGS += -I$(JAVA_HOME)/include/win32
OSID=windows
else
f := $(error Platform $(UNAME) not supported)
endif
endif
endif

# bit mode
MACHINE := $(shell uname -m)

ifeq ($(MACHINE),x86_64)
BIT_MODE=64
CFLAGS += -m64
else
BIT_MODE=32
CFLAGS += -m32
endif

# we look in $(C_SOURCE_DIR) for c files...
vpath %.c $(C_SOURCE_DIR)

# convenience variables
C_OBJFILES = $(addprefix $(C_BUILD_DIR)/,$(subst .c,.o,$(C_SOURCES)))

# default target
all: $(TARGET)

ifeq ($(UNAME),Darwin)
all: $(all) $(TARGET_dylib)
endif

# rule to compile the .c files
$(C_BUILD_DIR)/%.o: %.c
	@mkdir -p `dirname $@`
	$(COMPILE.c) $(OUTPUT_OPTION) $<

# link the C objects into a shared library
$(TARGET): $(C_OBJFILES) $(LDLIBS)
	@mkdir -p `dirname $@`
	$(LINK.so) $(OUTPUT_OPTION) $^

# a rule to build the .h file with javah
$(C_GENSOURCE_DIR)/io_urmia_dd_DirectDigest.h: $(JAVA_BUILD_DIR)/io/urmia/dd/DirectDigest.class
	rm -f $@
	$(JAVAH) $(JAVAH_FLAGS) $(OUTPUT_OPTION) io.urmia.dd.DirectDigest

# the .o file depends on the .h file
$(C_BUILD_DIR)/io_urmia_dd_DirectDigest.o: $(C_GENSOURCE_DIR)/io_urmia_dd_DirectDigest.h

$(TARGET_dylib): $(TARGET)
	@cp $(TARGET) $(TARGET_dylib)

clean::
	rm -f $(C_OBJFILES)
	rm -f $(TARGET)
#rm -f $(C_BUILD_DIR)/jnirules.mak

