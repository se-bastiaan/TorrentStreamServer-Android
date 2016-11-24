package com.github.se_bastiaan.torrentstreamserver;


import com.github.se_bastiaan.torrentstreamserver.nanohttpd.NanoHTTPD.Response;

public class FileType {

    public static final FileType
            MP4 = new FileType("mp4", "video/mp4", "DLNA.ORG_PN=AVC_MP4_BL_L3L_SD_AAC;DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000", "Streaming"),
            AVI = new FileType("avi", "video/x-msvideo", "DLNA.ORG_PN=AVC_MP4_BL_L3L_SD_AAC;DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000", "Streaming"),
            MKV = new FileType("mkv", "video/x-matroska", "DLNA.ORG_PN=AVC_MKV_MP_HD_AC3;DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000", "Streaming"),
            SRT = new FileType("srt", "application/x-subrip", "*", ""),
            VTT = new FileType("vtt", "text/vtt", "*", "");

    public final String dlnaContentFeatures;
    public final String dlnaTransferMode;
    public final String extension;
    public final String mimeType;

    private FileType(String extension, String mimeType, String dlnaContentFeatures, String dlnaTransferMode) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.dlnaContentFeatures = dlnaContentFeatures;
        this.dlnaTransferMode = dlnaTransferMode;
    }

    public void setHeaders(Response response) {
        setHeaders(response, null);
    }

    public void setHeaders(Response response, String subtitlesLocation) {
        response.addHeader("contentFeatures.dlna.org", this.dlnaContentFeatures);
        response.addHeader("TransferMode.DLNA.ORG", this.dlnaTransferMode);
        response.addHeader("DAAP-Server", "iTunes/11.0.5 (OS X)");
        response.addHeader("Last-Modified", "2015-01-01T10:00:00Z");
        response.addHeader("Content-Type", this.mimeType);
        if (subtitlesLocation != null) {
            response.addHeader("CaptionInfo.sec", subtitlesLocation);
        }
    }

}