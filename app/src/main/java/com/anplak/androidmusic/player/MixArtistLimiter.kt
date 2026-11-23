package com.anplak.androidmusic.player

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object MixArtistLimiter {
    fun limit(
        preferred: List<TrackInfo>,
        candidates: List<TrackInfo>,
        limit: Int,
        playlistIdsByTrack: Map<Long, Set<Long>> = emptyMap(),
        randomTierOrder: List<TrackInfo> = candidates,
        zone: ZoneId = ZoneId.systemDefault(),
        leadArtist: String? = null,
    ): List<TrackInfo> {
        if (limit <= 0 || (preferred.isEmpty() && candidates.isEmpty())) return emptyList()
        if (limit == 1) {
            val only = preferred.firstOrNull() ?: candidates.firstOrNull()
            return listOfNotNull(only)
        }

        val artistSource = candidates.ifEmpty { preferred }
        if (namedArtistCount(artistSource) <= 1) {
            return spreadAlbums((preferred + candidates).distinctBy { it.id }, limit)
        }

        val selection = Selection(limit = limit, provisional = limit / 2)
        val leadKey = artistKey(leadArtist.orEmpty())
        if (leadKey != null) {
            fillLead(selection, preferred, leadKey)
        }
        var pass = 1
        while (selection.chosen.size < limit) {
            val sizeBeforePass = selection.chosen.size
            fillPass(selection, preferred, candidates, playlistIdsByTrack, randomTierOrder, zone, pass)
            if (selection.chosen.size == sizeBeforePass) break
            pass++
        }
        return interleave(shrinkToCap(selection.chosen), leadKey)
    }

    internal fun artistKey(artist: String): String? = artist.trim().takeIf { it.isNotEmpty() }?.lowercase()

    internal fun maxShare(size: Int): Int = if (size <= 1) size else size / 2

    private fun albumKey(track: TrackInfo): String? {
        track.albumId?.let { albumId -> return "id:$albumId" }
        val title = track.album.trim().lowercase()
        if (title.isEmpty()) return null
        val artist = artistKey(track.artist).orEmpty()
        return "$artist\u0000$title"
    }

    private fun namedArtistCount(tracks: List<TrackInfo>): Int = tracks.mapNotNull { artistKey(it.artist) }.distinct().size

    private fun spreadAlbums(
        tracks: List<TrackInfo>,
        limit: Int,
    ): List<TrackInfo> {
        val chosen = ArrayList<TrackInfo>(minOf(limit, tracks.size))
        val chosenIds = HashSet<Long>()
        while (chosen.size < limit) {
            val sizeBeforePass = chosen.size
            val albumsThisPass = HashSet<String>()
            for (track in tracks) {
                if (chosen.size >= limit) break
                if (track.id in chosenIds) continue
                val album = albumKey(track)
                if (album != null && !albumsThisPass.add(album)) continue
                chosen += track
                chosenIds += track.id
            }
            if (chosen.size == sizeBeforePass) break
        }
        return chosen
    }

    private fun fillLead(
        selection: Selection,
        preferred: List<TrackInfo>,
        leadKey: String,
    ) {
        val leadTracks = preferred.filter { artistKey(it.artist) == leadKey }
        while (selection.chosen.size < selection.limit && selection.artistCount(leadKey) < selection.provisional) {
            val sizeBeforePass = selection.chosen.size
            val albumsThisPass = HashSet<String>()
            for (track in leadTracks) {
                val roomLeft =
                    selection.chosen.size < selection.limit && selection.artistCount(leadKey) < selection.provisional
                if (!roomLeft) break
                if (selection.contains(track)) continue
                val album = albumKey(track)
                if (album != null && !albumsThisPass.add(album)) continue
                selection.add(track)
            }
            if (selection.chosen.size == sizeBeforePass) break
        }
    }

    private fun fillPass(
        selection: Selection,
        preferred: List<TrackInfo>,
        candidates: List<TrackInfo>,
        playlistIdsByTrack: Map<Long, Set<Long>>,
        randomTierOrder: List<TrackInfo>,
        zone: ZoneId,
        pass: Int,
    ) {
        val takenArtists = HashSet<String>()
        takeOnePerArtist(selection, preferred, pass, takenArtists)
        takeOnePerArtist(selection, playlistTracks(selection, candidates, playlistIdsByTrack), pass, takenArtists)
        takeOnePerArtist(selection, decadeTracks(selection, candidates), pass, takenArtists)
        takeOnePerArtist(selection, addTimeTracks(selection, candidates, zone, pass, takenArtists), pass, takenArtists)
        val randomIds = randomTierOrder.mapTo(HashSet()) { it.id }
        takeOnePerArtist(selection, randomTierOrder, pass, takenArtists)
        val extras = candidates.filter { it.id !in randomIds }.sortedBy { it.id }
        takeOnePerArtist(selection, extras, pass, takenArtists)
    }

    private fun playlistTracks(
        selection: Selection,
        candidates: List<TrackInfo>,
        playlistIdsByTrack: Map<Long, Set<Long>>,
    ): List<TrackInfo> {
        val chosenPlaylistIds = selection.chosen.flatMap { playlistIdsByTrack[it.id].orEmpty() }.toSet()
        if (chosenPlaylistIds.isEmpty()) return emptyList()
        return candidates
            .filter { track -> playlistIdsByTrack[track.id].orEmpty().any { it in chosenPlaylistIds } }
            .sortedBy { it.id }
    }

    private fun decadeTracks(
        selection: Selection,
        candidates: List<TrackInfo>,
    ): List<TrackInfo> {
        val chosenDecades = selection.chosen.mapNotNull { it.year?.let(::decadeOf) }.toSet()
        if (chosenDecades.isEmpty()) return emptyList()
        return candidates
            .filter { track ->
                val year = track.year ?: return@filter false
                decadeOf(year) in chosenDecades
            }.sortedBy { it.id }
    }

    private fun addTimeTracks(
        selection: Selection,
        candidates: List<TrackInfo>,
        zone: ZoneId,
        pass: Int,
        takenArtists: Set<String>,
    ): List<TrackInfo> {
        val chosenDates = selection.chosen.mapNotNull { it.dateAddedSec?.let { sec -> addedOn(sec, zone) } }
        if (chosenDates.isEmpty()) return emptyList()
        val chosenDays = chosenDates.toSet()
        val chosenMonths = chosenDates.map { YearMonth.from(it) }.toSet()
        val dated =
            candidates.mapNotNull { track ->
                val sec = track.dateAddedSec ?: return@mapNotNull null
                if (!canTake(selection, track, pass, takenArtists)) return@mapNotNull null
                track to addedOn(sec, zone)
            }
        val sameDay = dated.filter { (_, date) -> date in chosenDays }.map { it.first }.sortedBy { it.id }
        if (sameDay.isNotEmpty()) return sameDay
        return dated
            .filter { (_, date) -> YearMonth.from(date) in chosenMonths }
            .map { it.first }
            .sortedBy { it.id }
    }

    private fun takeOnePerArtist(
        selection: Selection,
        tracks: List<TrackInfo>,
        pass: Int,
        takenArtists: MutableSet<String>,
    ) {
        if (selection.isFull() || tracks.isEmpty()) return
        val picks = ArrayList<TrackInfo>()
        val indexByArtist = HashMap<String, Int>()
        for (track in tracks) {
            if (!canTake(selection, track, pass, takenArtists)) continue
            val artist = artistKey(track.artist)
            if (artist == null) {
                picks += track
                continue
            }
            val existing = indexByArtist[artist]
            if (existing == null) {
                indexByArtist[artist] = picks.size
                picks += track
            } else if (selection.albumCount(track) < selection.albumCount(picks[existing])) {
                picks[existing] = track
            }
        }
        for (track in picks) {
            if (selection.isFull()) return
            if (!canTake(selection, track, pass, takenArtists)) continue
            selection.add(track)
            artistKey(track.artist)?.let { takenArtists += it }
        }
    }

    private fun canTake(
        selection: Selection,
        track: TrackInfo,
        pass: Int,
        takenArtists: Set<String>,
    ): Boolean {
        if (selection.contains(track) || selection.isFull()) return false
        val artist = artistKey(track.artist)
        if (artist != null) {
            if (artist in takenArtists) return false
            val count = selection.artistCount(artist)
            if (count >= selection.provisional || count >= pass) return false
        }
        val album = albumKey(track)
        return album == null || selection.albumCount(track) < pass
    }

    internal fun shrinkToCap(chosen: MutableList<TrackInfo>): List<TrackInfo> {
        while (chosen.size >= 2) {
            val cap = maxShare(chosen.size)
            val over =
                chosen
                    .mapNotNull { artistKey(it.artist) }
                    .groupingBy { it }
                    .eachCount()
                    .filter { (_, count) -> count > cap }
                    .keys
            if (over.isEmpty()) break
            val index = chosen.indexOfLast { artistKey(it.artist) in over }
            if (index < 0) break
            chosen.removeAt(index)
        }
        return chosen.toList()
    }

    private fun interleave(
        tracks: List<TrackInfo>,
        leadKey: String?,
    ): List<TrackInfo> {
        if (leadKey == null || tracks.size < 2) return tracks
        val lead = ArrayList<TrackInfo>()
        val others = ArrayList<TrackInfo>()
        for (track in tracks) {
            if (artistKey(track.artist) == leadKey) {
                lead += track
            } else {
                others += track
            }
        }
        if (lead.isEmpty() || others.isEmpty()) return tracks
        val result = ArrayList<TrackInfo>(tracks.size)
        result += lead.first()
        var leadIndex = 1
        var otherIndex = 0
        while (otherIndex < others.size || leadIndex < lead.size) {
            if (otherIndex < others.size) {
                result += others[otherIndex]
                otherIndex++
            }
            if (leadIndex < lead.size) {
                result += lead[leadIndex]
                leadIndex++
            }
        }
        return result
    }

    private fun decadeOf(year: Int): Int = (year / 10) * 10

    private fun addedOn(
        dateAddedSec: Long,
        zone: ZoneId,
    ): LocalDate = Instant.ofEpochSecond(dateAddedSec).atZone(zone).toLocalDate()

    private class Selection(
        val limit: Int,
        val provisional: Int,
    ) {
        val chosen = ArrayList<TrackInfo>(limit)
        private val chosenIds = HashSet<Long>()
        private val artistCounts = HashMap<String, Int>()
        private val albumCounts = HashMap<String, Int>()

        fun isFull(): Boolean = chosen.size >= limit

        fun contains(track: TrackInfo): Boolean = track.id in chosenIds

        fun artistCount(key: String): Int = artistCounts[key] ?: 0

        fun albumCount(track: TrackInfo): Int {
            val key = albumKey(track) ?: return 0
            return albumCounts[key] ?: 0
        }

        fun add(track: TrackInfo) {
            chosen += track
            chosenIds += track.id
            artistKey(track.artist)?.let { key -> artistCounts[key] = artistCount(key) + 1 }
            albumKey(track)?.let { key -> albumCounts[key] = (albumCounts[key] ?: 0) + 1 }
        }
    }
}
