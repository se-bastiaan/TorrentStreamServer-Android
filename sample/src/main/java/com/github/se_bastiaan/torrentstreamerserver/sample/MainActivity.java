/*
 *
 *  * This file is part of TorrentStreamer-Android.
 *  *
 *  * TorrentStreamer-Android is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Lesser General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * TorrentStreamer-Android is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with TorrentStreamer-Android. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.se_bastiaan.torrentstreamerserver.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener;
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamNotInitializedException;
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamServer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity implements TorrentServerListener {

    private static final String TORRENT = "Torrent";
    private Button button;
    private ProgressBar progressBar;
    private TorrentStreamServer torrentStreamServer;

    private String streamUrl = "magnet:?xt=urn:btih:E55DF583E4F8752E9E4AEB71F67A17FBF6983C68&dn=Jack+Reacher+Never+Go+Back+2016+HD-TS+x264-CPG&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.ilibr.org%3A80%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Feddie4.nl%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.ilibr.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.ex.ua%3A80%2Fannounce&tr=udp%3A%2F%2F9.rarbg.to%3A2710%2Fannounce&tr=udp%3A%2F%2Fipv4.tracker.harry.lu%3A80%2Fannounce&tr=udp%3A%2F%2Fexplodie.org%3A6969%2Fannounce&tr=udp%3A%2F%2Fshadowshq.yi.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Fp4p.arenabg.ch%3A1337%2Fannounce&tr=udp%3A%2F%2Fp4p.arenabg.com%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Fcoppersurfer.tk%3A6969%2Fannounce";

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            progressBar.setProgress(0);
            if(torrentStreamServer.isStreaming()) {
                torrentStreamServer.stopStream();
                button.setText("Start stream");
                return;
            }
            try {
                torrentStreamServer.startStream(streamUrl);
            } catch (IOException | TorrentStreamNotInitializedException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error!", Toast.LENGTH_SHORT).show();
            }
            button.setText("Stop stream");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String action = getIntent().getAction();
        Uri data = getIntent().getData();
        if (action != null && action.equals(Intent.ACTION_VIEW) && data != null) {
            try {
                streamUrl = URLDecoder.decode(data.toString(), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        TorrentOptions torrentOptions = new TorrentOptions.Builder()
                .saveLocation(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                .removeFilesAfterStop(true)
                .build();

        torrentStreamServer = TorrentStreamServer.getInstance();
        torrentStreamServer.setTorrentOptions(torrentOptions);
        torrentStreamServer.setServerHost("127.0.0.1");
        torrentStreamServer.setServerPort(8080);
        torrentStreamServer.startTorrentStream();
        torrentStreamServer.addListener(this);

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(onClickListener);
        progressBar = (ProgressBar) findViewById(R.id.progress);

        progressBar.setMax(100);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        torrentStreamServer.stopTorrentStream();
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {
        Log.d(TORRENT, "OnStreamPrepared");
        torrent.startDownload();
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        Log.d(TORRENT, "onStreamStarted");
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        Log.e(TORRENT, "onStreamError", e);
        button.setText("Start stream");
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        progressBar.setProgress(100);
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {
        if(status.bufferProgress <= 100 && progressBar.getProgress() < 100 && progressBar.getProgress() != status.bufferProgress) {
            Log.d(TORRENT, "Progress: " + status.bufferProgress);
            progressBar.setProgress(status.bufferProgress);
        }
    }

    @Override
    public void onStreamStopped() {
        Log.d(TORRENT, "onStreamStopped");
    }

    @Override
    public void onServerReady(String url) {
        Log.d(TORRENT, "onServerReady: " + url);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setDataAndType(Uri.parse(url), "video/mp4");
        startActivity(intent);
    }

}
