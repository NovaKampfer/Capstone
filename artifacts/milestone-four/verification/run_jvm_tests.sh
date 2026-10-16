#!/usr/bin/env bash
# Supplemental JVM verification; does not replace the Android Gradle build.
set -euo pipefail
if [[ $# -ne 2 ]]; then
  echo "Usage: bash verification/run_jvm_tests.sh /path/to/kotlin-jars /path/to/room-jars" >&2
  exit 2
fi
deps="$(cd "$1" && pwd)"
room="$(cd "$2" && pwd)"
root="$(cd "$(dirname "$0")/.." && pwd)"
classes="$(mktemp -d)"
main="$root/Enhanced/app/src/main/java/com/MatthewBaerg/multitasked"
tests="$root/Enhanced/app/src/test/java/com/MatthewBaerg/multitasked/ui/tasks"
classpath="$deps/kotlin-stdlib-2.0.21.jar:$deps/kotlinx-coroutines-core-jvm-1.9.0.jar:$deps/kotlinx-coroutines-test-jvm-1.9.0.jar:$deps/annotations-13.0.jar:$deps/javax.inject-1.jar:$deps/junit-4.13.2.jar:$deps/hamcrest-core-1.3.jar:$deps/firestore/classes.jar"
classpath="$classpath:$room/room-common.jar:$room/room-runtime/classes.jar:$room/sqlite/classes.jar:$room/android-api.jar"
sources=("$main/data/model/TaskItem.kt" "$main/data/model/Board.kt" "$main/data/model/User.kt" "$main/di/SortingDispatcher.kt")
for name in BoardDetailContract TaskSortPolicy TaskUrgency TaskPriorityQueue TaskMergeSorter TaskClock TaskSortingService; do
  sources+=("$main/ui/tasks/$name.kt")
done
sources+=("$main"/data/local/*.kt "$main/ui/local/LocalDateInput.kt" "$root/Enhanced/app/src/test/java/com/MatthewBaerg/multitasked/ui/local/LocalDateInputTest.kt")
suites=()
for name in TaskMergeSorterTest TaskPriorityQueueTest TaskUrgencyTest TaskSortingServiceTest TaskSortPolicyTest BoardDetailUiStateTest; do
  sources+=("$tests/$name.kt")
  suites+=("com.MatthewBaerg.multitasked.ui.tasks.$name")
done
suites+=("com.MatthewBaerg.multitasked.ui.local.LocalDateInputTest")
echo "Compiling production Kotlin algorithms and coroutine service with Kotlin 2.0.21 (JVM target 11)"
java -cp "$deps/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -jvm-target 11 -classpath "$classpath" \
  -d "$classes" "${sources[@]}"
echo "Room data sources type-checked against official API jars (no annotation processing or Android execution)"
echo "Running seven JUnit suites against the compiled production sources"
java -cp "$classes:$classpath" org.junit.runner.JUnitCore "${suites[@]}"
echo "Temporary compiled classes: $classes"
