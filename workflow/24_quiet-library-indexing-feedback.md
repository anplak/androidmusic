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
