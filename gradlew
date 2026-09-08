#!/bin/sh
set -e

GRADLE_VERSION=8.11.1
GRADLE_HOME="$HOME/.gradle/lekho-gradle-$GRADLE_VERSION"
GRADLE_ZIP="$HOME/.gradle/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$HOME/.gradle"
  curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$GRADLE_ZIP"
  rm -rf "$GRADLE_HOME"
  mkdir -p "$GRADLE_HOME"
  unzip -q "$GRADLE_ZIP" -d "$HOME/.gradle"
  mv "$HOME/.gradle/gradle-$GRADLE_VERSION" "$GRADLE_HOME"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
