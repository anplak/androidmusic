# Future code improvements
This document enumerates prioritized, high-impact static-analysis issues identified by Detekt (and related lint findings) that were baselined during the initial enforcement work. These findings are intentionally deferred so the repo can be brought to a green CI state; they are grouped, prioritized, and include short remediation guidance and proposed PR scopes.

Status before this document
- Unit tests: passing (local `./gradlew test` succeeded).
- Spotless/ktlint: applied and passing after targeted suppressions for Compose idioms.
- Android Lint: historical findings baselined at `config/lint-baseline.xml`; critical runtime issues (Locale usage, SharedPreferences KTX, toUri usage) were fixed.

How to use this document
- Each bullet lists a rule/category, example files, why it matters, and a short remediation approach.
- I recommend implementing the suggested PRs in small, focused branches so changes are reviewable and easy to revert.

Priority 1 — High (refactor soon)
- LongMethod (detekt: LongMethod)
  - Example files: `MusicPlayerApp.kt` (many responsibilities; ~258 lines), `InsightsScreen.kt` (InsightsContent ~106 lines), `LibraryScreen.kt` (~121 lines).
  - Why: Large functions/composables are hard to review, test, and maintain; they hide logic that should be unit tested.
  - Remediation: Extract smaller composables / helper functions, move ViewModel logic out of UI, and keep composables focused on rendering only.
  - Proposed PRs: `refactor/ui-extract-musicplayerapp` (extract top-level tabs into separate composables), `refactor/ui-insights-split` (split InsightsContent into 2–3 composables + small helpers).

- TooManyFunctions (detekt: TooManyFunctions)
  - Example files: `PlaybackViewModel.kt` (18 functions), `LibraryViewModel.kt`, `PlaylistsViewModel.kt`.
  - Why: Very large ViewModels mix responsibilities (state + heavy logic) and are brittle.
  - Remediation: Move groups of related functions to helper classes/services (e.g., playback stats tracker, queue manager), or split ViewModel responsibilities into multiple ViewModels where appropriate.
  - Proposed PR: `refactor/viewmodel-playback-split` (introduce PlaybackController/PlaybackStats service and reduce PlaybackViewModel surface area).

- LongParameterList (detekt: LongParameterList)
  - Example files: `MusicPlayerApp.kt`, `MainTabsContent` (many parameters), `PlaylistDetailScreen.kt`
  - Why: Long parameter lists make callsites verbose and error-prone.
  - Remediation: Group related parameters into small data classes (e.g., a `UiCallbacks` data object), or hoist dependencies into ViewModels / CompositionLocal when appropriate.
  - Proposed PR: `refactor/introduce-ui-callbacks` (convert groups of callbacks into a single `UiActions` data class for large composables).

Priority 2 — Medium (improve design & safety)
- CyclomaticComplexMethod / ComplexCondition
  - Example files: `PlaybackViewModel.kt` (complex branching in tracking/finalizing session), other ViewModels.
  - Why: High complexity raises the chance of logic bugs and makes unit testing harder.
  - Remediation: Break conditions into named boolean helpers, extract branches into well-named functions, and add targeted unit tests for edge cases.
  - Proposed PR: `cleanup/playback-complex-conditions` (introduce helper methods and unit tests for session classification boundaries).

- MaxLineLength / Formatting
  - Example files: multiple long lines flagged by detekt/ktlint prior to Spotless fixes.
  - Why: Long lines reduce readability.
  - Remediation: Apply Spotless (already applied), and prefer wrapping or extracting expressions into locals.

Priority 3 — Low / Cosmetic (address over time)
- UnusedPrivateProperty, ReturnCount, MethodReturnCount
  - Example files: small occurrences across data & tests.
  - Why: Cleanup reduces noise and improves intent clarity.
  - Remediation: Remove dead properties, simplify methods, or add narrow suppressions with TODO comments linking to this document.

Security / Runtime-sensitive items (fix now or keep tracked)
- ConstantLocale (TimeFormatter) — fixed in this pass by avoiding static final SimpleDateFormat instances; keep tests around locale formatting changes.
- UseKtx (String.toUri / SharedPreferences.edit KTX) — applied KTX API usage where safe.

Testing & verification
- Unit tests were executed and passed locally (`./gradlew test` completed successfully).
- After each targeted change we re-ran `./gradlew :app:check` to validate formatting and lint baseline behavior.

Suggested short roadmap (next 4 PRs)
1. PR: `refactor/spotless-and-lint-baseline` — (already done locally) small hygiene changes, ensure lint baseline present and documented. (status: created locally, not committed)
2. PR: `refactor/ui-extract-musicplayerapp` — split MusicPlayerApp into smaller composables; reduce LongMethod and LongParameterList noise.
3. PR: `refactor/viewmodel-playback-split` — extract playback control and stats logic into testable classes, shrink PlaybackViewModel surface.
4. PR: `cleanup/detekt-highs` — tackle top 20 Detekt findings across files (lower priority problems and obvious refactors).

Notes and bookkeeping
- This file was generated from the current Detekt/Spotless/Lint outputs and the temporary baseline snapshot. The exact Detekt baseline XML could not be found in the repo at `config/detekt/detekt-baseline.xml`; if you prefer a machine-parsable source, run `./gradlew :app:detekt --debug` and capture XML/HTML reports (I can produce them on request).
- I did not commit any changes to the repository. All fixes and baseline wiring were applied locally per your instruction not to commit.

If you want, next I can:
- Create the four PR branches locally (no commits) and present the diffs for review, or
- Start implementing PR #2 (`refactor/ui-extract-musicplayerapp`) and produce a patch for review.

