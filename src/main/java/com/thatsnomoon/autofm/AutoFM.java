package com.thatsnomoon.autofm;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.thatsnomoon.autofm.internal.GuildMusicManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Ben
 */

public class AutoFM extends ListenerAdapter {

    private final Logger LOG = Main.LOG;

    private final Random random = new Random();

    private String prefix;

    final boolean USES_STREAM;
    private final List<String> URLS;
    private final String STREAM_URL;

    public final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();

    private final long OWNER_ID;

    private final boolean USES_AUTO_JOIN_CHANNELS;
    private final Set<Long> AUTO_JOIN_CHANNELS;

    private final boolean USES_DJ_ROLES;
    private final Set<Long> DJ_ROLES;

    private boolean loaded = false;

    AutoFM(Config cfg) {

        LOG.info("Prefix: " + cfg.getPrefix());
        this.prefix = cfg.getPrefix();

        this.OWNER_ID = cfg.getOwnerID();

        this.USES_STREAM = cfg.usesStream();

        this.USES_AUTO_JOIN_CHANNELS = cfg.usesAutoJoinChannels();
        this.USES_DJ_ROLES = cfg.usesDJRoles();

        this.AUTO_JOIN_CHANNELS = (USES_AUTO_JOIN_CHANNELS) ? cfg.getChannels() : null;
        this.DJ_ROLES = (USES_DJ_ROLES) ? cfg.getRoles() : null;

        if (USES_STREAM) {
            this.URLS = null;
            loaded = true;
        } else if (cfg.usesYTPlaylist()) {
            URLS = new ArrayList<>();
            for (String url : cfg.getPlaylists()) {
                loadYTPlaylist(url);
            }
        } else {
            this.URLS = new AutoPlaylist().URLS;
            loaded = true;
        }
        this.STREAM_URL = (USES_STREAM) ? cfg.getStream() : null;

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager, this);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (!event.getMessage().getContent().startsWith(prefix)) return;
        if (!event.getChannel().canTalk()) {
            event.getAuthor().openPrivateChannel().queue(c -> c.sendMessage("I can't send messages in that channel.").queue());
            return;
        }
        String command = event.getMessage().getContent().split(" ", 2)[0].substring(prefix.length()).toLowerCase();
        LOG.debug("Received message event from channel " + event.getChannel().getName() + ". Command: " + command);

        if (event.getAuthor().getIdLong() == OWNER_ID) {
            if (command.equals("shutdown")) {
                LOG.info("Shutting down");
                event.getJDA().shutdown();
                return;
            } else if (command.equals("restart")) {
                LOG.info("Restarting");
                System.out.println();
                event.getJDA().shutdown();
                Main.startBot();
                return;
            }
        }
        VoiceChannel userVC = event.getMember().getVoiceState().getChannel();
        if (userVC == null) {
            event.getChannel().sendMessage("You aren't in a voice channel!").queue(AutoFM::deleteAfter10s);
            return;
        }
        if (!userVC.getMembers().contains(event.getGuild().getSelfMember()) && !command.equals("summon")) {
            event.getChannel().sendMessage("You aren't in my voice channel!").queue(AutoFM::deleteAfter10s);
            return;
        }

        boolean isDJ = ((USES_DJ_ROLES) && !Collections.disjoint(DJ_ROLES, event.getMember().getRoles().stream().map(Role::getIdLong).collect(Collectors.toList()))) || !USES_DJ_ROLES || event.getAuthor().getIdLong() == OWNER_ID;
        switch (command) {
            case "skip" :
                if (userVC.getMembers().size() > 2 && !isDJ) {
                    event.getChannel().sendMessage("You must be a DJ to use `skip`!").queue(AutoFM::deleteAfter10s);
                    return;
                }
                if (USES_STREAM) {
                    event.getChannel().sendMessage("Bot is running in stream mode, `skip` is not availible!").queue(AutoFM::deleteAfter10s);
                    break;
                }
                event.getChannel().sendMessage(nextTrack(event.getGuild())).queue(AutoFM::deleteAfter10s);
                break;
            case "refresh" :
                if (userVC.getMembers().size() > 2 && !isDJ) {
                    event.getChannel().sendMessage("You must be a DJ to use `refresh`!").queue(AutoFM::deleteAfter10s);
                    return;
                }
                if (!USES_STREAM) {
                    event.getChannel().sendMessage("Bot is running in playlist mode, `refresh` is not availible!").queue(AutoFM::deleteAfter10s);
                    break;
                }
                event.getChannel().sendMessage(startStream(event.getGuild())).queue(AutoFM::deleteAfter10s);
                break;
            case "summon" :
                if (event.getGuild().getAudioManager().isConnected() && event.getGuild().getAudioManager().getConnectedChannel().getMembers().size() > 1 && !isDJ) {
                    event.getChannel().sendMessage("You must be a DJ to use `summon` when I'm already in a voice channel with others!").queue(AutoFM::deleteAfter10s);
                    return;
                }
                event.getChannel().sendMessage("Joining " + userVC.getName() + "...").queue(m -> {
                    connectToVoiceChannel(event.getGuild().getAudioManager(), userVC);
                    m.delete().queue();
                });
                break;
            case "leave" :
                if (userVC.getMembers().size() > 2 && !isDJ) {
                    event.getChannel().sendMessage("You must be a DJ to use `leave`!").queue(AutoFM::deleteAfter10s);
                    return;
                }
                if (!event.getGuild().getAudioManager().isConnected()) {
                    event.getChannel().sendMessage("I'm not connected to a voice channel!").queue(AutoFM::deleteAfter10s);
                    return;
                }
                event.getChannel().sendMessage("Leaving " + event.getGuild().getAudioManager().getConnectedChannel().getName() + "...").queue(m -> {
                    closeVoiceConnection(event.getGuild().getAudioManager());
                    m.delete().queue();
                });
                break;
        }
    }

    String nextTrack(Guild guild) {
        LOG.debug("Playing next track.");
        GuildMusicManager musicManager = getGuildAudioPlayer(guild);
        String result = musicManager.trackController.playNextRandom();
        return (result == null) ? "URL list was empty!" : "Playing next track.";
    }

    String startStream(Guild guild) {
        LOG.debug("Starting stream.");
        GuildMusicManager musicManager = getGuildAudioPlayer(guild);
        return musicManager.trackController.startStream();
    }

    void autoJoin(JDA jda) {
        if (!USES_AUTO_JOIN_CHANNELS) return;
        LOG.info("Auto-Joining channels");
        for (long id : AUTO_JOIN_CHANNELS) {
            VoiceChannel vc = jda.getVoiceChannelById(id);
            if (vc == null) {
                LOG.warn("Voice channel not found. ID: " + id);
                continue;
            }
            vc.getGuild().getAudioManager().openAudioConnection(vc);
        }
    }

    private static void connectToVoiceChannel(AudioManager audioManager, VoiceChannel vc) {
        if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
            Main.LOG.trace("Closing audio connecting in guild: " + audioManager.getGuild().getName());
            audioManager.closeAudioConnection();
            Main.LOG.trace("Connecting to voice channel: " + vc.getName());
            audioManager.openAudioConnection(vc);
        }
        else if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            Main.LOG.trace("Connecting to voice channel: " + vc.getName());
            audioManager.openAudioConnection(vc);
        }
    }

    private static void closeVoiceConnection(AudioManager audioManager) {
        if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
            Main.LOG.trace("Closing audio connecting in guild: " + audioManager.getGuild().getName());
            audioManager.closeAudioConnection();
        }
    }

    public String getRandomURL() {
        LOG.trace("Getting random URL...");
        if (URLS.size() == 0) return null;
        String URL = URLS.get(random.nextInt(URLS.size()));
        LOG.trace("Found " + URL);
        return URL;
    }

    private void loadYTPlaylist(String playlistUrl) {
        playerManager.loadItemOrdered(this, playlistUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                LOG.warn("Playlist URL was a single track. Was an incorrect URL entered?");
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                LOG.info("Loaded YouTube playlist " + playlist.getName());
                URLS.addAll(playlist.getTracks().stream().map(t -> t.getInfo().uri).collect(Collectors.toList()));
//                loaded();
            }

            @Override
            public void noMatches() {
                LOG.error("No matches for playlist. URL: " + playlistUrl);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                LOG.error("Playlist load failed. URL: " + playlistUrl, exception);
            }
        });
    }

    public String getStreamUrl() {
        return STREAM_URL;
    }

//    public boolean isLoaded() {
//        return loaded;
//    }
//
//    private void loaded() {
//        loaded = true;
//    }

    private static void deleteAfter10s(Message msg) {
        msg.delete().queueAfter(10, TimeUnit.SECONDS);
    }
}
