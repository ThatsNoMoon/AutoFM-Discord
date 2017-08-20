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
 * JDA event listener to automatically pause and resume based on the number of users in the current voice channel.
 * @author ThatsNoMoon
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
        /* Gets a list of the members in the voice channel that was just left, doesn't include other bots */
        List<Member> members = voiceLeaveEvent.getChannelLeft().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().equals(m.getJDA().getSelfUser())).collect(Collectors.toList());

        /* If the voice channel doesn't contain the self member, return */
        if (!members.contains(voiceLeaveEvent.getGuild().getSelfMember())) return;

        /* If the voice channel only contains the self member,
         * and the audio player isn't already paused, pause the audio player */
        if (members.size() == 1  && !parent.getGuildAudioPlayer(voiceLeaveEvent.getGuild()).player.isPaused()) {
            LOG.trace("No other users in current voice channel, pausing.");
            parent.getGuildAudioPlayer(voiceLeaveEvent.getGuild()).player.setPaused(true);
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent voiceJoinEvent) {
        LOG.trace("Guild voice join event");

        /* If the member that just joined is the self member, start a random song, or the stream */
        if (voiceJoinEvent.getMember().getUser().equals(voiceJoinEvent.getJDA().getSelfUser())) {
            if (!parent.USES_STREAM) {
                LOG.trace("Joined voice channel, starting random track.");
                parent.nextTrack(voiceJoinEvent.getGuild());
            }
            else {
                LOG.trace("Joined voice channel, starting stream.");
                parent.startStream(voiceJoinEvent.getGuild());
            }
            /* If the bot is the only one in its voice channel other than bots when it joins,
             * and the player isn't already paused, automatically pause */
            if (voiceJoinEvent.getChannelJoined().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().equals(m.getJDA().getSelfUser())).collect(Collectors.toList()).size() < 2 && !parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.isPaused()) {
                LOG.trace("No other users in current voice channel, pausing.");
                parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.setPaused(true);
            }
            return;
        }
        /* Gets a list of the members in the voice channel that was just joined, doesn't include other bots */
        List<Member> members = voiceJoinEvent.getChannelJoined().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().equals(m.getJDA().getSelfUser())).collect(Collectors.toList());

        /* If the channel that was just joined doesn't include the self member, return */
        if (!members.contains(voiceJoinEvent.getGuild().getSelfMember())) return;

        /* If the channel that was just joined now has more than 1 member in it, and the player was already paused,
         * unpause the player. If the bot is using a stream, refresh the stream.
         */
        if (members.size() >= 2 && parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.isPaused()) {
            LOG.trace("User just joined the current voice channel, unpausing.");
            parent.getGuildAudioPlayer(voiceJoinEvent.getGuild()).player.setPaused(false);
            if (parent.USES_STREAM) {
                parent.startStream(voiceJoinEvent.getGuild());
            }
        }

    }

    /*
     * Also overrides the move event, so that if the bot or a user just moves, it will still automatically pause or unpause
     * Could use a refactor.
     */
    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent voiceMoveEvent) {
        LOG.trace("Guild voice move event");

        /* Gets a list of the members in the voice channel that was just left, doesn't include other bots */
        List<Member> outOfMembers = voiceMoveEvent.getChannelLeft().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().equals(m.getJDA().getSelfUser())).collect(Collectors.toList());

        /* Gets a list of the members in the voice channel that was just joined, doesn't include other bots */
        List<Member> intoMembers = voiceMoveEvent.getChannelJoined().getMembers().stream().filter(m -> !m.getUser().isBot() || m.getUser().equals(m.getJDA().getSelfUser())).collect(Collectors.toList());

        /* If neither the channel that was left or the channel that was joined has the self member, return */
        if (!outOfMembers.contains(voiceMoveEvent.getGuild().getSelfMember()) && !intoMembers.contains(voiceMoveEvent.getGuild().getSelfMember())) {
            LOG.trace("Not current voice channel, returning");
            return;
        }
        /* If the channel that was just left only has the self member in it,
         * and the player isn't already paused, pause the player */
        if (outOfMembers.size() == 1 && !parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.isPaused()) {
            LOG.trace("No other users in current voice channel, pausing.");
            parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.setPaused(true);
            return;
        }

        /* Since the previous check checked both the joined channel and the channel that was left,
         * this check ensures that the joined channel specifically is the current voice channel */
        if (!intoMembers.contains(voiceMoveEvent.getGuild().getSelfMember())) {
            LOG.trace("Not current voice channel, returning.");
            return;
        }
        /* If the channel that was just joined has at least 1 other member, and the player isn't already unpaused,
         * unpause the player.
         */
        if (intoMembers.size() >= 2 && parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.isPaused()) {
            LOG.trace("User joined current voice channel, unpausing.");
            parent.getGuildAudioPlayer(voiceMoveEvent.getGuild()).player.setPaused(false);
            if (parent.USES_STREAM) {
                parent.startStream(voiceMoveEvent.getGuild());
            }
        }
    }
}
