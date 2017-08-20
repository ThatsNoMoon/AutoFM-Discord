package com.thatsnomoon.autofm.internal;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.thatsnomoon.autofm.Main;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author ThatsNoMoon
 * Controls the audio player for each guild
 */
public class TrackController extends AudioEventAdapter {
    private final AudioPlayer player;
    private final GuildMusicManager musicManager;

    private final Random random = new Random();
    private final Logger LOG = Main.LOG;

    /**
    * @param player The audio player this scheduler uses
    */
    TrackController(AudioPlayer player, GuildMusicManager musicManager) {
        this.player = player;
        this.musicManager = musicManager;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            playNextRandom();
        }
    }

    public String playNextRandom() {
        StringBuilder sb = new StringBuilder();
        String randomURL = musicManager.parent.getRandomURL();
        musicManager.parent.playerManager.loadItemOrdered(musicManager, randomURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                sb.append("Now playing: ").append(track.getInfo().title);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {}

            @Override
            public void noMatches() {
                LOG.error("No matches for track. URL: " + randomURL);
                sb.append("No matches for track. URL: ").append(randomURL);
                playNextRandom();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                LOG.error("Track load failed. URL: " + randomURL, exception);
                sb.append("Load failed. URL: ").append(randomURL).append("\nError:\n```\n").append(exception.getLocalizedMessage()).append("\n```");
            }
        });
        for (int i = 0; i < 60 && sb.length() == 0; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public String startStream() {
        StringBuilder sb = new StringBuilder();
        musicManager.parent.playerManager.loadItemOrdered(musicManager, musicManager.parent.getStreamUrl(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                sb.append("Now playing stream: ").append(track.getInfo().title);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {}

            @Override
            public void noMatches() {
                LOG.error("No matches for stream. URL: " + musicManager.parent.getStreamUrl());
                sb.append("Couldn't find stream.");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                LOG.error("Stream load failed. URL: " + musicManager.parent.getStreamUrl(), exception);
                sb.append("Couldn't load stream.\nError message:\n```\n").append(exception.getLocalizedMessage()).append("\n```");
            }
        });
        for (int i = 0; i < 60 && sb.length() == 0; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
