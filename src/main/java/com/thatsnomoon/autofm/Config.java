package com.thatsnomoon.autofm;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Ben
 */

class Config {

    private final Logger LOG = Main.LOG;

    private String token = "";

    private String prefix = "fm!";

    private boolean isStream = false;
    private String stream = "";

    private boolean usesPlaylist = false;
    private String playlist = "";
    private List<String> playlists = new ArrayList<>();

    private boolean usesDJRoles = false;
    private boolean usesAutoJoinChannels = false;

    private long ownerID = 0L;

    private Set<Long> channels = new HashSet<>();
    private Set<Long> roles = new HashSet<>();

    Config() {
        LOG.trace("Loading Config File");
        try (Stream<String> stream = Files.lines(Paths.get("./config.txt"))) {
            stream.forEach(s -> {
                s = s.trim();
//                LOG.trace("Parsing string: " + ( s.startsWith("token:") ? s.substring(0, 15) + "..." : s ));
                int i = s.indexOf(':')+1;
                if (s.startsWith("token:")) {
                    token = s.substring(i).trim();
                    LOG.debug("Set token");
                }
                else if (s.startsWith("prefix:")) {
                    prefix = s.substring(i).trim();
                    LOG.debug("Set prefix to: " + prefix);
                }
                else if (s.startsWith("stream:")) {
                    this.isStream = true;
                    this.stream = s.substring(i).trim();
                    LOG.debug("Set stream URL to: " + this.stream);
                }
//                else if (s.startsWith("playlist:")) {
//                    this.usesPlaylist = true;
//                    this.playlists.add(s.substring(i));
//                    LOG.debug("Added playlist, URL: " + s.substring(i));
//                }
                else if(s.startsWith("owner ID:")) {
                    try {
                        ownerID = Long.parseLong(s.substring(i));
                        LOG.debug("Owner ID: " + s.substring(i));
                    }
                    catch (NumberFormatException e) {
                        LOG.error("Invalid user ID: " + s.substring(i));
                    }
                }
                else if (s.startsWith("auto-join channels:")) {
                    usesAutoJoinChannels = true;
                    String[] channelStrings = s.substring(i).replace(" ", "").split(",");
                    for (String c : channelStrings) {
                        try {
                            if (!channels.add(Long.parseLong(c)))
                                LOG.warn("Duplicate channel ID: " + c + ", skipped");
                            else LOG.debug("Added auto-join channel: " + c);
                        }
                        catch (NumberFormatException e) {
                            LOG.error("Invalid channel ID: " + c);
                        }
                    }
                }
                else if (s.startsWith("DJ roles:")) {
                    usesDJRoles = true;
                    String[] roleStrings = s.substring(i).replace(" ", "").split(",");
                    for (String r : roleStrings) {
                        try {
                            if (!roles.add(Long.parseLong(r)))
                                LOG.warn("Duplicate role ID: " + r + ", skipped");
                            else LOG.debug("Added DJ role: " + r);
                        }
                        catch (NumberFormatException e) {
                            LOG.error("Invalid role ID: " + r);
                        }
                    }
                }
            });
        } catch (IOException e) {
            LOG.fatal("Exception loading config: ", e);
            System.exit(2);
        }
        if (token.equals("")) {
            LOG.fatal("You must specify a token!");
            System.exit(-1);
        }
        if (isStream && stream.equals("")) {
            LOG.fatal("When using stream mode you must specify a stream!");
            System.exit(-1);
        }
    }

    String getToken() {
        return token;
    }

    String getPrefix() {
        return prefix;
    }

    boolean usesStream() {
        return isStream;
    }

    String getStream() {
        return stream;
    }

    boolean usesYTPlaylist() {
        return usesPlaylist;
    }

    List<String> getPlaylists() {
        return Collections.unmodifiableList(playlists);
    }

    boolean usesDJRoles() {
        return usesDJRoles;
    }

    boolean usesAutoJoinChannels() {
        return usesAutoJoinChannels;
    }

    long getOwnerID() {
        return ownerID;
    }

    Set<Long> getChannels() {
        return Collections.unmodifiableSet(channels);
    }

    Set<Long> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
}
