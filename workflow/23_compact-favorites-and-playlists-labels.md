### User story

As a user, I want short, readable bottom-navigation labels so Favorites and Playlists do not wrap or compete for space on a phone.

### Description

The five-item bottom navigation gives each destination limited width. **Favorites** and **Playlists** are the longest labels and can clip or wrap at increased font scale. This story uses concise visible labels—**Liked** and **Lists**—while retaining the full product terms **Favorites** and **Playlists** in screen copy, empty states, and accessibility descriptions.

**Estimate:** 1 day.

**Prerequisite:** story **18** (navigation chrome and the short Library label).

### Acceptance criteria

- **AC1:** Bottom navigation displays **Liked** for Favorites and **Lists** for Playlists.
- **AC2:** All five visible navigation labels remain on one line at 320 dp width with font scale 1.3.
- **AC3:** TalkBack announces the full destination names **Favorites** and **Playlists**, including selected state.
- **AC4:** Empty states, actions, detail screens, and section headings continue to use the unambiguous full terms where space permits.
- **AC5:** Tab identity, order, icons, selected state, and navigation behavior do not change.
- **AC6:** Search's **Playlists** result section and all “Add to playlist” actions retain existing wording.

### Functional requirements

- **FR1:** Add dedicated short-label resources for bottom navigation rather than globally replacing `favorites` and `playlists`.
- **FR2:** Extend `NavigationTab` metadata if needed so visible label and accessibility destination name can use different resources.
- **FR3:** Map only the Favorites and Playlists navigation items to **Liked** and **Lists**; other labels remain **For You**, **Library**, and **History**.
- **FR4:** Preserve existing navigation test tags and destination routing.
- **FR5:** Ensure label text is constrained to one line without reducing font size below the Material navigation typography.

### Non-functional requirements

- **NFR1:** Support phone widths down to 320 dp and system font scale up to 1.3 without clipping or overlap.
- **NFR2:** Maintain minimum 48 dp touch targets for every navigation item.
- **NFR3:** Do not introduce custom text autosizing, horizontal scrolling, or a new navigation dependency.
- **NFR4:** All new visible and accessibility text must be localizable; do not derive accessibility wording from abbreviated labels.

### UX design

- Visible navigation vocabulary:
  - **For You**
  - **Library**
  - **Liked**
  - **Lists**
  - **History**
- Keep the heart icon for **Liked** and queue/music-list icon for **Lists**, so the short text and icon reinforce one another.
- Use the full names when the UI has room: **No favorites yet**, **Smart Playlists**, **Your Playlists**, and **Add to playlist**.
- Do not solve fitting by shrinking one label independently or using unclear truncation such as “Favor…” / “Playli…”.

### Testing/validation strategy

- **Compose/UI:** assert the five visible labels and unchanged navigation test tags.
- **Layout:** screenshot or manual validation at 320 dp and 360 dp widths with font scales 1.0 and 1.3.
- **Accessibility:** inspect semantics or use TalkBack to confirm full names and selected state.
- **Regression:** navigate through all five tabs; Favorites and Playlists content, search labels, and playlist actions keep their full wording.
- **Localization readiness:** verify the short and full resources are independent and no content description reads “Lists” where “Playlists” is intended.

### Out of scope

- Changing navigation icons, order, or destinations.
- Combining Favorites and Playlists into one tab.
- Hiding labels entirely or replacing bottom navigation with a rail/drawer.
- Renaming domain models, database tables, classes, or analytics identifiers.
- Rewriting playlist and favorite screen content.

---

### Overall app state after this story

The five-tab navigation remains readable on compact screens while content and accessibility retain precise destination names.

### Value added after this story

Users get stable one-line navigation labels without sacrificing clarity elsewhere in the app.
