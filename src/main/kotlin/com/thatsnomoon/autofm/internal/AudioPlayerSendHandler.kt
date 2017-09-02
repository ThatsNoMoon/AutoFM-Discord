package com.thatsnomoon.autofm.internal

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler


/**
 * Handler for sending audio to Discord
 * @author Sedmelluq
 */

class AudioPlayerSendHandler(private val player: AudioPlayer): AudioSendHandler {
    private var lastFrame: AudioFrame? = null

    override fun canProvide(): Boolean {
        if (lastFrame == null)
            lastFrame = player.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteArray? {
        if (lastFrame == null)
            lastFrame = player.provide()
        val data = lastFrame?.data
        lastFrame = null
        return data
    }

    override fun isOpus(): Boolean = true
}