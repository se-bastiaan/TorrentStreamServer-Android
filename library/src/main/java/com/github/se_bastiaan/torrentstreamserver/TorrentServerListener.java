package com.github.se_bastiaan.torrentstreamserver;

import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

public interface TorrentServerListener extends TorrentListener {

    void onServerReady(String url);

}
