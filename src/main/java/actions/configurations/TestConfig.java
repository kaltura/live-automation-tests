package actions.configurations;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by asher.saban on 2/22/2015.
 */
public class TestConfig {

    @JsonProperty("test-name")
    private String testName;

    @JsonProperty("stream-type")
    private String streamType;

    @JsonProperty("encoder")
    private EncoderConfig encoder;

    @JsonProperty("test-duration")
    private int testDuration;

    @JsonProperty("path-to-ffmpeg")
    private String pathToFfmpeg;

    @JsonProperty("destination-folder")
    private String destinationFolder;

    @JsonProperty("delete-files")
    private String deleteFiles;

    @JsonProperty("service-url")
    private String ServiceUrl;

    @JsonProperty("partner-id")
    private String partnerId;

    @JsonProperty("entry-id")
    private String entryId;

    private final Map<String , Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    private void set(String name, Object value) {
        otherProperties.put(name, value);
    }

    public String getTestName() {
        return testName;
    }

    public EncoderConfig getEncoder() {
        return encoder;
    }

    public int getTestDuration() {
        return testDuration;
    }

    public String getPathToFfmpeg() {
        return pathToFfmpeg;
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public String getStreamType() {
        return streamType;
    }

    public String getDeleteFiles() {
        return deleteFiles;
    }

    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    public String getServiceUrl() {
        return ServiceUrl;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getEntryId() {
        return entryId;
    }
}
