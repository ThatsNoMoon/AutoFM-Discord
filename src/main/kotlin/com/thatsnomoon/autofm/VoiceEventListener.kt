package com.thatsnomoon.autofm

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


/**
 * @author Ben
 */

class VoiceEventListener(private val parent: AutoFM): ListenerAdapter() {

    override fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
        val guild = event.guild
        /* the members in whatever voice channel is relevant */
        val members: List<Member>
        when (event) {
            is GuildVoiceJoinEvent -> {
                members = event.channelJoined.members
            }
            is GuildVoiceLeaveEvent -> {
                members = event.channelLeft.members
            }
            is GuildVoiceMoveEvent -> {
                members = when {
                    event.channelJoined.members.contains(guild.selfMember) -> {
                        event.channelJoined.members
                    }
                    event.channelLeft.members.contains(guild.selfMember) -> {
                        event.channelLeft.members
                    }
                    else -> return
                }
            }
            else -> return
        }
        /* if the bot isn't in the voice channel the event happened in it doesn't matter */
        if (!members.contains(guild.selfMember)) return

        /* if the bot just joined a channel and it's not playing or it's paused, start playing and unpause */
        if (event is GuildVoiceJoinEvent && (!parent.isPlaying(guild) || parent.isPaused(guild)) && event.member.isSelf) {
            parent.next(guild)
            parent.unpause(guild)
        }

        /* if there aren't any other members in the voice channel and the bot isn't paused already, pause */
        if (members.size < 2 && !parent.isPaused(guild)) {
            parent.pause(guild)
        }


        /* if there are other members in the voice channel and it was paused, unpause */
        else if (members.size > 1 && parent.isPaused(guild)) {
            parent.unpause(guild)
        }
    }
}