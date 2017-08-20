package com.thatsnomoon.autofm;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;

/**
 * @author ThatsNoMoon
 */

public class Main {

    public static Logger LOG;

    /**
     * Main method to enter code
     * @param args command-line arguments, should be either "debug" or not included.
     */

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

    /**
     * Starts AutoFM and loads the config file
     */

    static void startBot() {
        LOG.info("Starting AutoFM...");
        LOG.debug("Creating Config...");
        Config cfg = new Config();
        try {
            LOG.debug("Creating and building JDABuilder");
            JDA jda = new JDABuilder(AccountType.BOT)
                    .setToken(cfg.getToken())
                    .setAudioSendFactory(new NativeAudioSendFactory())
                    .buildBlocking();
            AutoFM autoFM = new AutoFM(cfg);
            jda.addEventListener(autoFM);
            jda.addEventListener(new VoiceEventListener(autoFM));
            autoFM.autoJoin(jda);
        } catch (LoginException | InterruptedException | RateLimitedException e) {
            LOG.fatal("Failed to log in: ", e);
            System.exit(2);
        }
    }
}
