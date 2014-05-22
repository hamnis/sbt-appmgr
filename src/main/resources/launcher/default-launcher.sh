#!/bin/bash
set -e
if [ -z $APP_HOME ]; then
  echo "No APP_HOME environment variable set"
  exit 1
fi

NAME=$(app conf get app.name)
DESCRIPTION=$(app conf get app.description)
PROGRAM=$(app conf get app.program)
case $PROGRAM in
  /*) ;;
  *) PROGRAM=${APP_HOME}/current/bin/${PROGRAM}
esac

if [ -z $APP_FOREGROUND ]; then
  mkdir -p "${APP_HOME}/logs"
  exec >> "${APP_HOME}/logs/${NAME}.out"
  exec 2>&1
fi

echo "Starting ${DESCRIPTION}"

# Plainly pass any argument in the "${NAME}" group as a system property
for line in $(app cat-conf -g ${NAME} | cut -f 2- -d .)
do
  JAVA_OPTS="${JAVA_OPTS} -D$line"
done

export JAVA_OPTS
export JVM_OPT=$JAVA_OPTS
export APP_HOME=${APP_HOME}/current
exec ${PROGRAM} 2>&1
