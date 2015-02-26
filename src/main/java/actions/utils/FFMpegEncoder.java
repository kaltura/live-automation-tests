package actions.utils;

import actions.utils.ProcessHandler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by asher.saban on 2/16/2015.
 */
public class FFMpegEncoder {

    private static final long TIME_OUT_SECONDS = 20;

    public static void stream() {

    }

    public static void saveFirstFrame(File videoFile, File destination) throws IOException {
        ProcessBuilder pb = ProcessHandler.createProcess(buildFirstFrameCommand(videoFile, destination));
        Process p = ProcessHandler.start(pb);
        ProcessHandler.waitFor(p, TIME_OUT_SECONDS, TimeUnit.SECONDS);
    }

    private static String buildFirstFrameCommand(File videoFile, File destination) {
        //ffmpeg.exe -i {source} -vframes 1 -f image2 {dest}
        String command = "c:\\ffmpeg\\bin\\ffmpeg.exe -y -i " + videoFile.getAbsolutePath() + " -vframes 1 -f image2 "+ destination.getAbsolutePath();
        return command;
    }

}
