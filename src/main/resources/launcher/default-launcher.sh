#!/bin/bash
set -e
if [ -z $APP_HOME ]; then
  echo "No APP_HOME environment variable set"
  exit 1
fi

NAME=$(app conf get launcher.name)
DESCRIPTION=$(app conf get launcher.description)
PROGRAM=$(app conf get launcher.command)

if [ -z $NAME]; then
  echo "Missing NAME in config, defaulting to main"
  NAME="main"
fi

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

if [ -f $APP_HOME/environment ]; then
  source $APP_HOME/environment
fi

exec ${PROGRAM} 2>&1
