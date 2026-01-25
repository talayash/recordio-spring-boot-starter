#!/bin/sh
set -e
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.6"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven..."
    mkdir -p "$HOME/.m2/wrapper/dists"
    curl -sL "$MAVEN_URL" -o "/tmp/maven.zip"
    unzip -q "/tmp/maven.zip" -d "$HOME/.m2/wrapper/dists"
    rm "/tmp/maven.zip"
fi

export M2_HOME="$MAVEN_HOME"
export PATH="$MAVEN_HOME/bin:$PATH"
exec mvn "$@"
