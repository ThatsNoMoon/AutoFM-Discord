/*
 * Copyright (c) 2017 Benjamin Scherer
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.thatsnomoon.autofm

import club.minnced.kjda.entities.join
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.thatsnomoon.autofm.internal.GuildMusicManager
import kotlinx.coroutines.experimental.Job
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors


/**
 * @author ThatsNoMoon
 * Main bot class, contains all the configuration information
 */

class AutoFM(private val jda: JDA, cfg: Config) {

    val playerManager = DefaultAudioPlayerManager()
    val musicManagers: MutableMap<Long, GuildMusicManager> = mutableMapOf()

    private val random = Random()

    val PREFIX = cfg.prefix

    val OWNER_IDS = cfg.ownerIDs

    val USES_STREAM = cfg.usesStream
    private val URLS: List<String>
    val STREAM_URL = if(cfg.usesStream) cfg.stream else ""

    val USES_DJ_ROLES = cfg.usesDJRoles
    val DJ_ROLES = cfg.roles

    val USES_VOICE_TIMEOUTS = cfg.usesVoiceTimeout
    val REJOIN_AFTER_TIMEOUT = cfg.rejoinAfterTimeout
    val VOICE_TIMEOUT_MS = cfg.voiceTimeoutMs

    private val voiceTimeouts = mutableMapOf<Long, Job>()
    private val channelsWaitingForReconnect = mutableSetOf<Long>()

    init {

        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)

        /* if the bot isn't using a livestream... */
        URLS = if (!USES_STREAM) {
            if(cfg.usesYTPlaylists) {
                val tempList = mutableListOf<String>()
                /* either add all the songs from all the configured playlists... */
                cfg.playlists.forEach { tempList.addAll(loadYTPlaylist(it)) }
                tempList
            } else {
                /* or load a playlist from playlist.txt */
                getAutoPlaylist()
            }
        } else
            /* otherwise initialize it to an empty list */
            emptyList()

        /* for each of the channel IDs defined in the config, try to join them
         * if they don't exist, put an error into console and do nothing
         */
        for (i in cfg.channels) {
            val vc = jda.getVoiceChannelById(i)
            if (vc == null) {
                LOG.warn("Couldn't find voice channel with ID $i")
                continue
            }
            vc.join()
        }

        /* add event listeners for commands to control the bot
         * and voice events for automatic pausing when there are
         * no users in its voice channel */
        jda.addEventListener(CommandHandler(this))
        if (USES_VOICE_TIMEOUTS)
            jda.addEventListener(VoiceEventListenerUsingTimeouts(this))
        else
            jda.addEventListener(VoiceEventListener(this))
    }

    internal fun addVoiceTimeout(id: Long, job: Job) {
        voiceTimeouts.put(id, job)
    }

    internal fun removeVoiceTimeout(id: Long) {
        val job = voiceTimeouts[id]?: return
        job.cancel()
        voiceTimeouts.remove(id)
    }

    internal fun clearVoiceTimeouts() {
        voiceTimeouts.forEach {it.value.cancel()}
        voiceTimeouts.clear()
    }

    internal fun isChannelWaiting(id: Long): Boolean = channelsWaitingForReconnect.contains(id)

    internal fun addWaitingChannel(id: Long) = channelsWaitingForReconnect.add(id)

    internal fun removeWaitingChannel(id: Long) = channelsWaitingForReconnect.remove(id)

    internal fun clearWaitingChannels() = channelsWaitingForReconnect.clear()

    internal fun clearWaitingChannelsFromGuild(id: Long) {
        val channelsInGuild = jda.getGuildById(id).voiceChannels.map { it.idLong }
        channelsWaitingForReconnect.removeIf { channelsInGuild.contains(it) }
    }

    /* get the GuildMusicManager for the specified Guild */
    internal fun getGuildMusicManager(guild: Guild): GuildMusicManager {
        var musicManager = musicManagers[guild.idLong]
        if (musicManager == null) {
            musicManager = GuildMusicManager(playerManager, this)
            musicManagers.put(guild.idLong, musicManager)
        }
        guild.audioManager.sendingHandler = musicManager.getSendHandler()
        return musicManager
    }

    /* go to the next track, using the URLS list */
    internal fun nextTrack(guild: Guild): Pair<String, String> {
        LOG.debug("Playing next track.")
        val musicManager = getGuildMusicManager(guild)
        return musicManager.trackController.playNextRandom()
    }

    /* start (or restart) the livestream using the STREAM_URL */
    internal fun startStream(guild: Guild): Pair<String, String> {
        LOG.debug("Starting stream.")
        val musicManager = getGuildMusicManager(guild)
        return musicManager.trackController.startStream()
    }

    /* either start (or restart) the stream, or go to the next random track
     * from URLS, depending on what mode the bot is running in */
    internal fun next(guild: Guild): Pair<String, String> = if (USES_STREAM) startStream(guild) else nextTrack(guild)

    /* pause the specified guild's audio player
     * simple convenience function */
    internal fun pause(guild: Guild) {
        getGuildMusicManager(guild).player.isPaused = true
    }

    /* unpause the specified guild's audio player
     * simple convenience function */
    internal fun unpause(guild: Guild) {
        getGuildMusicManager(guild).player.isPaused = false
    }

    /* determine if the specified Guild's AudioPlayer is paused
     * simple convenience function */
    internal fun isPaused(guild: Guild): Boolean = getGuildMusicManager(guild).player.isPaused

    /* determine if the specified Guild's AudioPlayer is playing
     * simple convenience function */
    internal fun isPlaying(guild: Guild): Boolean = getGuildMusicManager(guild).player.playingTrack != null

    /* gets a random URL from URLS */
    internal fun getRandomURL(): String? {
        LOG.trace("Getting random URL...")
        if (URLS.isEmpty()) return null
        val URL = URLS[random.nextInt(URLS.size)]
        LOG.trace("Found $URL")
        return URL
    }

    /* load all of the tracks in the specified playlist into the URLS list */
    private fun loadYTPlaylist(playlistUrl: String): List<String> {
        LOG.debug("Loading YT Playlist. URL: " + playlistUrl)
        val tempList = mutableListOf<String>()
        playerManager.loadItemOrdered(this, playlistUrl, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                LOG.warn("Playlist URL was a single track. Was an incorrect URL entered?")
                tempList += track.info.uri
            }
            override fun playlistLoaded(playlist: AudioPlaylist) {
                LOG.trace("Loading playlist tracks...")
                tempList.addAll(playlist.tracks.stream().map { t -> t.info.uri }.collect(Collectors.toList()) as Collection<String>)
                LOG.info("Loaded YouTube playlist " + playlist.name)
            }

            override fun noMatches() {
                LOG.error("No matches for playlist. URL: " + playlistUrl)
            }

            override fun loadFailed(exception: FriendlyException) {
                LOG.error("Playlist load failed. URL: " + playlistUrl, exception)
            }
        })
        var i = 0
        while (i < 500 && tempList.isEmpty()) {
            Thread.sleep(50); i++
        }
        return tempList
    }

    /* load all of the uncommented lines from playlist.txt into the URLS list */
    private fun getAutoPlaylist(): List<String> {
        if (!Files.exists(Paths.get("./playlist.txt"))) {
            LOG.fatal("Playlist file not found! Do you not have a playlist.txt?")
            System.exit(-1)
        }
        val tempList = mutableListOf<String>()
        Files.lines(Paths.get("./playlist.txt")).apply { forEach {
            if (!it.startsWith("#")) tempList.add(it)
        } }
        if (tempList.isEmpty() && !USES_STREAM) {
            LOG.fatal("Your playlist file must not be empty!")
            System.exit(-1)
        }
        return tempList
    }
}