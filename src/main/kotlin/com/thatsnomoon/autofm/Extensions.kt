package com.thatsnomoon.autofm

import club.minnced.kjda.RestPromise
import club.minnced.kjda.entities.isSelf
import club.minnced.kjda.entities.sendTextAsync
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit


/**
 * Convenience extensions to make it faster and cleaner to use JDA easily
 * @author ThatsNoMoon
 */
 
infix fun Message.reply(text: String): RestPromise<Message> = channel.sendTextAsync { text }

infix fun Message.replyThenDelete(text: String) {
    channel.sendTextAsync { text } then { it?.delete()?.queueAfter(10, TimeUnit.SECONDS) }
}

fun Message.replyThenDelete(text: String, delay: Long = 10) {
    channel.sendTextAsync { text } then { it?.delete()?.queueAfter(delay, TimeUnit.SECONDS) }
}

infix fun User.privateMessage(text: String): RestPromise<Message> = this.openPrivateChannel().complete().run { sendTextAsync { text } }

val Member.isSelf: Boolean
        get() = user.isSelf