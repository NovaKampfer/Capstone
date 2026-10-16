# Repeatable checks

From the package root, run the supplemental SQL checks with Python 3:

```bash
python3 verification/test_sql_contract.py
```

No third-party Python modules are required. The script extracts the query strings and migration statements from the Kotlin files and uses the version-one SQL fixture in androidTest/assets. It tests SQLite behavior, not the generated Room implementation.

See KOTLIN_DEPENDENCIES.md to reproduce the 43-test JVM run and Kotlin data-layer type-check. For actual Android and Room integration validation, use the Gradle commands in the main README. Read Evidence/VALIDATION.md before interpreting the results.
