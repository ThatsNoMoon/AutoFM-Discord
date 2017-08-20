package com.thatsnomoon.autofm.internal;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.thatsnomoon.autofm.AutoFM;
import com.thatsnomoon.autofm.internal.AudioPlayerSendHandler;
import com.thatsnomoon.autofm.internal.TrackController;

/**
 * Holder for both the player and a track scheduler for one guild.
 */
public class GuildMusicManager {
    /**
    * Audio player for the guild.
    */
    public final AudioPlayer player;
    /**
    * Track scheduler for the player.
    */
    public final TrackController trackController;

    final AutoFM parent;

  /**
   * Creates a player and a track scheduler.
   * @param manager Audio player manager to use for creating the player.
   */
    public GuildMusicManager(AudioPlayerManager manager, AutoFM parent) {
        this.parent = parent;
        player = manager.createPlayer();
        trackController = new TrackController(player, this);
        player.addListener(trackController);
    }

    /**
    * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
    */
    public AudioPlayerSendHandler getSendHandler() {
    return new AudioPlayerSendHandler(player);
  }
}
