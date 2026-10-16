# Supplemental JVM verification

Preferred verification is the normal Android Gradle build described in the package README. `run_jvm_tests.sh` is a supplemental fallback that was actually executed for this package. It does not compile the Compose screen, ViewModel, or Hilt module.

Run with Bash and JDK 17:

```bash
bash verification/run_jvm_tests.sh /absolute/path/to/dependency-jars /absolute/path/to/room-jars
```

The dependency directory needs these official artifacts, with the filenames shown:

- `org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21`
- `org.jetbrains.kotlin:kotlin-stdlib:2.0.21`
- `org.jetbrains.kotlin:kotlin-script-runtime:2.0.21`
- `org.jetbrains.kotlin:kotlin-reflect:1.6.10` (compiler runtime dependency)
- `org.jetbrains.kotlin:kotlin-daemon-embeddable:2.0.21`
- `org.jetbrains.intellij.deps:trove4j:1.0.20200330`
- `org.jetbrains:annotations:13.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:1.9.0`
- `javax.inject:javax.inject:1`
- `junit:junit:4.13.2`
- `org.hamcrest:hamcrest-core:1.3`
- `com.google.firebase:firebase-firestore:26.0.2` (AAR; extract its `classes.jar` into `firestore/classes.jar` under the dependency directory)

JAR filenames follow `artifact-version.jar`. Retrieve Kotlin/JUnit dependencies from Maven Central and Firebase from Google's Maven repository. Dependency binaries are not bundled. The script leaves its compiled classes in a newly created temporary directory and prints that directory for inspection. It does not edit project source or contact Firebase.

The second directory contains `room-common.jar` from androidx.room:room-common:2.6.1; `room-runtime/classes.jar` extracted from androidx.room:room-runtime:2.6.1; `sqlite/classes.jar` extracted from androidx.sqlite:sqlite:2.4.0; and `android-api.jar` from com.google.android:android:4.1.1.4. The last jar supplies referenced Android type signatures only; it is not a target SDK or usable Android runtime. The script type-checks the database sources and runs 43 JVM tests, without Room annotation processing, UI compilation, or Android execution.
