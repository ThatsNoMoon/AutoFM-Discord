package com.thatsnomoon.autofm;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Ben
 */

class AutoPlaylist {

    final List<String> URLS;

    AutoPlaylist() {
        Logger LOG = Main.LOG;
        ArrayList<String> tempList = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get("./playlist.txt"))) {
            stream.forEach(s -> {
                if (!s.startsWith("#")) tempList.add(s);
            });
        } catch (IOException e) {
            LOG.fatal("Exception occurred generating AutoPlaylist:", e);
            System.exit(2);
        }
        if (tempList.isEmpty()) {
            LOG.fatal("Your playlist file must not be empty!");
            System.exit(-1);
        }
        URLS = Collections.unmodifiableList(tempList);
    }
}
