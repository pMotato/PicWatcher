package com.yich.layout.picwatcherlib.progress;

public interface ProgressListener {

    void progress(long bytesRead, long contentLength, boolean done);

}
