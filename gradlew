#!/bin/sh
set -eu
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
GRADLE_VERSION="8.9"
GRADLE_HOME="$ROOT_DIR/.gradle-dist/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$ROOT_DIR/.gradle-dist"
  ARCHIVE="$ROOT_DIR/.gradle-dist/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ARCHIVE" ]; then
    command -v curl >/dev/null 2>&1 || { echo "curl is required to bootstrap Gradle $GRADLE_VERSION" >&2; exit 1; }
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
  fi
  command -v unzip >/dev/null 2>&1 || { echo "unzip is required to bootstrap Gradle $GRADLE_VERSION" >&2; exit 1; }
  unzip -q -o "$ARCHIVE" -d "$ROOT_DIR/.gradle-dist"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
