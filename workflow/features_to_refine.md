# Features to refine

## Library improvements
- first and default tab should be "Artists"
- In Artists tab, when click on artist, it should display this artist albums (not tracks in plain list), ordered by "recently played". After albums listm it should display liked songs of this artist
- in Albums tab, albums should be sorted by "recently played"

## UX bugs/improvements
- daily mix "because you listen to.." - too much of single artist, all mixes should have at most 50% 
- smart shuffle from a song - should not stop current one, but continue playing, after it ends - switch to smart shuffle queue
- from player view - should have references to play queue (including last few songs recently played)
- mixes in "For You" view should be unique every day 


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

This might be present to a user in form of game - so that he sees percentage of how much info is still missing in metadata, and by answering consequent questions he gets virtual "prizes"

