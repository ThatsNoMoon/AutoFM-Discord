/*
 * Copyright (c) 2017 Benjamin Scherer
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.thatsnomoon.autofm

import club.minnced.kjda.client
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import net.dv8tion.jda.core.AccountType.BOT
import net.dv8tion.jda.core.JDA
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger


/**
 * @author ThatsNoMoon
 * Entry-point to AutoFM and container for the AutoFM Logger
 */

var LOG: Logger = LogManager.getLogger("AutoFM")

private var jda: JDA? = null


/* entry point to the bot on startup */
fun main(args: Array<String>) {
    /* if the command-line arguments are "debug" set the logger to the debug logger
     * to log more information */
    if (!args.isEmpty() && args[0] == "debug")
        LOG = LogManager.getLogger("AutoFM-Debug")
    start()
}

fun start() {
    LOG.info("Starting AutoFM")
    LOG.debug("Creating Config...")
    val cfg = Config()
    /* uses tempJDA variable to avoid nullable-type errors when initializing the AutoFM class */
    val tempJDA = client(BOT) {
        setAudioSendFactory(NativeAudioSendFactory())
        setToken(cfg.token)
    }
    jda = tempJDA
    AutoFM(tempJDA, cfg)
}

fun restart() {
    jda?.shutdown()
    start()
}