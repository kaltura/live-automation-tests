package tests;

import actions.configurations.ConfigurationReader;
import actions.configurations.EncoderConfig;
import actions.configurations.TestConfig;
import actions.encoders.Encoder;
import actions.encoders.ImageUtils;
import actions.streamdownloader.StreamDownloader;
import actions.streamdownloader.StreamDownloaderFactory;
import actions.streamdownloader.hls.TsFilesComparator;
import actions.utils.GlobalContext;
import actions.utils.ManifestUrlBuilder;
import actions.utils.MultiBitrateResults;
import actions.utils.ProcessHandler;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
* Created by asher.saban on 2/25/2015.
*/
public class MultiBitrateSyncTest {

    private TestConfig config;
    private String dest;
    private Encoder encoder;
    private StreamDownloader downloader;

    private static String generateRandomSuffix() {
        return new SimpleDateFormat("yyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    }

    private TestConfig getTestConfiguration(String configFileName) throws Exception {
        //read configuration file:
        URL u = getClass().getResource("/" + configFileName);
        if (u == null) {
            throw new Exception("Configuration file: " + configFileName + " not found.");
        }
        return ConfigurationReader.getTestConfigurations(u.getPath());
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public void initializeTest() throws Exception {
        config = getTestConfiguration("test-conf.json");    //TODO

        //initialize encoder
        EncoderConfig encoderConfig = config.getEncoder();
        encoder = new Encoder(encoderConfig.getEncoderName(),encoderConfig.getPathToExecutable(),encoderConfig.getArgs());

        //initialize image utils
        ImageUtils.initializeImageUtils(config.getPathToFfmpeg());

        //initialize stream downloader
        downloader = StreamDownloaderFactory.getDownloader(config.getStreamType());
    }

    private void comment(String msg) {
        Reporter.log(msg,true);
    }

    @Test
    public void streamVideoAndSleep() throws IOException {
        comment("About to stream video");
        encoder.startStream();
        comment("Sleeping");
        sleep(90);
        comment("Done sleeping");

    }

    @Test(dependsOnMethods = "streamVideoAndSleep")
    public void downloadTsFiles() throws URISyntaxException {
        comment("Building manifest url");
        URI uri = ManifestUrlBuilder.buildManifestUrl(config.getServiceUrl(), config.getEntryId(), config.getPartnerId());

        dest = config.getDestinationFolder() + "/" + generateRandomSuffix();
        int duration = config.getTestDuration();
        comment("Manifest URL:" + uri);
        comment("Destination folder:" + dest);
        comment("Test duration:" + duration);

        try {
            downloader.downloadFiles(uri.toString(), dest);
            Thread.sleep(duration * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            comment("Shutting down downloader");
            downloader.shutdownDownloader();

            comment("Stopping streaming");
            encoder.stopStreaming();
        }
    }

    @Test(dependsOnMethods = "downloadTsFiles")
    public void compareFiles() {
        comment("Comparing files");
        Assert.assertEquals(true,TsFilesComparator.compareFiles(new File(dest), (Integer) GlobalContext.getValue("NUM_STREAMS")));
    }

    @AfterClass
    public void clearResources() {
        comment("Shutting down ProcessHandler");
        ProcessHandler.shutdown();
    }



//    @Test(enabled = false)
//    public void testMultiBitrate() throws IOException, URISyntaxException {
////        Reporter.log("------------------",true);
////        Reporter.log(config.getEntryId(),true);
//
//
//        comment("About to stream video");
//        ProcessBuilder pb = ProcessHandler.createProcess((String) config.getOtherProperties().get("ffmpegStreamCommand"));
//        Process p = ProcessHandler.start(pb);
//
//        comment("Sleeping..");
//        sleep(120);
//        comment("Done sleeping..");
//
//        comment("Building manifest url");
//        URI uri = ManifestUrlBuilder.buildManifestUrl(config.getServiceUrl(), config.getEntryId(), config.getPartnerId());
//
//        String dest = config.getDestinationFolder() + "/" + generateRandomSuffix();
//        int duration = config.getTestDuration();
//        comment("Manifest URL:" + uri);
//        comment("Destination folder:" + dest);
//        comment("Test duration:" + duration);
//
//        HLSDownloader hlsDownloader = new HLSDownloader();
//        try {
//            hlsDownloader.downloadHLSFiles(uri.toString(), dest);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            Thread.sleep(duration * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        comment("Shutting down downloader");
//        hlsDownloader.shutdownDownloader();
//
//        comment("Stopping streaming");
//        ProcessHandler.destroy(p);
//        comment("Closing ProcessHandler");
//
//        comment("Comparing files");
//        Map<Integer, MultiBitrateResults> results = new HashMap<>();
//        TsFilesComparator.compareFiles(new File(dest), results);
//
//        System.out.println("Shutting down ProcessHandler");
//        ProcessHandler.shutdown();
//    }
}
