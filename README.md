# Matthew Baerg — CS 499 ePortfolio

MultiTasked demonstrates software design, algorithms and databases in one Kotlin Android application.

Portfolio: https://novakampfer.github.io/Capstone/

## Layout
- `docs/`: dependency-free static portfolio, narratives, publication ZIPs and evidence.
- `artifacts/milestone-two`, `milestone-three`, `milestone-four`: sanitized historical packages.
- `artifacts/final`: separate final application with narrowly scoped test/UI repairs and generated Room schema.

## Preview and verify
Run `python -m http.server 8000 --directory docs`, then open http://localhost:8000/.
Run `python tools/check_site.py` to validate local links, fragments, downloads and credential exclusions.
No framework build is needed. GitHub Actions deploys only `docs/`.

## Android reproduction
Use Android Studio's JDK (17+), SDK 36 and the supplied Gradle wrapper. Add your own app/google-services.json privately; public downloads intentionally exclude it. Run `gradlew assembleDebug testDebugUnitTest`, then `gradlew connectedDebugAndroidTest` with an emulator. Do not commit Firebase configuration, SDK paths, signing keys or runtime databases. A build with configuration does not imply cloud authorization or shared-workspace validation.

## Before instructor submission
The actual code-review recording is embedded at https://novakampfer.github.io/Capstone/#review with a direct MP4 link, captions and transcript. Review the complete audio and first-person assessment. The five-outcome rubric has been audited; Outcome 1 still needs stronger genuine collaboration evidence. The original plan proposed synchronization/account isolation that the delivered separate Room workspace does not provide. Full Firebase interaction and wider UI checks remain separate from the passing automated tests.

## Publishing
The designated repository is NovaKampfer/Capstone. The user authorized making the initially empty private repository public for Pages. The workflow uses the official GitHub Pages actions. Existing custom domains must be preserved on future changes.
Official reference: https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages

## Authorship
Matthew Baerg supplied the artifact, milestone narratives, journals and instructor feedback transcription. AI assisted portfolio preparation, source inspection, test repairs and verification. No broader software license is newly assigned.
