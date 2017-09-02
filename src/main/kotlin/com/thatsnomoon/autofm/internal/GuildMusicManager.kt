package com.thatsnomoon.autofm.internal

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.thatsnomoon.autofm.AutoFM


/**
 * @author Ben
 */

class GuildMusicManager(manager: AudioPlayerManager, parent: AutoFM) {
    val player: AudioPlayer = manager.createPlayer()
    val trackController: TrackController

    init {
        val tempController = TrackController(player, this, parent)
        trackController = tempController
        player.addListener(trackController)
    }

    fun getSendHandler(): AudioPlayerSendHandler = AudioPlayerSendHandler(player)
}