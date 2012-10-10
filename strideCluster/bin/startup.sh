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
STRIDE_HOME=`dirname "$PRGDIR"`/..

STRIDE_CONF_DIR="${STRIDE_CONF_DIR:-$STRIDE_HOME/conf}"
STRIDE_LOG_DIR="$STRIDE_HOME/logs"
STRIDE_OUT="$STRIDE_LOG_DIR"/stride.out

JAVA=$JAVA_HOME/bin/java
JAVA_HEAP_MAX=-Xmx2048m 

CLASSPATH="${STRIDE_CONF_DIR}"
CLASSPATH=${CLASSPATH}:$JAVA_HOME/lib/tools.jar
for f in $STRIDE_HOME/lib/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

echo $CLASSPATH

STRIDE_OPTS="$STRIDE_OPTS -Dstride.home.dir=$STRIDE_HOME"
STRIDE_OPTS="$STRIDE_OPTS -Dstride.log.dir=$STRIDE_LOG_DIR"
#exec $JAVA $JAVA_HEAP_MAX $STRIDE_OPTS -cp "$CLASSPATH" com.lin.stride.server > /dev/null &
nohup $JAVA $JAVA_HEAP_MAX $STRIDE_OPTS -cp "$CLASSPATH" com.lin.stride.search.server.StrideSearchServer >> "$STRIDE_OUT" 2>&1 &
