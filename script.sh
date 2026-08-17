#!/usr/bin/env bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEST_DIR="$ROOT_DIR/tests"
OUT_DIR="$ROOT_DIR/output"
SOOT_OUT_DIR="$ROOT_DIR/sootOutput"
SRC_DIR="$ROOT_DIR/src/main/java"
BUILD_DIR="$ROOT_DIR/target/classes"
SOOT_JAR="$ROOT_DIR/lib/soot-4.6.0-jar-with-dependencies.jar"
BUILD_CP="$SRC_DIR:$SOOT_JAR"
TOOL_CP="$ROOT_DIR/target/classes:$SOOT_JAR"
LEVEL="${1:-${LEVEL:-1}}"

count_virtual_callsites() {
  local class_dir="$1"
  local total=0
  local class_list

  class_list="$(find "$class_dir" -name '*.class' | sort | while IFS= read -r class_file; do
    class_name="${class_file#$class_dir/}"
    class_name="${class_name%.class}"
    class_name="${class_name//\//.}"
    printf '%s\n' "$class_name"
  done)"

  while IFS= read -r class_file; do
    local class_name
    local class_count

    class_name="${class_file#$class_dir/}"
    class_name="${class_name%.class}"
    class_name="${class_name//\//.}"

    class_count="$(javap -classpath "$class_dir" -c "$class_name" 2>/dev/null | awk -v class_list="$class_list" '
      BEGIN {
        split(class_list, classes, "\n")
        for (i in classes) {
          if (classes[i] != "") {
            app_classes[classes[i]] = 1
          }
        }
      }
      /^[[:space:]]*[[:alnum:]_$<>\[\].,[:space:]]+\)[[:space:]]*;[[:space:]]*$/ {
        current_method = $0
      }
      /invokevirtual|invokeinterface/ {
        if (current_method ~ /_static\(/) {
          next
        }

        target = $0
        sub(/^.*\/\/ Method /, "", target)
        sub(/[:(].*$/, "", target)
        gsub("/", ".", target)
        sub(/\.[^.]+$/, "", target)

        if (target in app_classes) {
          count++
        }
      }
      END { print count + 0 }
    ')"
    total=$((total + class_count))
  done < <(find "$class_dir" -name '*.class' | sort)

  printf '%s\n' "$total"
}

count_transformed_callsites() {
  local class_dir="$1"
  local total=0

  while IFS= read -r class_file; do
    local class_name
    local class_count

    class_name="${class_file#$class_dir/}"
    class_name="${class_name%.class}"
    class_name="${class_name//\//.}"

    class_count="$(javap -classpath "$class_dir" -c "$class_name" 2>/dev/null | awk '
      /^[[:space:]]*[[:alnum:]_$<>\[\].,[:space:]]+\)[[:space:]]*;[[:space:]]*$/ {
        current_method = $0
      }
      /invokestatic/ {
        if (current_method ~ /_static\(/) {
          next
        }
        if ($0 ~ /_static[:(]/) {
          count++
        }
      }
      END { print count + 0 }
    ')"
    total=$((total + class_count))
  done < <(find "$class_dir" -name '*.class' | sort)

  printf '%s\n' "$total"
}

echo "Cleaning old files..."
rm -rf "$ROOT_DIR/target" "$SOOT_OUT_DIR" "$OUT_DIR"
find "$TEST_DIR" -name '*.class' -delete
mkdir -p "$OUT_DIR"

echo "Building optimizer..."
mkdir -p "$BUILD_DIR"
javac -cp "$BUILD_CP" -d "$BUILD_DIR" $(find "$SRC_DIR" -name '*.java')

for test_file in "$TEST_DIR"/Test*.java; do
  test_name="$(basename "$test_file" .java)"
  original_dir="$OUT_DIR/$test_name/original"
  transformed_dir="$OUT_DIR/$test_name/transformed"

  mkdir -p "$original_dir" "$transformed_dir"

  echo
  echo "===== $test_name ====="
  echo "Compiling testcase..."
  javac -d "$original_dir" "$test_file"

  echo "Running original program..."
  start=$(date +%s%N)
  java -Xint -cp "$original_dir" "$test_name" > /dev/null
  end=$(date +%s%N)
  orig_time_ms=$(( (end - start)/1000000 ))

  echo "Running optimizer..."
  rm -rf "$SOOT_OUT_DIR"
  java -cp "$TOOL_CP" devirtualizer.Main -d "$original_dir" -c "$test_name" -l "$LEVEL"

  if [ -d "$SOOT_OUT_DIR" ]; then
    cp -r "$SOOT_OUT_DIR"/. "$transformed_dir"/
  fi

  echo "Running transformed program..."
  start=$(date +%s%N)
  java -Xint -cp "$transformed_dir" "$test_name" > /dev/null
  end=$(date +%s%N)
  trans_time_ms=$(( (end - start)/1000000 ))


  if [ "$orig_time_ms" -gt 0 ]; then
    improvement=$(awk "BEGIN {printf \"%.2f\", (($orig_time_ms - $trans_time_ms) / $orig_time_ms) * 100}")
  else
    improvement="0.00"
  fi

  echo "Original time: ${orig_time_ms} ms"
  echo "Transformed time: ${trans_time_ms} ms"
  echo "Improvement: ${improvement}%"
done
