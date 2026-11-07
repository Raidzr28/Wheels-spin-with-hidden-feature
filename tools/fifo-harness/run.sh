#!/usr/bin/env sh
# Compiles the real PickerViewModel against the stubs in src/ and runs the scenarios.
# Needs kotlinc 2.0+ and a JDK on PATH. See README.md.
set -e

here=$(cd "$(dirname "$0")" && pwd)
out="$here/build"
viewmodel="$here/../../app/src/main/java/com/wheelsspin/namepicker/PickerViewModel.kt"

mkdir -p "$out"
kotlinc "$here"/src/*.kt "$viewmodel" -include-runtime -d "$out/fifo-harness.jar"
java -jar "$out/fifo-harness.jar"
