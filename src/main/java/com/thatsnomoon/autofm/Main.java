package com.thatsnomoon.autofm;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;

public class Main {

    public static Logger LOG;

    public static void main(String[] args) {
        if (args.length >= 1)
            if (args[0].equals("debug")) {
                LOG = LogManager.getLogger("AutoFM-Debug");
            } else {
                LOG = LogManager.getLogger("AutoFM");
            }
        else {
            LOG = LogManager.getLogger("AutoFM");
        }
        startBot();
    }

    static void startBot() {
        LOG.info("Starting AutoFM...");
        LOG.debug("Creating Config...");
        Config cfg = new Config();
        JDA jda;
        try {
            LOG.debug("Creating and building JDABuilder");
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(cfg.getToken())
                    .setAudioSendFactory(new NativeAudioSendFactory())
                    .buildBlocking();
            AutoFM autoFM = new AutoFM(cfg);
            jda.addEventListener(autoFM);
            jda.addEventListener(new VoiceEventListener(autoFM));
//            if (cfg.usesYTPlaylist()) {
//                for (int i = 0; i < 60 && !autoFM.isLoaded(); i++) {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                autoFM.autoJoin(jda);
//                return;
//            }
            autoFM.autoJoin(jda);
        } catch (LoginException | InterruptedException | RateLimitedException e) {
            LOG.fatal("Failed to log in: ", e);
            System.exit(2);
        }
    }
}
