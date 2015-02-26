package actions.streamdownloader.hls;

import actions.utils.HttpUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by asher.saban on 2/17/2015.
 */
public class HLSDownloaderWorker implements Runnable {

    private static final int DEFAULT_TS_DURATION = 10;
    private String url;
    private String destinationPath;
    private int lastTsNumber;
    private volatile boolean stopDownloading;

    public HLSDownloaderWorker(String url, String destinationPath) {
        this.url = url;
        this.destinationPath = destinationPath;
        this.lastTsNumber = -1;
        this.stopDownloading = false;
    }

    public void stopDownload(){
        this.stopDownloading = true;
    }

    @Override
    public void run() {

        //extract base url:
        String baseUrl = url.substring(0, url.lastIndexOf("/"));
        String playlistFileName = url.substring(url.lastIndexOf("/")+1);
        String dest = destinationPath + "/" + playlistFileName;

        int counter = 0;
        while (true) {

            if (stopDownloading) {
                System.out.println("Shutting down downloader...");
                return;
            }

            counter++;
            System.out.println("iteration: " + counter);

            //download m3u8:
            CloseableHttpClient httpClient = HttpUtils.getHttpClient();
            String content = null;
            try {
                content = HttpUtils.doGetRequest(httpClient, url);
            } catch (IOException e) {
                System.out.println("Get request failed.");
                e.printStackTrace();
                continue; //TODO
            }

            long downloadTime = System.currentTimeMillis();
            int tsDuration = 0;

            //get duration: search #EXT-X-TARGETDURATION
            String[] lines = content.split("\n");
            int numLines = lines.length;

            for (int i = 0; i < numLines; i++) {
                String line = lines[i];
                if (line.startsWith("#EXT-X-TARGETDURATION:")) {
                    System.out.println(line);
                    tsDuration = parseStringToInt(line.substring("#EXT-X-TARGETDURATION:".length()).trim());
                }
                //a .ts file
                else if (line.startsWith("#EXTINF:")) {
                    System.out.println(line);
                    //extract ts file:
                    int j = i + 1;
                    while (j < lines.length && (lines[j].startsWith("#") || lines[j].equals("")) && !lines[j].trim().endsWith(".ts")) {
                        j++;
                    }

                    String tsName = lines[j];
                    System.out.println("Found .ts file: " + tsName);

                    //parse ts address to relative address and file name:
                    File ts = new File(tsName);
                    String relativePath = ts.getParent();
                    //in case relative path is null
                    relativePath = (relativePath == null) ? "" : relativePath;
                    String fileName = ts.getName();

                    //get the ts number:
                    int tsNumber = -1;
                    try {
                        tsNumber = getTsNumber(fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }

                    //skip .ts files we already downloaded in previous iterations
                    if (tsNumber <= lastTsNumber) {
                        continue;
                    }
                    lastTsNumber = tsNumber;

                    //download ts:
                    try {
                        HttpUtils.downloadFile(baseUrl + "/" + tsName, dest + "/" + fileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    i = j;
                }
            }

            //sleep for at least the maxminal duration of the .ts that is specified in the m3u8 file - #EXT-X-TARGETDURATION
            long endDownloadTime = System.currentTimeMillis();
            System.out.println();
            System.out.println("download took: " + (endDownloadTime - downloadTime) + " ms. max duration: " + tsDuration * 1000);
            System.out.println();
            if ((endDownloadTime - downloadTime) < (tsDuration * 1000)) {
                try {
                    System.out.println("sleeping for: " + ((tsDuration * 1000) - (endDownloadTime - downloadTime)));
                    Thread.sleep((tsDuration * 1000) - (endDownloadTime - downloadTime));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int getTsNumber(String tsName) throws Exception {
        Pattern pattern = Pattern.compile("\\D(\\d+)\\.ts");
        Matcher matcher = pattern.matcher(tsName);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        throw new Exception("ts with name: " + tsName + " does not contain ts number");
    }

    private int parseStringToInt(String str) {
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            System.out.println("#EXT-X-TARGETDURATION was not found in m3u8 file.");
            return DEFAULT_TS_DURATION;
        }
    }
}
