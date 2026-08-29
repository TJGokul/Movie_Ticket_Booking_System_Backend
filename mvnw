#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper Bash Script for Linux/macOS
# ----------------------------------------------------------------------------
set -e
MAVEN_VERSION="3.9.8"
DIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_HOME="$DIR/.mvn/maven-$MAVEN_VERSION"

if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
    echo "[INFO] Maven not found locally. Downloading Apache Maven $MAVEN_VERSION..."
    mkdir -p "$DIR/.mvn"
    ZIP="$DIR/.mvn/maven.zip"
    curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip" -o "$ZIP"
    unzip -q "$ZIP" -d "$DIR/.mvn"
    rm -f "$ZIP"
    mv "$DIR/.mvn/apache-maven-$MAVEN_VERSION" "$MAVEN_HOME"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
