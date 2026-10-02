### User story

As a user, I want library indexing to happen quietly with simple, contextual feedback so routine updates do not interrupt browsing with technical counts I cannot act on.

### Description

After every successful sync, `LibraryScreen` currently shows a prominent snackbar containing indexed and skipped counts for duration, folder, and artist rules. Scheduled background syncs make this message appear during normal browsing, and the terminology has little meaning without the Library Index context. This story makes routine success silent, keeps subtle progress and actionable failure feedback on Library, and explains exclusion rules where users can manage them.

**Estimate:** 1–2 days.

**Prerequisites:** stories **11** (background sync) and **12** (index exclusions).

### Acceptance criteria

- **AC1:** A successful scheduled/background sync does not show a snackbar or popup.
- **AC2:** While a non-initial sync is running, Library shows a subtle inline or linear progress indicator without blocking browsing.
- **AC3:** The initial scan with no cached tracks may keep a full loading state with plain wording such as **Finding music on this device…**.
- **AC4:** A sync failure shows a concise inline message and a **Retry** action; it does not expose an exception or raw technical text.
- **AC5:** Library Index explains which rules can exclude files (duration, folders, blocked artists) in the context where those rules can be changed.
- **AC6:** Removing the success popup does not suppress track-list updates or successful sync state transitions.
- **AC7:** Progress and failure feedback are announced accessibly without repeated announcements on recomposition.

### Functional requirements

- **FR1:** Remove the `scanSummary` success-snackbar effect from `LibraryScreen` and its technical `scan_summary` presentation.
- **FR2:** Keep sync lifecycle state separate from one-time UI messages:
  - no cache + running → initial loading;
  - cache available + running → non-blocking progress;
  - success → update content silently;
  - failure → inline retry state.
- **FR3:** Replace “Scanning/Indexing” jargon in the main Library surface with user-oriented wording such as **Updating library…** or **Finding music…**.
- **FR4:** Keep detailed rule descriptions in `LibraryIndexScreen`; clarify that excluded files will not appear in Library.
- **FR5:** If counts are retained, show them only inside Library Index under a clearly labeled **Last update** summary, not as an automatic overlay. This is optional and must not require persistence solely for this story.
- **FR6:** Retry must invoke the existing explicit sync path and clear the failure state when a new attempt starts.
- **FR7:** Delete obsolete strings/state plumbing when no longer used, while preserving `LibraryScanResult` data needed by synchronization logic or tests.

### Non-functional requirements

- **NFR1:** Feedback changes must not delay sync, list updates, or cached-library startup.
- **NFR2:** Do not add notification permissions, a foreground service, or a new persistence layer.
- **NFR3:** Progress UI must not cause the list to jump vertically when it appears or disappears; reserve space or overlay a slim indicator.
- **NFR4:** Avoid repeated snackbar/event delivery after rotation or recomposition.
- **NFR5:** Accessibility announcements should occur once per meaningful state transition and avoid reading numeric exclusion summaries automatically.

### UX design

- **Routine update:** silent completion; the refreshed list is the outcome.
- **In progress with cached content:** a thin progress line or compact status at the top of content, using subdued theme colors.
- **First scan:** centered progress with **Finding music on this device…**; avoid implementation terms such as MediaStore, rows, or index database.
- **Failure:** compact inline text **Couldn’t update library** with **Retry**. Existing music stays usable.
- **Library Index:** plain-language helper copy, for example **Files matching these rules are left out of your Library.**
- Do not use a modal dialog, toast, or success snackbar for scheduled work.

### Testing/validation strategy

- **ViewModel unit:** Running/Success/Failed transitions with and without cached tracks map to the intended UI state.
- **Compose/UI:** cached content remains visible during sync; progress is subtle; success creates no snackbar; failure exposes Retry.
- **Compose/UI:** progress appearance does not shift the first Library row or browse tabs.
- **E2E:** open Library, allow scheduled sync to complete, verify content updates and no `scan_summary` overlay appears.
- **E2E:** simulate sync failure, verify cached tracks remain usable, tap Retry, and verify recovery.
- **Accessibility:** inspect progress semantics/live-region behavior and ensure state changes are not announced repeatedly.
- **Regression:** first-run empty library, exclusion rules, Artist/Album tabs, and Library Index navigation continue to work.

### Out of scope

- Changing which files are indexed or how exclusion rules are evaluated.
- A detailed scan log, per-file error list, or diagnostics export.
- System notifications for library updates.
- User-configurable sync schedules.
- Reworking MediaStore permissions or the background synchronization architecture.

---

### Overall app state after this story

Library indexing is visible only when attention is useful: subtle progress during work, actionable feedback on failure, and silent routine success.

### Value added after this story

The Library feels calmer and more understandable while preserving synchronization reliability and control over exclusions.

---

# Implementation Plan

## Open questions for PO
- [ ] **Success counts — drop entirely or surface under Library Index "Last update"?** FR5 marks this optional. *Impact if unanswered:* we drop the counts from the UI entirely. `LibraryScanResult` stays in the data layer (needed by sync and `LibrarySyncCoordinatorTest`), but no persistence layer is added solely to display counts (NFR2).
- [ ] **First-scan wording — use "Finding music on this device…"?** AC3 allows the existing loading state with plainer wording. *Impact if unanswered:* we use exactly that string as the example given in the story.
- [ ] **Non-initial sync — keep text label next to the thin progress line?** UX section allows "a thin progress line or compact status". *Impact if unanswered:* thin `LinearProgressIndicator` only (already implemented), no additional text — quietest option, and the refresh affordance already exists via the top-bar action.

## Assumptions
- The existing `LinearProgressIndicator` (already shown while `isRefreshing` on a warm cache, `LibraryScreen.kt:150-157`) satisfies AC2. This story keeps it, adds a reserved-height wrapper so its appearance does not shift content (NFR3), and improves accessibility semantics.
- `LibrarySyncCoordinator` remains the single source of truth for sync state; no new channels, persistence, or notifications are introduced (NFR2).
- `LibraryScanResult` and its counter fields stay in `data/` — they are used by `MusicLibraryRepository.syncLibrary()` and referenced from `LibrarySyncCoordinatorTest`. FR7 requires deleting obsolete UI plumbing, not the domain type.
- Removing the success snackbar will not regress `LibraryIndexE2ETest` / `FavoritesE2ETest`, which call `waitForLibraryReindexSettled()`; the helper will be updated to wait for the quiet-state signal instead.
- No `LiveRegion`/custom semantics infrastructure exists in the codebase yet; we will add the minimum needed on the new indicator/error composables.
- Project is single-module, Kotlin + Jetpack Compose, no Maven; `java-lint` (Maven-only per `scripts/java-code-quality/java-lint.sh`) is not applicable here.

## Scope of change
**In scope:** removing the success snackbar + its state plumbing, adjusting progress/failure presentation on `LibraryScreen`, small copy updates on `LibraryIndexScreen`, string-resource cleanup, unit/E2E test updates.

**Out of scope:** sync logic, exclusion-rule evaluation, MediaStore querying, persistence for counts, any new notifications or foreground service.

**Affected areas:**
- `app/src/main/java/com/anplak/androidmusic/ui/LibraryScreen.kt`
- `app/src/main/java/com/anplak/androidmusic/ui/LibraryViewModel.kt`
- `app/src/main/java/com/anplak/androidmusic/ui/LibraryIndexScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/anplak/androidmusic/ui/LibraryViewModelTest.kt`
- `app/src/androidTest/java/com/anplak/androidmusic/ui/E2ETestHelpers.kt`
- `app/src/androidTest/java/com/anplak/androidmusic/ui/LibraryScreenE2ETest.kt` (add coverage for quiet-success behavior)

## Current state
- `LibraryViewModel` collects `syncCoordinator.syncState` (`LibraryViewModel.kt:119-133`). On `LibrarySyncState.Success` it (a) calls `assembler.updateSyncFlags(isRefreshing = false, syncFailed = false)` and (b) sets `_scanSummary.value = state.result`.
- `LibraryScreen.kt:88-101` observes `scanSummary` via `LaunchedEffect` and shows a snackbar built from `R.string.scan_summary`, then calls `viewModel.clearScanSummary()`. The snackbar is hosted on a `SnackbarHost` tagged `scan_summary` (`LibraryScreen.kt:127-132`).
- Non-blocking progress and failure already exist as state-driven UI:
  - `LinearProgressIndicator` while `state.isRefreshing` (warm-cache path only; cold-cache path is `LibraryUiState.Loading` → full-screen `LoadingState`).
  - `TextButton(R.string.library_sync_retry)` while `state.syncFailed` — this is a tap-target styled as a button, not an inline error message.
- `LibraryUiAssembler.updateSyncFlags` is the single writer of `isRefreshing`/`syncFailed`; `LibraryUiState.Content` carries them.
- `LibraryScreen.LoadingState()` (cold cache) uses `CircularProgressIndicator` + `R.string.scanning_library` ("Scanning library…") — jargon to replace (FR3).
- `LibraryIndexScreen` shows the rule lists but no plain-language explanation that excluded files disappear from Library (AC5, FR4).
- `E2ETestHelpers.waitForLibraryReindexSettled()` waits on test tag `scan_summary` (which this story deletes) — the helper must be updated.
- `LibraryViewModelTest."scan summary exposed when sync completes"` asserts `viewModel.scanSummary.value?.indexedCount == 1` — must be removed and replaced with a quiet-success assertion.

## Target design
Sync feedback is expressed **only** via `LibraryUiState.Content.isRefreshing` / `syncFailed` and the cold-cache `Loading` state. There is no parallel one-shot event channel for sync outcomes. Success is silent: the updated track list *is* the feedback. Failure is inline and actionable.

```
LibrarySyncCoordinator.syncState
            │
            ▼
   LibraryViewModel (collect)
            │  updateSyncFlags(isRefreshing, syncFailed)
            ▼
   LibraryUiAssembler ──► LibraryUiState
                              │           ┌─ Loading           → centered progress + "Finding music on this device…"
                              ▼           ├─ Content(isRefreshing=true)  → reserved-height slot with LinearProgressIndicator
                        LibraryScreen     ├─ Content(syncFailed=true)    → inline row: "Couldn't update library" + [Retry]
                                          └─ Content(success)            → nothing extra; list already updated
```

The reserved-height slot for the progress indicator is the mechanism that satisfies NFR3 (no vertical jump when it appears/disappears).

### Key decisions
| Decision | Chosen | Alternatives considered | Rationale |
|----------|--------|-------------------------|-----------|
| Where to express success feedback | Nowhere — rely on the list update itself | Keep snackbar but softer; toast; notification | AC1 + UX "Routine update: silent completion; the refreshed list is the outcome." Any transient popup violates the story. |
| How to remove the one-shot event | Delete `_scanSummary` / `clearScanSummary` / snackbar `LaunchedEffect` entirely | Keep event channel but skip display; convert to `SharedFlow` with `replay=0` | Dead state is worse than no state. `LibrarySyncState` already carries everything the UI needs; an extra channel is a source of recomposition bugs (NFR4). |
| Progress indicator layout | Reserved-height container (`Modifier.height(4.dp)`) that always exists; indicator only drawn while refreshing | Conditional insert of `LinearProgressIndicator`; overlay in `Box` | Conditional insertion is what causes the list-jump NFR3 forbids. Reserving the height keeps the layout stable without extra measurement passes. |
| Failure presentation | Compact inline row: `Text("Couldn't update library")` + trailing `TextButton("Retry")` | Keep single tap-to-retry button; snackbar; dialog | AC4 requires "concise inline message and a Retry action" — two distinct elements. Current single-button text does not read as a failure message. |
| First-scan wording | "Finding music on this device…" | "Scanning library…" (current); "Indexing…" | AC3 + FR3 explicitly name this copy and forbid implementation terms. |
| Library Index helper copy | One supporting line at the top of the screen: "Files matching these rules are left out of your Library." | Per-section captions; tooltip on each rule | Cheapest change that satisfies AC5/FR4 — one string, one `Text` composable, no restructuring of the existing `LazyColumn`. |
| Accessibility announcements | `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on the failure row only; progress indicator uses default progress semantics | `LiveRegionMode.Assertive`; announce every state change | NFR5 + AC7 require announcements on *meaningful transitions* and explicitly ban numeric summaries. A polite live region on the failure row announces the error once when it appears and once when it disappears. The progress indicator's built-in role is enough. |
| What to do with `LibraryScanResult` | Keep as-is in `data/` | Delete; rename; strip counter fields | Used by `MusicLibraryRepository.syncLibrary()` and asserted in `LibrarySyncCoordinatorTest`. FR7 explicitly preserves it. |
| Optional "Last update" counts under Library Index | Skip in this story | Implement behind `LibraryIndexUiState` | FR5 marks it optional; shipping it would require either new persistence (banned by NFR2) or process-lifetime state with little value. Defer to a follow-up if the PO wants it (Open question 1). |

## Implementation steps

1. **Delete the success-snackbar plumbing** — `app/src/main/java/com/anplak/androidmusic/ui/LibraryViewModel.kt`
   Remove `_scanSummary`, `scanSummary`, and `clearScanSummary()` (lines 77-78, 227-229). In the `syncCoordinator.syncState.collect` block, drop the `_scanSummary.value = state.result` line on `Success` (line 127); keep only the `updateSyncFlags` call. Remove the now-unused `LibraryScanResult` import.
   *Why:* this is the entire one-shot success-event channel; deleting it guarantees AC1 by construction rather than by convention.

2. **Remove snackbar UI from LibraryScreen** — `app/src/main/java/com/anplak/androidmusic/ui/LibraryScreen.kt`
   Delete: the `scanSummary` collect (line 73), the `LaunchedEffect(scanSummary)` block (lines 88-101), the `snackbarHostState` and the `snackbarHost` slot on the `Scaffold` (lines 74, 127-132). Drop unused imports (`SnackbarHost`, `SnackbarHostState`, `LocalContext`, `LibraryScanResult`-adjacent imports).

3. **Reserve space for the refresh indicator** — `LibraryScreen.kt`
   Replace the conditional `if (state.isRefreshing) { LinearProgressIndicator(...) }` with a fixed-height slot that always occupies the same vertical space:

   ```kotlin
   Box(
       modifier = Modifier
           .fillMaxWidth()
           .height(4.dp)
           .testTag("library_refresh_slot"),
   ) {
       if (state.isRefreshing) {
           LinearProgressIndicator(modifier = Modifier.fillMaxSize().testTag("library_refresh_indicator"))
       }
   }
   ```

   The slot's tag lets UI tests assert "content did not move" by comparing the `track_list` top offset before/after sync starts.

4. **Replace retry button with an inline failure row** — `LibraryScreen.kt`
   Replace the current `if (state.syncFailed) { TextButton(...) }` block (lines 158-165) with a compact row:

   ```kotlin
   if (state.syncFailed) {
       Row(
           modifier = Modifier
               .fillMaxWidth()
               .padding(horizontal = 16.dp, vertical = 4.dp)
               .semantics { liveRegion = LiveRegionMode.Polite }
               .testTag("library_sync_failed"),
           verticalAlignment = Alignment.CenterVertically,
           horizontalArrangement = Arrangement.SpaceBetween,
       ) {
           Text(
               text = stringResource(R.string.library_sync_failed),
               style = MaterialTheme.typography.bodyMedium,
               color = MaterialTheme.colorScheme.error,
           )
           TextButton(
               onClick = { viewModel.refresh() },
               modifier = Modifier.testTag("library_sync_retry"),
           ) {
               Text(stringResource(R.string.retry))
           }
       }
   }
   ```

   The existing `library_sync_retry` test tag is preserved so current tests keep working. The new `library_sync_failed` wrapper carries the polite live region — TalkBack announces it once on appearance, once on disappearance (AC7, NFR5).

5. **Update first-scan copy** — `LibraryScreen.kt` `LoadingState()` + `strings.xml`
   Change `LoadingState` to use a new string `R.string.finding_music` instead of `R.string.scanning_library`. Keep the existing `loading_state` / `loading_indicator` test tags.

6. **Add plain-language helper on Library Index** — `LibraryIndexScreen.kt`
   Insert a single supporting `Text` as the first `item` of the `LazyColumn`, before the "Max track duration" item:

   ```kotlin
   item {
       Text(
           text = stringResource(R.string.library_index_helper),
           style = MaterialTheme.typography.bodyMedium,
           color = MaterialTheme.colorScheme.onSurfaceVariant,
           modifier = Modifier.testTag("library_index_helper"),
       )
   }
   ```

7. **String resources** — `app/src/main/res/values/strings.xml`
   - Add `<string name="finding_music">Finding music on this device…</string>`
   - Add `<string name="library_sync_failed">Couldn\'t update library</string>`
   - Add `<string name="retry">Retry</string>`
   - Add `<string name="library_index_helper">Files matching these rules are left out of your Library.</string>`
   - Delete `scan_summary` (line 143) — no longer referenced
   - Delete `scanning_library` (line 27) — replaced by `finding_music`
   - Keep `library_sync_retry` (line 28) if still referenced; otherwise delete and use the new pair `library_sync_failed` + `retry`.

8. **Update unit tests** — `app/src/test/java/com/anplak/androidmusic/ui/LibraryViewModelTest.kt`
   - Delete the `"scan summary exposed when sync completes"` test (lines 283-293).
   - Add `"successful sync does not expose a scan summary event"` asserting the ViewModel has no `scanSummary` property (compile-time guarantee) — practically, replace with: after sync completes on a warm cache, `uiState.value` is `Content` with `isRefreshing == false` and `syncFailed == false`. This covers AC6.
   - Keep `sync failure keeps cache visible with retry flag` — already covers AC4's ViewModel half.
   - Add `"retry clears failure flag and restarts sync"`: set `shouldFailSync = true`, observe `syncFailed == true`, flip the fake to succeed, call `viewModel.refresh()`, assert `syncFailed == false` and `syncLibraryCallCount` incremented (covers FR6).

9. **Update E2E helpers and tests** — `E2ETestHelpers.kt`, `LibraryScreenE2ETest.kt`
   - `E2ETestHelpers.waitForLibraryReindexSettled()`: drop the `scan_summary` branch; wait for `track_list` OR `empty_state` OR `library_sync_failed`. This matches the new quiet terminal states.
   - Add a new E2E in `LibraryScreenE2ETest`: `"backgroundSync does not show success snackbar"` — open Library on a warm cache, trigger sync via tab re-visit, wait for settle, assert no node with text matching the old `scan_summary` pattern and no snackbar semantics present.
   - Add a layout-stability assertion: capture `track_list` bounds before triggering a background sync, assert the same bounds after `library_refresh_indicator` appears (NFR3).

10. **README touch-up** — `README.md`
    The "Features → Library & playback" section already describes cached display; no change required. Confirm no stale mention of "scan counts" or the snackbar remains.

## Key code snippets

**ViewModel — collapsed sync-state collection** (`LibraryViewModel.kt`):

```kotlin
viewModelScope.launch {
    syncCoordinator.syncState.collect { state ->
        when (state) {
            LibrarySyncState.Idle -> Unit
            LibrarySyncState.Running ->
                assembler.updateSyncFlags(isRefreshing = true, syncFailed = false)
            is LibrarySyncState.Success ->
                assembler.updateSyncFlags(isRefreshing = false, syncFailed = false)
            is LibrarySyncState.Failed ->
                assembler.updateSyncFlags(isRefreshing = false, syncFailed = true)
        }
    }
}
```

`LibraryScanResult` is still produced by the coordinator; we just don't surface it to the UI. No other code path changes.

**LibraryScreen — content branch skeleton** (progress slot + failure row):

```kotlin
is LibraryUiState.Content -> {
    Column(modifier = Modifier.fillMaxSize()) {
        LibraryBrowseTabs(selected = state.browseTab, onTabSelected = viewModel::setBrowseTab)
        LibraryRefreshSlot(isRefreshing = state.isRefreshing)
        if (state.syncFailed) {
            LibrarySyncFailedRow(onRetry = viewModel::refresh)
        }
        when (state.browseTab) { /* unchanged */ }
    }
}
```

`LibraryRefreshSlot` and `LibrarySyncFailedRow` are small private composables at the bottom of `LibraryScreen.kt`, matching the file's existing pattern (`LoadingState`, `EmptyLibraryState`, …).

**Accessibility — failure row semantics:**

```kotlin
Modifier.semantics { liveRegion = LiveRegionMode.Polite }
```

Applied once on the row's container. No `paneTitle`, no custom `contentDescription` for the counts (there are none anymore) — this is the minimum needed to satisfy AC7/NFR5 without repeated announcements on recomposition.

## Data & contract changes
None. No DB migrations, no API changes, no message-contract changes. `LibraryScanResult` keeps its current shape and remains the return type of `MusicLibraryRepository.syncLibrary()`.

## Non-functional considerations
- **Performance (NFR1):** no new I/O; the change removes a `LaunchedEffect` + snackbar render. The reserved-height slot costs one `Box` measure per recomposition — negligible.
- **Security / authz:** not applicable; no new permissions, no new data exposure.
- **Error handling (AC4, FR6):** failure state continues to come from `LibrarySyncState.Failed`. `viewModel.refresh()` already calls `assembler.updateSyncFlags(isRefreshing = true, syncFailed = false)` before `syncNow()`, so the retry action clears the failure row optimistically — verified by the new unit test in step 8.
- **Observability:** no new logging required; existing sync failure paths are unchanged. The story intentionally removes user-facing numeric diagnostics, not internal ones.
- **Concurrency:** no change to `LibrarySyncCoordinator`'s mutex/in-flight logic. Debounce behavior in `onLibraryVisible` is preserved.
- **i18n:** all new copy goes through `strings.xml`; plural rules not needed (no counts in user-facing strings anymore).
- **a11y (AC7, NFR5):** polite live region on the failure row only; progress uses the framework's built-in `ProgressBar` role; no announcements on routine success.

## Test scenarios
- **Unit (`LibraryViewModelTest`):**
  - Cold cache + running → `Loading` is emitted and stays until sync completes.
  - Warm cache + running → `Content(isRefreshing = true)` and track list unchanged.
  - Success on warm cache → `Content(isRefreshing = false, syncFailed = false)`, no extra event exposed.
  - Failure on warm cache → `Content(syncFailed = true)` and cached tracks remain.
  - `refresh()` after failure → `syncFailed` flips to `false` and `syncLibraryCallCount` increments.
- **Integration:** not applicable — no repository/data-source boundary is touched.
- **E2E / manual:**
  - Background sync completes → no snackbar/popup, track list reflects new content.
  - Sync failure → inline "Couldn't update library" + Retry visible, cached list still scrollable; tapping Retry clears the row and re-syncs.
  - First launch with empty cache → full-screen "Finding music on this device…" until first sync finishes.
  - Library Index → helper line visible at the top of the screen.
- **Edge cases & failures:**
  - Rotation during an active sync → no duplicate announcements, no re-show of any transient message (NFR4).
  - Rapid tab switches during sync → progress indicator appears/disappears without list jump (NFR3); no crash (covered by existing `tabSwitchStress_noCrash`).
  - Sync fails, then succeeds on retry → failure row appears once, disappears once (live region announces each transition exactly once).

## Verification steps
Run from repo root (`/Users/aplakhotnyi/anplak-repo/androidmusic`).

- [ ] `./gradlew :app:assembleDebug` — compile
- [ ] `./gradlew :app:test` — full unit test suite
- [ ] `./gradlew :app:test --tests "com.anplak.androidmusic.ui.LibraryViewModelTest"` — targeted ViewModel tests
- [ ] `./gradlew spotlessApply` — auto-format (ktlint)
- [ ] `./gradlew :app:spotlessCheck` — verify formatting
- [ ] `./gradlew :app:detekt` — Kotlin static analysis (fails on findings per `app/build.gradle.kts`)
- [ ] `./gradlew :app:lint` — Android Lint (abortOnError=true, warningsAsErrors=true)
- [ ] `./gradlew :app:check` — aggregate gate (depends on spotlessCheck, detekt, lint)
- [ ] `./scripts/run-e2e-wifi.sh` — on-device E2E (requires connected device; covers the updated `waitForLibraryReindexSettled` paths)
- [ ] `./gradlew :app:connectedDebugAndroidTest --tests "*MusicLibraryIntegrationTest*"` — MediaStore integration smoke (optional for this story; run if a device is attached)
- [ ] `java-lint`: not applicable — this is a Gradle/Kotlin Android project, no Maven modules (`scripts/java-code-quality/java-lint.sh` is Maven-only)

## Risks & rollback
| Risk | Likelihood/Impact | Mitigation |
|------|-------------------|------------|
| E2E tests in `LibraryIndexE2ETest` / `FavoritesE2ETest` break because `waitForLibraryReindexSettled` no longer sees `scan_summary` | Medium / Medium | Update the helper in the same change; helper's contract is "sync has settled", not "snackbar shown". New terminal states cover all existing call sites. |
| TalkBack users miss the silent success | Low / Low | That is the intended behavior per AC1/AC7; meaningful transitions (failure) are still announced. |
| Removing `scanSummary` breaks an obscure consumer | Low / Low | `grep` shows exactly three consumers: `LibraryScreen`, `LibraryViewModelTest`, `E2ETestHelpers`. All three are updated in this plan. |
| Reserved-height slot adds a thin visual strip when not refreshing | Low / Low | 4.dp transparent `Box` matches the indicator's height; visually indistinguishable from the existing layout in Material 3's default spacing. If it bothers design, reduce to 2.dp — does not change behavior. |
| First-scan string change breaks an existing E2E assertion | Low / Low | No existing test asserts on `"Scanning library…"` text; tests assert on the `loading_indicator` tag, which is unchanged. |

**Rollback:** revert the single commit. The change is confined to UI + ViewModel plumbing and does not touch `LibrarySyncCoordinator`, the repository, or the schema, so rollback cannot strand persisted state. No feature flag needed — the behavior change is the feature.

## Dependencies
No new dependencies.

