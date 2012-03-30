# Standard definitions for make utility: Java version.
# Assumes that this file is included from a Makefile that defines
# JAVA_SRCS to be a list of Java source files to be compiled.
# It may optionally define OTHER_CLASSES to contain names of classes
# that aren’t derivable from the names of the JAVA_SRCS files.
# The including Makefile may subsequently override JFLAGS (flags to
# the Java compiler), and JAVAC (the Java compiler’s name), by putting
# these definitions after the "include".
#

TURNIN := /lusr/bin/turnin
GRADER := FIXME
LAB1_NAME := cs439-labADisk
LAB2_NAME := cs439-labFlatFS
LAB3_NAME := cs439-labHierFS

JAVAC = javac
JFLAGS = -g
JAVA_SRCS = ActiveTransactionList.java Common.java Disk.java DiskWorker.java PTree.java \
	SimpleLock.java WriteBackList.java ADisk.java DirEnt.java DiskResult.java FlatFS.java ResourceException.java Transaction.java \
	CallbackTracker.java DiskCallback.java DiskUnit.java LogStatus.java RFS.java TransID.java

CLASSES = $(JAVA_SRCS:.java=.class) $(OTHER_CLASSES)

.PHONY: clean check default

# Default entry
default: $(CLASSES)

$(CLASSES): $(JAVA_SRCS)
	$(JAVAC) $(JFLAGS) $(JAVA_SRCS)

clean:
	/bin/rm -f $(CLASSES) *~

handin-1: handin.tar
	echo "FIX MAKEFILE TO INCLUDE THE TA user ID for the cs machines"
	echo "Turning in handin.tar containing the following files:"
	tar tf handin.tar
	$(TURNIN) --submit $(GRADER) $(LAB1_NAME) handin.tar

handin-2: handin.tar
	echo "FIX MAKEFILE TO INCLUDE THE TA user ID for the cs machines"
	echo "Turning in handin.tar containing the following files:"
	tar tf handin.tar
	$(TURNIN) --submit $(GRADER) $(LAB2_NAME) handin.tar

handin-3: handin.tar
	echo "FIX MAKEFILE TO INCLUDE THE TA user ID for the cs machines"
	echo "Turning in handin.tar containing the following files:"
	tar tf handin.tar
	$(TURNIN) --submit $(GRADER) $(LAB3_NAME) handin.tar

handin.tar: clean
	tar cf handin.tar `find . -type f | grep -v '^\.*$$' | grep -v '/CVS/' | grep -v '/\.svn/' | grep -v '/\.git/' | grep -v 'lab[0-9].*\.tar\.gz' | grep -v '/\~/' | grep -v '/\.tar/'` 