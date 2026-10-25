#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"

# Prefer an explicitly configured JDK, then Ashish's installed Android Studio JDK.
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
    JAVA_EXEC="$JAVA_HOME/bin/java"
    JAVAC_EXEC="$JAVA_HOME/bin/javac"
elif [[ -x "$HOME/Applications/android-studio/jbr/bin/javac" ]]; then
    JAVA_EXEC="$HOME/Applications/android-studio/jbr/bin/java"
    JAVAC_EXEC="$HOME/Applications/android-studio/jbr/bin/javac"
elif command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
    JAVA_EXEC="$(command -v java)"
    JAVAC_EXEC="$(command -v javac)"
else
    echo "JDK 17 or newer required. Set JAVA_HOME to your JDK folder."
    exit 1
fi

mkdir -p out
find src -name '*.java' -print | sort > sources.txt
"$JAVAC_EXEC" --release 17 -encoding UTF-8 -d out @sources.txt

if [[ "${1:-}" == "--test" ]]; then
    find test -name '*.java' -print | sort > test-sources.txt
    "$JAVAC_EXEC" --release 17 -encoding UTF-8 -cp out -d out @test-sources.txt
    "$JAVA_EXEC" -cp out com.ashish.placementtracker.TrackerTests
else
    "$JAVA_EXEC" -cp out com.ashish.placementtracker.Main
fi
