#!/usr/bin/env bash
if [ "$JAVA_HOME" == "" ]; then
  echo "Cannot find JAVA_HOME installation: $JAVA_HOME must be set"
  exit 1
fi

cygwin=false
darwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`
script=`basename "$PRGDIR"`
bin=`cd "$bin"; pwd`
PRGDIR="$bin/$script"

# the root of the Hadoop installation
BATCHCOUNT_HOME=`dirname "$PRGDIR"`/..

JAVA=$JAVA_HOME/bin/java

CLASSPATH=${CLASSPATH}:$JAVA_HOME/lib/tools.jar

for f in $BATCHCOUNT_HOME/lib/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

eval $JAVA -classpath "$CLASSPATH" com.lin.stride.search.server.ServerShutdown