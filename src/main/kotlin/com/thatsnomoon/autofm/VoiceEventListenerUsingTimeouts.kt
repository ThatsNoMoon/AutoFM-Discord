package com.thatsnomoon.autofm

import club.minnced.kjda.entities.disconnect
import club.minnced.kjda.entities.join
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


/**
 * Listener for voice events to handle automatic pausing and unpausing when nobody is using the bot to save resources
 * @author ThatsNoMoon
 */

class VoiceEventListenerUsingTimeouts(private val parent: AutoFM): ListenerAdapter() {

    private val TIMEOUT = parent.VOICE_TIMEOUT_MS
    private val REJOIN_AFTER_TIMEOUT = parent.REJOIN_AFTER_TIMEOUT

    override fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
        val guild = event.guild
        /* the members in whatever voice channel is relevant */
        val members: List<Member>
        /* the relevant channel */
        val channel: VoiceChannel
        when (event) {
            is GuildVoiceJoinEvent -> {
                members = event.channelJoined.members
                channel = event.channelJoined
            }
            is GuildVoiceLeaveEvent -> {
                members = event.channelLeft.members
                channel = event.channelLeft
            }
            is GuildVoiceMoveEvent -> {
                when {
                    event.channelJoined.members.contains(guild.selfMember) || (REJOIN_AFTER_TIMEOUT && parent.isChannelWaiting(event.channelJoined.idLong)) -> {
                        members = event.channelJoined.members
                        channel = event.channelJoined
                    }
                    event.channelLeft.members.contains(guild.selfMember) -> {
                        members = event.channelLeft.members
                        channel = event.channelLeft
                    }
                    else -> return
                }
            }
            else -> return
        }
        /* if the bot isn't in the voice channel the event happened in, and the bot isn't waiting for a member to join back into its channel it doesn't matter */
        if (!members.contains(guild.selfMember) && !parent.isChannelWaiting(channel.idLong)) return

        if (REJOIN_AFTER_TIMEOUT && event !is GuildVoiceLeaveEvent && parent.isChannelWaiting(channel.idLong)) {
            if (members.isNotEmpty()) {
                channel.join()
                parent.removeWaitingChannel(channel.idLong)
            }
        }

        /* if the bot just joined a channel and it's not playing or it's paused, start playing and unpause */
        if (event is GuildVoiceJoinEvent && (!parent.isPlaying(guild) || parent.isPaused(guild)) && event.member.isSelf) {
            parent.next(guild)
            parent.unpause(guild)
            parent.removeVoiceTimeout(event.guild.idLong)
            if (REJOIN_AFTER_TIMEOUT)
                parent.clearWaitingChannelsFromGuild(event.guild.idLong)
        }

        /* if there aren't any other members in the voice channel and the bot isn't paused already, pause */
        if (members.size < 2 && !parent.isPaused(guild)) {
            parent.pause(guild)
            val guildID = event.guild.idLong
            val jda = event.jda
            val channelID = channel.idLong
            parent.addVoiceTimeout(event.guild.idLong, launch(CommonPool) {
                delay(TIMEOUT)
                val timeoutGuild = jda.getGuildById(guildID)
                if (timeoutGuild != null) {
                    timeoutGuild.disconnect()
                    parent.getGuildMusicManager(timeoutGuild).player.isPaused = true
                    if (REJOIN_AFTER_TIMEOUT)
                        parent.addWaitingChannel(channelID)
                }
                parent.removeVoiceTimeout(guildID)
            })
        }


        /* if there are other members in the voice channel and it was paused, unpause */
        else if (members.size > 1 && parent.isPaused(guild)) {
            parent.unpause(guild)
            parent.removeVoiceTimeout(event.guild.idLong)
        }
    }
}