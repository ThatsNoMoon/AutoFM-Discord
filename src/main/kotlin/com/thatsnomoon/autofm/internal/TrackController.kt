package com.thatsnomoon.autofm.internal

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.thatsnomoon.autofm.AutoFM
import com.thatsnomoon.autofm.LOG


/**
 * Manager for starting random tracks and starting streams
 * @author ThatsNoMoon
 */

class TrackController(val player: AudioPlayer, private val musicManager: GuildMusicManager, private val parent: AutoFM): AudioEventAdapter() {

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext && !parent.USES_STREAM)
            playNextRandom()

    }

    fun playNextRandom(): Pair<String, String> {
        val resultSB = StringBuilder()
        val urlSB = StringBuilder()
        val randomURL = parent.getRandomURL()
        parent.playerManager.loadItemOrdered(musicManager, randomURL, object: AudioLoadResultHandler {

            override fun trackLoaded(track: AudioTrack) {
                player.playTrack(track)
                resultSB.append("Now Playing: ").append(track.info.title)
                urlSB.append(track.info.uri)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                LOG.warn("Found a playlist in the tracks list! Adding the first video...")
                val track = playlist.tracks[0]
                player.playTrack(track)
                resultSB.append("Now Playing: ").append(track.info.title)
                urlSB.append(track.info.uri)
            }

            override fun noMatches() {
                LOG.error("No matches for track. URL: $randomURL")
                resultSB.append("No matches for track.")
                urlSB.append(randomURL)
                playNextRandom()
            }

            override fun loadFailed(exception: FriendlyException) {
                LOG.error("Track load failed. URL: $randomURL", exception)
                resultSB.append("Load failed.").append("\nError:\n```\n").append(exception.localizedMessage).append("\n```")
                urlSB.append(randomURL)
            }
        })

        /* wait until there are results to return before returning them */
        var i = 0
        while (i < 60 && (resultSB.isEmpty() || urlSB.isEmpty())) {
            Thread.sleep(50); i++
        }
        return Pair(resultSB.toString(), urlSB.toString())
    }

    fun startStream(): Pair<String, String> {
        val resultSB = StringBuilder()
        val urlSB = StringBuilder()
        val URL = parent.STREAM_URL
        parent.playerManager.loadItemOrdered(musicManager, URL, object: AudioLoadResultHandler {

            override fun trackLoaded(track: AudioTrack) {
                player.playTrack(track)
                resultSB.append("Now Playing: ").append(track.info.title)
                urlSB.append(track.info.uri)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                LOG.warn("Found a playlist in the stream URL! Adding the first video...")
                val track = playlist.tracks[0]
                player.playTrack(track)
                resultSB.append("Now playing: ").append(track.info.title)
                urlSB.append(track.info.uri)
            }

            override fun noMatches() {
                LOG.error("No matches for track. URL: $URL")
                resultSB.append("No matches for track. Trying again.")
                urlSB.append(URL)
                playNextRandom()
            }

            override fun loadFailed(exception: FriendlyException) {
                LOG.error("Track load failed. URL: $URL", exception)
                resultSB.append("Load failed.").append("\nError:\n```\n").append(exception.localizedMessage).append("\n```")
                urlSB.append(URL)
            }
        })

        /* wait until there are results to return before returning them */
        var i = 0
        while (i < 60 && (resultSB.isEmpty() || urlSB.isEmpty())) {
            Thread.sleep(50); i++
        }
        return Pair(resultSB.toString(), urlSB.toString())
    }

}