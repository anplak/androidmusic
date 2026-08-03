# Features to refine

## Look and feel — refined
Split into workflow stories (2–3 days each):
- `17_persistent-mini-player.md` — global mini player (title, Play, Like) from any screen
- `18_navigation-chrome-and-header-density.md` — remove duplicated tab titles; “Library” label; denser chrome
- `19_dark-geek-theme.md` — dark geek-minimal design system
- `20_media-artwork-and-app-icon.md` — MediaStore artwork + styled fallbacks; music-meaningful launcher icon

## Search and playlist management — refined
Split into workflow stories (no more than 2–3 days each):
- `21_collection-aware-library-search.md` — return relevant Tracks, Artists, and Albums and open collection results directly
- `22_add-collections-to-playlists.md` — add all tracks from an Artist or Album detail screen to a playlist
- `23_compact-favorites-and-playlists-labels.md` — use concise bottom-navigation labels while retaining clear full destination names
- `24_quiet-library-indexing-feedback.md` — replace the technical indexing result popup with quiet, contextual progress and outcomes

## UX bugs/improvements
- mini player not visible (check if merged)
- daily mix "because you listen to.." - too much of single artist, all mixes should have at most 50% 
- smart shuffle from a song - should not stop current one
- from player view - should be references to play queue (including last few songs recently played)

## AppFunctions integration
use android integration with gemini to run app intents in mcp style
see AppFunctions Jetpack library
this allows running commands by voice control

## Ask user if some metadata is missing
Some tracks and albums are missing year of issue, genre, language and other data, which excludes them from daily mixes
Add possibility for app user to add missing data by asking him directly: 
- Which decade is this album from?
- which genre is that artist (choose from list or type)
- from which country that artist originated from?
Those metadata can be used to generate better mixes

