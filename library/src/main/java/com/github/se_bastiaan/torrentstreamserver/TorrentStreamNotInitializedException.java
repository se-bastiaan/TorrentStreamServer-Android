package com.github.se_bastiaan.torrentstreamserver;

public class TorrentStreamNotInitializedException extends Exception {

    public TorrentStreamNotInitializedException() {
        super("TorrentStream has not been initialized yet. Please start TorrentStream before starting a stream.");
    }
}
