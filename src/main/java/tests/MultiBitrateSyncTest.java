package tests;

import actions.configurations.ConfigurationReader;
import actions.configurations.EncoderConfig;
import actions.configurations.EntryConfig;
import actions.configurations.TestConfig;
import actions.encoders.Encoder;
import actions.encoders.ImageUtils;
import actions.kaltura.CreateLiveEntry;
import actions.kaltura.StartSession;
import actions.streamdownloader.StreamDownloader;
import actions.streamdownloader.StreamDownloaderFactory;
import actions.streamdownloader.hls.TsFilesComparator;
import actions.utils.GlobalContext;
import actions.utils.ManifestUrlBuilder;
import actions.utils.ProcessHandler;
import actions.utils.StringUtils;
import com.kaltura.client.KalturaApiException;
import com.kaltura.client.KalturaClient;
import com.kaltura.client.types.KalturaLiveStreamEntry;
import org.apache.commons.io.FileUtils;
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

/**
* Created by asher.saban on 2/25/2015.
*/
public class MultiBitrateSyncTest {

    private TestConfig config;
    private String dest;
    private Encoder encoder;
    private StreamDownloader downloader;
    private KalturaClient client;
    private KalturaLiveStreamEntry entry;

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

        //create client:
        int partnerId = Integer.valueOf(config.getPartnerId());
        //TODO, add admin secret to conf file
        StartSession session = new StartSession(partnerId,"http://www.kaltura.com/","ec97d846f44e131988cd98ec8b488e0d");
        client = session.execute();

        //create live entry:
        EntryConfig entryConf = config.getEntryDetails();
        CreateLiveEntry liveEntry = new CreateLiveEntry(client, "auto-test", true, entryConf.isDvr(), entryConf.isRecording(), entryConf.getConversionProfileId());
        entry = liveEntry.execute();
        comment("created test with name: " + entry.name + ", id: " + entry.id);

        //initialize encoder
        EncoderConfig encoderConfig = config.getEncoder();

        //append streams to ffmpeg command. TODO, this is a workaround!
        String ffmpegCommand = encoderConfig.getArgs();
        for (int i = 1; i <= 3; i++) {
            //TODO, primary/secondary
            ffmpegCommand = ffmpegCommand.replace("{" + i + "}", entry.primaryBroadcastingUrl + "/" + entry.id + "_" + i);
        }
        System.out.println("FFMpeg command: " + ffmpegCommand);
        encoder = new Encoder(encoderConfig.getEncoderName(),encoderConfig.getPathToExecutable(),ffmpegCommand);

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
        comment("Done sleeping. verifying encoder is running...");
        Assert.assertTrue(encoder.isRunning());

    }

    @Test(dependsOnMethods = "streamVideoAndSleep")
    public void downloadTsFiles() throws URISyntaxException {
        comment("Building manifest url");
        URI uri = ManifestUrlBuilder.buildManifestUrl(config.getServiceUrl(), entry.id, config.getPartnerId());

        dest = config.getDestinationFolder() + "/" + StringUtils.generateRandomSuffix();
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
        Assert.assertEquals(true, TsFilesComparator.compareFiles(new File(dest), (Integer) GlobalContext.getValue("NUM_STREAMS")));

        //test passed, delete downloads
        try {
            if (config.isDeleteFiles()) {
                comment("Deleting temporary files...");
                FileUtils.forceDelete(new File(dest));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @AfterClass
    public void clearResources() {
        comment("Shutting down ProcessHandler");
        ProcessHandler.shutdown();
    }
}
