package com.thatsnomoon.autofm;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ben
 */

public class VoiceEventListener extends ListenerAdapter {

    private final AutoFM parent;
    private final Logger LOG = Main.LOG;

    VoiceEventListener(AutoFM parent) {
        this.parent = parent;
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent voiceLeaveEvent) {
        LOG.trace("Guild voice leave event");
        List<Member> members = voiceLeaveEvent.getChannelLeft().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().getIdLong() == m.getJDA().getSelfUser().getIdLong()).collect(Collectors.toList());
        if (!members.contains(voiceLeaveEvent.getGuild().getSelfMember())) return;
        if (members.size() == 1  && !parent.getGuildAudioPlayer(voiceLeaveEvent.getGuild()).player.isPaused()) {
            LOG.trace("No other users in current voice channel, pausing.");
            parent.getGuildAudioPlayer(voiceLeaveEvent.getGuild()).player.setPaused(true);
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent voiceJoinEvent) {
        LOG.trace("Guild voice join event");
        if (voiceJoinEvent.getMember().getUser().getIdLong() == voiceJoinEvent.getJDA().getSelfUser().getIdLong()) {
            if (!parent.USES_STREAM) {
                LOG.trace("Joined voice channel, starting random track.");
                parent.nextTrack(voiceJoinEvent.getGuild());
            }
            else {
                LOG.trace("Joined voice channel, starting stream.");
                parent.startStream(voiceJoinEvent.getGuild());
            }
            if (voiceJoinEvent.getChannelJoined().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().getIdLong() == m.getJDA().getSelfUser().getIdLong()).collect(Collectors.toList()).size() < 2 && !parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.isPaused()) {
                LOG.trace("No other users in current voice channel, pausing.");
                parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.setPaused(true);
            }
            return;
        }
        LOG.trace("Paused: " + String.valueOf(parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.isPaused()));
        List<Member> members = voiceJoinEvent.getChannelJoined().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().getIdLong() == m.getJDA().getSelfUser().getIdLong()).collect(Collectors.toList());
        if (!members.contains(voiceJoinEvent.getGuild().getSelfMember())) return;
        if (members.size() >= 2 && parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.isPaused()) {
            LOG.trace("User just joined the current voice channel, unpausing.");
            parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.setPaused(false);
            if (parent.USES_STREAM) {
                parent.startStream(voiceJoinEvent.getGuild());
            }
        }

    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent voiceMoveEvent) {
        LOG.trace("Guild voice move event");
        List<Member> outOfMembers = voiceMoveEvent.getChannelLeft().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().getIdLong() == m.getJDA().getSelfUser().getIdLong()).collect(Collectors.toList());
        List<Member> intoMembers = voiceMoveEvent.getChannelJoined().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().getIdLong() == m.getJDA().getSelfUser().getIdLong()).collect(Collectors.toList());
        if (!outOfMembers.contains(voiceMoveEvent.getGuild().getSelfMember()) && !intoMembers.contains(voiceMoveEvent.getGuild().getSelfMember())) {
            LOG.trace("Not current voice channel, returning");
            return;
        }
        LOG.trace("Paused: " + String.valueOf(parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.isPaused()));
        if (outOfMembers.size() == 1 && !parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.isPaused()) {
            LOG.trace("No other users in current voice channel, pausing.");
            parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.setPaused(true);
            return;
        }

        if (!intoMembers.contains(voiceMoveEvent.getGuild().getSelfMember())) {
            LOG.trace("Not current voice channel, returning.");
            return;
        }
        if (intoMembers.size() >= 2 && parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.isPaused()) {
            LOG.trace("User joined current voice channel, unpausing.");
            parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.setPaused(false);
            if (parent.USES_STREAM) {
                parent.startStream(voiceMoveEvent.getGuild());
            }
        }
    }
}
