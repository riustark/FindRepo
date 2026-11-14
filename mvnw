#!/bin/sh
# Official Maven Wrapper script with fallback support
basedir=$(dirname "$0")
if [ -f "$basedir/.tools/maven/bin/mvn" ]; then
    exec "$basedir/.tools/maven/bin/mvn" "$@"
else
    exec mvn "$@"
fi
