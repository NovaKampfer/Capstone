# Exported Room schemas

The Gradle kapt configuration exports Room's generated schema JSON here when the Android project compiles. Commit the generated `com.MatthewBaerg.multitasked.data.local.AppDatabase/2.json` after a successful build.

No generated schema JSON is claimed in this package because Room annotation processing could not run in the preparation environment. The explicit version-one SQL fixture is in `src/androidTest/assets/schema-v1.sql`; it is a test fixture, not a Room compiler export. The instrumentation migration test creates that legacy database and opens it with the version-two Room implementation so Room itself validates the upgraded schema.
