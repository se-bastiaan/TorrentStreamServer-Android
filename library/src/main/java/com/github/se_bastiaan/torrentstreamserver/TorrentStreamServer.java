package com.github.se_bastiaan.torrentstreamserver;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TorrentStreamServer {

    private static TorrentStreamServer instance;

    private String serverHost;
    private Integer serverPort;
    private final List<TorrentServerListener> listeners = new ArrayList<>();

    private TorrentOptions torrentOptions;
    private TorrentStream torrentStream;
    private TorrentStreamWebServer torrentStreamWebServer;

    private TorrentListener internalListener = new InternalTorrentServerListener();

    private TorrentStreamServer() {
        this.torrentOptions = new TorrentOptions.Builder().build();
    }

    public static TorrentStreamServer getInstance() {
        if (instance == null) {
            instance = new TorrentStreamServer();
        }
        return instance;
    }

    public TorrentOptions getOptions() {
        return torrentOptions;
    }

    public void setTorrentOptions(TorrentOptions torrentOptions) {
        this.torrentOptions = torrentOptions;
        if (this.torrentStream != null) {
            this.torrentStream.setOptions(torrentOptions);
        }
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public boolean isStreaming() {
        if (this.torrentStream == null) {
            return false;
        }
        return this.torrentStream.isStreaming();
    }

    public void resumeSession() {
        if (torrentStream != null) {
            this.torrentStream.resumeSession();
        }
    }

    public void pauseSession() {
        if (torrentStream != null) {
            this.torrentStream.pauseSession();
        }
    }

    public String getCurrentTorrentUrl() {
        if (torrentStream == null) {
            return null;
        }
        return this.torrentStream.getCurrentTorrentUrl();
    }

    public Integer getTotalDhtNodes() {
        if (torrentStream == null) {
            return 0;
        }
        return this.torrentStream.getTotalDhtNodes();
    }

    public Torrent getCurrentTorrent() {
        if (torrentStream == null) {
            return null;
        }
        return this.torrentStream.getCurrentTorrent();
    }

    public void addListener(TorrentServerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(TorrentServerListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void startTorrentStream() {
        this.torrentStream = TorrentStream.init(torrentOptions);
        this.torrentStream.addListener(internalListener);
    }

    public void stopTorrentStream() {
        if (this.torrentStream != null && this.torrentStream.isStreaming()) {
            this.torrentStream.stopStream();
        }
        this.torrentStream = null;
    }

    /**
     * Start stream download for specified torrent
     *
     * @param torrentUrl {@link String} .torrent or magnet link
     */
    public void startStream(String torrentUrl) throws IOException, TorrentStreamNotInitializedException {
        startStream(torrentUrl, null, null);
    }

    /**
     * Start stream download for specified torrent
     *
     * @param torrentUrl {@link String} .torrent or magnet link
     * @param srtSubtitleFile {@link File} SRT subtitle
     * @param vttSubtitleFile {@link File} VTT subtitle
     */
    public void startStream(String torrentUrl, File srtSubtitleFile, File vttSubtitleFile) throws TorrentStreamNotInitializedException, IOException {
        if (this.torrentStream == null) {
            throw new TorrentStreamNotInitializedException();
        }
        this.torrentStream.startStream(torrentUrl);

        this.torrentStreamWebServer = new TorrentStreamWebServer(serverHost, serverPort);
        this.torrentStreamWebServer.setSrtSubtitleLocation(srtSubtitleFile);
        this.torrentStreamWebServer.setVttSubtitleLocation(vttSubtitleFile);
        this.torrentStreamWebServer.start();
    }

    /**
     * Stop current torrent stream
     */
    public void stopStream() {
        if (this.torrentStreamWebServer != null && this.torrentStreamWebServer.wasStarted()) {
            this.torrentStreamWebServer.stop();
        }

        if (this.torrentStream != null && this.torrentStream.isStreaming()) {
            this.torrentStream.stopStream();
        }
    }

    private class InternalTorrentServerListener implements TorrentServerListener {
        @Override
        public void onServerReady(String url) {
            for (TorrentServerListener listener : listeners) {
                listener.onServerReady(url);
            }
        }

        @Override
        public void onStreamPrepared(Torrent torrent) {
            for (TorrentServerListener listener : listeners) {
                listener.onStreamPrepared(torrent);
            }
        }

        @Override
        public void onStreamStarted(Torrent torrent) {
            for (TorrentServerListener listener : listeners) {
                listener.onStreamStarted(torrent);
            }
        }

        @Override
        public void onStreamError(Torrent torrent, Exception e) {
            for (TorrentServerListener listener : listeners) {
                listener.onStreamError(torrent, e);
            }
        }

        @Override
        public void onStreamReady(Torrent torrent) {
            for (TorrentServerListener listener : listeners) {
                listener.onStreamReady(torrent);
            }

            torrentStreamWebServer.setVideoTorrent(torrent);
            onServerReady(torrentStreamWebServer.getStreamUrl());
        }

        @Override
        public void onStreamProgress(Torrent torrent, StreamStatus streamStatus) {
            for (TorrentServerListener listener : listeners) {
                listener.onStreamProgress(torrent, streamStatus);
            }
        }

        @Override
        public void onStreamStopped() {
            for (TorrentServerListener listener : listeners) {
                listener.onStreamStopped();
            }
        }
    }

}
