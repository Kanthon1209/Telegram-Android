#!/bin/sh
set -e
ROOT="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
JAVA_HOME="${JAVA_HOME:-/home/box/jdk/jdk-17.0.21+9}"
export PATH="$JAVA_HOME/bin:$PATH"
OUT="$ROOT/kgram-auth-tests/out"
rm -rf "$OUT"
mkdir -p "$OUT"
javac --release 17 -encoding UTF-8 -d "$OUT" \
  "$ROOT/TMessagesProj/src/main/java/org/telegram/messenger/kgram/KgramServerConfig.java" \
  "$ROOT/TMessagesProj/src/main/java/org/telegram/messenger/kgram/KgramAuthPolicy.java" \
  "$ROOT/kgram-auth-tests/KgramAuthPolicyTest.java"
java -cp "$OUT" KgramAuthPolicyTest "$ROOT"
