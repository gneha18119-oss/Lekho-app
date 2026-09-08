#!/bin/sh
set -e

GRADLE_VERSION=8.11.1
BASE="$HOME/.gradle/lekho-gradle-$GRADLE_VERSION"
DIST="$BASE/gradle-$GRADLE_VERSION"
ZIP="$BASE/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$BASE"
  curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  rm -rf "$DIST"
  unzip -q "$ZIP" -d "$BASE"
fi

exec "$DIST/bin/gradle" "$@"
