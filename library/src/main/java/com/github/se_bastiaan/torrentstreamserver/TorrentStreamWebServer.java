package com.github.se_bastiaan.torrentstreamserver;

import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstreamserver.nanohttpd.NanoHTTPD;
import com.github.se_bastiaan.torrentstreamserver.nanohttpd.SimpleWebServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.github.se_bastiaan.torrentstreamserver.FileType.AVI;
import static com.github.se_bastiaan.torrentstreamserver.FileType.MKV;
import static com.github.se_bastiaan.torrentstreamserver.FileType.MP4;
import static com.github.se_bastiaan.torrentstreamserver.FileType.SRT;
import static com.github.se_bastiaan.torrentstreamserver.FileType.VTT;

public class TorrentStreamWebServer extends SimpleWebServer {

    private HashMap<String, FileType> EXTENSIONS;

    private Torrent torrent;
    private File srtSubtitleFile;
    private File vttSubtitleFile;

    public TorrentStreamWebServer(String host, int port) {
        super(host, port, true);

        EXTENSIONS = new HashMap<>();
        FileType[] FILE_TYPES = {MP4, AVI, MKV};
        for (FileType localFileType : FILE_TYPES) {
            EXTENSIONS.put(localFileType.extension, localFileType);
        }
        EXTENSIONS.put("3gp", MP4);
        EXTENSIONS.put("mov", MP4);
    }

    public void setVideoTorrent(Torrent torrent) {
        this.torrent = torrent;
    }

    public void setSrtSubtitleLocation(File srtSubtitleFile) {
        this.srtSubtitleFile = srtSubtitleFile;
    }

    public void setVttSubtitleLocation(File vttSubtitleFile) {
        this.vttSubtitleFile = vttSubtitleFile;
    }

    public String getStreamUrl() {
        File file = torrent.getVideoFile();
        return "http://" + getHostname() + ":" + getListeningPort() + "/video" + file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf('.'));
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        Response response;
        String extension = uri.substring(uri.lastIndexOf('.') + 1);
        FileType fileType;

        if(EXTENSIONS.containsKey(extension)) {
            fileType = EXTENSIONS.get(extension);
            if(torrent != null) {
                response = serveTorrent(torrent, session);
                fileType.setHeaders(response);
            } else {
                response = getNotFoundResponse();
            }
        } else if(extension.equals(SRT.extension)) {
            fileType = SRT;
            if(srtSubtitleFile != null) {
                response = serveFile(uri, session.getHeaders(), srtSubtitleFile, fileType.mimeType);
                fileType.setHeaders(response);
            } else {
                response = getNotFoundResponse();
            }

        } else if(extension.equals(VTT.extension)) {
            fileType = VTT;
            if(vttSubtitleFile != null) {
                response = serveFile(uri, session.getHeaders(), vttSubtitleFile, fileType.mimeType);
                fileType.setHeaders(response);
            } else {
                response = getNotFoundResponse();
            }

        } else {
            response = getForbiddenResponse("You can't access this location");
        }

        return response;
    }

    private Response serveTorrent(Torrent torrent, IHTTPSession session) {
        File file = torrent.getVideoFile();

        if (file == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "", "");
        }

        Map<String, String> header = session.getHeaders();
        String mime = getMimeTypeForFile(file.getAbsolutePath());

        try {
            Response res;
            String etag = Integer.toHexString((file.getAbsolutePath() + file.length()).hashCode());

            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");

            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            String ifRange = header.get("if-range");
            boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

            String ifNoneMatch = header.get("if-none-match");
            boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    torrent.setInterestedBytes(startFrom);

                    InputStream inputStream = torrent.getVideoStream();
                    inputStream.skip(startFrom);

                    res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, inputStream, newLen);
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes */" + fileLen);
                    res.addHeader("ETag", etag);
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified

                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    torrent.setInterestedBytes(0);

                    InputStream inputStream = torrent.getVideoStream();
                    res = newFixedLengthResponse(Response.Status.OK, mime, inputStream, (int) file.length());
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }

            return res;
        } catch (IOException ioe) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Forbidden");
        }
    }



}
