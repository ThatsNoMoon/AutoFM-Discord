package com.thatsnomoon.autofm

import club.minnced.kjda.SPACES
import java.nio.file.Files
import java.nio.file.Paths


/**
 * Class to represent a configuration, including:
 * The bot's token
 * The bot's prefix
 * The bot owner's ID
 * Whether the bot uses a stream or not, and if it does, the stream URL
 * Whether the bot uses a YouTube playlist(s) instead of a text-based playlist, and if it does, the playlist URLs.
 * Whether the bot uses DJ roles, and if it does, a list of the roles' IDs
 * Whether the bot uses auto-join channels, and if it does, a list of the channels' IDs
 * @author ThatsNoMoon
 */

class Config {

    var token = ""

    var prefix = "fm!"

    var usesStream = false
    var stream = ""

    var usesYTPlaylists = false
    val playlists = mutableListOf<String>()

    var usesDJRoles = false
    var usesAutoJoinChannels = false

    val ownerIDs = mutableSetOf<Long>()

    val channels = mutableSetOf<Long>()
    val roles = mutableSetOf<Long>()

    init {
        if (!Files.exists(Paths.get("./config.txt"))) {
            LOG.fatal("Config file not found! Do you not have a config.txt?")
            System.exit(-1)
        }
        Files.lines(Paths.get("./config.txt")).map { it.trim() }.apply { forEach {

            /* i represents the index that strings can be cut to to get the option after "option:" */
            val i = if(it.indexOf(':') != -1) it.indexOf(':') + 1 else return@forEach
            /* the string after "option:" */
            val option = it.substring(i).trim()

            when {
                it.startsWith("token:") -> {
                    token = option
                    LOG.debug("Set token")
                }
                it.startsWith("prefix:") -> {
                    prefix = option
                    LOG.debug("Set prefix to: $prefix")
                }
                it.startsWith("stream:") -> {
                    usesStream = true
                    stream = option
                    LOG.debug("Set stream URL to: $stream")
                }
                it.startsWith("playlist:") -> {
                    usesYTPlaylists = true
                    playlists += option
                    LOG.debug("Added playlist, URL: $option")
                }
                it.startsWith("owner ID:") -> {
                    if(option.toLongOrNull() != null) {
                        ownerIDs += option.toLong()
                        LOG.debug("Owner ID set to: $option")
                    } else
                        LOG.debug("Invalid user ID: $option")
                }
                it.startsWith("auto-join channels:") -> {
                    usesAutoJoinChannels = true
                    option.replace(SPACES, "").split(",").apply { forEach {
                        if (it.toLongOrNull() != null) {
                            if (!channels.add(it.toLong()))
                                LOG.warn("Duplicate channel ID: $it, skipped")
                            else
                                LOG.debug("Added auto-join channel, ID: $it")
                        } else
                            LOG.error("Invalid channel ID: $it")
                    } }
                }
                it.startsWith("DJ roles:") -> {
                    usesDJRoles = true
                    option.replace(SPACES, "").split(",").apply { forEach {
                        if (it.toLongOrNull() != null) {
                            if (!roles.add(it.toLong()))
                                LOG.warn("Duplicate role ID: $it, skipped")
                            else
                                LOG.debug("Added DJ role, ID: $it")
                        } else
                            LOG.error("Invalid role ID: $it")
                    } }
                }
            }
        } }
    }
}