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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity implements TorrentServerListener {

    private static final String TORRENT = "Torrent";
    private Button button;
    private TextView videoLocationText;
    private ProgressBar progressBar;
    private TorrentStreamServer torrentStreamServer;

    private String streamUrl = "magnet:?xt=urn:btih:88594aaacbde40ef3e2510c47374ec0aa396c08e&dn=bbb%5Fsunflower%5F1080p%5F30fps%5Fnormal.mp4&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80%2Fannounce&ws=http%3A%2F%2Fdistribution.bbb3d.renderfarming.net%2Fvideo%2Fmp4%2Fbbb%5Fsunflower%5F1080p%5F30fps%5Fnormal.mp4";

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

        videoLocationText = (TextView) findViewById(R.id.video_location_text);
        button = (Button) findViewById(R.id.button);
        progressBar = (ProgressBar) findViewById(R.id.progress);

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

        String ipAddress = "127.0.0.1";
        try {
            InetAddress inetAddress = getIpAddress(this);
            if (inetAddress != null) {
                ipAddress = inetAddress.getHostAddress();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        torrentStreamServer = TorrentStreamServer.getInstance();
        torrentStreamServer.setTorrentOptions(torrentOptions);
        torrentStreamServer.setServerHost(ipAddress);
        torrentStreamServer.setServerPort(8080);
        torrentStreamServer.startTorrentStream();
        torrentStreamServer.addListener(this);

        button.setOnClickListener(onClickListener);

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
        videoLocationText.setText(null);
    }

    @Override
    public void onServerReady(String url) {
        Log.d(TORRENT, "onServerReady: " + url);

        videoLocationText.setText(url);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setDataAndType(Uri.parse(url), "video/mp4");
        startActivity(intent);
    }

    public static InetAddress getIpAddress(Context context) throws UnknownHostException {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        if (ip == 0) {
            return null;
        } else {
            byte[] ipAddress = convertIpAddress(ip);
            return InetAddress.getByAddress(ipAddress);
        }
    }

    private static byte[] convertIpAddress(int ip) {
        return new byte[]{
                (byte) (ip & 0xFF),
                (byte) ((ip >> 8) & 0xFF),
                (byte) ((ip >> 16) & 0xFF),
                (byte) ((ip >> 24) & 0xFF)};
    }

}
