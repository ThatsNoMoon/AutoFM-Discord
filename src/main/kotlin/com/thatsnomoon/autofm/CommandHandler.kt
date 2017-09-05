/*
 * Copyright (c) 2017 Benjamin Scherer
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.thatsnomoon.autofm

import club.minnced.kjda.SPACES
import club.minnced.kjda.entities.disconnect
import club.minnced.kjda.entities.join
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


/**
 * Listener for guild message events used to handle commands
 * @author ThatsNoMoon
 */

class CommandHandler(private val parent: AutoFM): ListenerAdapter() {

    /* values to use when determining how the bot responds to commands */
    private val PREFIX = parent.PREFIX
    private val OWNER_IDS = parent.OWNER_IDS

    private val USES_STREAM = parent.USES_STREAM

    private val USES_DJ_ROLES = parent.USES_DJ_ROLES
    private val DJ_ROLES = parent.DJ_ROLES

    /* only receive messages from guilds since DMs can't use music bots */
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val msg = event.message
        if (!msg.content.startsWith(PREFIX)) return
        /* if the bot can't talk in the channel the command was sent in, send the user who send the command an error message */
        if (!event.channel.canTalk()) {
            /* the first of many extension functions this class uses, defined in Extensions.kt */
            event.author privateMessage "I can't send messages in that channel."
            return
        }
        val command = msg.content.split(SPACES, 2)[0].substring(PREFIX.length).toLowerCase()
        LOG.debug("Received message event from channel ${event.channel.name}. Command: $command")

        if (OWNER_IDS.contains(event.author.idLong)) {
            when (command) {
                "shutdown" -> {
                    msg reply "Shutting down..."
                    LOG.info("Shutting down")
                    event.jda.shutdown()
                    return
                }
                "restart" -> {
                    msg reply "Restarting..."
                    LOG.info("Restarting")
                    println()
                    restart()
                    return
                }
            }
        }
        val userVC = event.member.voiceState.channel
        if (userVC == null) {
            msg replyThenDelete "You aren't in a voice channel!"
            return
        }
        if (!userVC.members.contains(event.guild.selfMember) && !arrayOf("summon","join").contains(command)) {
            msg replyThenDelete "We aren't in the same voice channel!"
            return
        }

        val isDJ = USES_DJ_ROLES && event.member.roles.stream().map{ it.idLong }.anyMatch{ DJ_ROLES.contains(it) } || !USES_DJ_ROLES || OWNER_IDS.contains(event.author.idLong)
        when (command) {
            "np", "playing", "nowplaying" -> {
                val info = parent.getGuildMusicManager(event.guild).player.playingTrack.info
                msg reply "Now Playing: ${info.title}\nURL: ${info.uri}"
            }
            "skip" -> {
                if (USES_STREAM) {
                    msg replyThenDelete "Bot is running in stream mode, `skip` is not availible!"
                    return
                }
                if (userVC.members.size > 2 && !isDJ) {
                    msg replyThenDelete "You must be a DJ to use `skip`!"
                    return
                }
                /* use some kotlin sugar to respond with a Pair from a function and have it look nice */
                msg reply parent.nextTrack(event.guild).run { "$first\nURL: $second" }
            }
            "refresh" -> {
                if (!USES_STREAM) {
                    msg replyThenDelete "Bot is running in playlist mode, `refresh` is not availible!"
                    return
                }
                if (userVC.members.size > 2 && !isDJ) {
                    msg replyThenDelete "You must be a DJ to use `refresh`!"
                    return
                }
                msg reply parent.startStream(event.guild).run { "$first\nURL: $second" }
            }
            "summon", "join" -> {
                if (event.guild.audioManager.isConnected && event.guild.audioManager.connectedChannel.members.size > 1 && !isDJ) {
                    msg replyThenDelete "You must be a DJ to use `$command` when I'm already in a voice channel with others!"
                    return
                }
                msg reply "Joining ${userVC.name}..." then {
                    userVC.join()
                    it?.delete()?.queue()
                }
            }
            "leave" -> {
                if (userVC.members.size > 2 && !isDJ) {
                    msg replyThenDelete "You must be a DJ to use `leave`!"
                    return
                }
                if (!event.guild.audioManager.isConnected) {
                    msg replyThenDelete "I'm not connected to a voice channel!"
                    return
                }
                msg reply "Leaving ${userVC.name}..." then {
                    event.guild.disconnect()
                    it?.delete()?.queue()
                }
            }
        }
    }
}