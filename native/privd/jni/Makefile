TARGET=libprivd.so

all: $(TARGET)

# OS X include path for java headers

JDK_ROOT=/opt/java
CFLAGS=-I$(JDK_ROOT)/include -I$(JDK_ROOT)/include/linux -fPIC -m32 -Wall -O2

LD=gcc
CC=gcc

# Name used for source, header, and object file

PREFIX=com_netifera_platform_internal_system_privd_PrivilegeDaemonNative

OBJECT=$(PREFIX).o
HEADER=$(PREFIX).h
SOURCE=$(PREFIX).c

# where to find the plugin projects

WORKSPACE_ROOT=../../../platform/com.netifera.platform.core

# name of the package which contains the java class we are producing an JNI for

PACKAGE=com.netifera.platform.system.privd

# name of the class itself

CLASS=com.netifera.platform.internal.system.privd.PrivilegeDaemonNative

# classpath for calling javah  ie: '../../../platform/com.netifera.platform.core/com.netifera.platform.system.privd/bin'

CLASSPATH=$(WORKSPACE_ROOT)/$(PACKAGE)/bin

LIBS=-lc
LDFLAGS=-m32 -s -shared

$(TARGET): $(OBJECT)
	$(LD) $(LDFLAGS) -o $(TARGET) $(LIBS) $(OBJECT)


$(HEADER):
	javah -classpath $(CLASSPATH) -d . $(CLASS)

$(OBJECT): $(HEADER) $(SOURCE)
	$(CC) $(CFLAGS) -c $(SOURCE)

install: $(TARGET)
	cp $(TARGET) $(WORKSPACE_ROOT)/$(PACKAGE)
clean:
	rm -f $(HEADER) $(OBJECT) $(TARGET)


