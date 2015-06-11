package tests;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import actions.configurations.ConfigurationReader;
import actions.configurations.TestConfig;
import actions.kaltura.StartSession;
import actions.streamdownloader.hls.HLSDownloader;

import com.kaltura.client.KalturaApiException;
import com.kaltura.client.KalturaClient;
import com.kaltura.client.enums.KalturaNullableBoolean;
import com.kaltura.client.types.KalturaLiveStreamEntry;
import com.kaltura.client.types.KalturaLiveStreamEntryFilter;
import com.kaltura.client.types.KalturaLiveStreamListResponse;

public class PartnerMonitor {

	private TestConfig config;
	private KalturaClient client;
	
	public static void main(String[] args) throws Exception {
		PartnerMonitor monitor = new PartnerMonitor();
		monitor.execute();
	}
	 
	public void execute() throws Exception {
		config = getTestConfiguration("test-conf.json");
		int partnerId = Integer.valueOf(config.getPartnerId());
		StartSession session = new StartSession(partnerId,
				config.getServiceUrl(), config.getAdminSecret());
		client = session.execute();
		
		Set<String> entries = new HashSet<>();

		while(true) {
			System.out.println("...");
			Set<String> curEntries = getEntries();
			for (String entryId : curEntries) {
				if(!entries.contains(entryId)) {
					// Create downloaders threads
					System.out.println("### Create new thread for entry - " + entryId);
					Timer timer = new Timer(entryId, true);
					timer.schedule(new Downloader(entryId, true, config.getDestinationFolder()), 1);
					timer = new Timer(entryId, true);
					timer.schedule(new Downloader(entryId, false, config.getDestinationFolder()), 1);
				}
			}
			
			entries = curEntries;
			Thread.sleep(60*1000);
		}
	}
	
	private TestConfig getTestConfiguration(String configFileName)
			throws Exception {
		// read configuration file:
		URL u = getClass().getResource("/" + configFileName);
		if (u == null) {
			throw new Exception("Configuration file: " + configFileName
					+ " not found.");
		}
		return ConfigurationReader.getTestConfigurations(u.getPath());
    }
	
	private Set<String> getEntries() throws KalturaApiException {
		Set<String> result = new HashSet<String>();
		
		KalturaLiveStreamEntryFilter filter = new KalturaLiveStreamEntryFilter();
		filter.isLive = KalturaNullableBoolean.TRUE_VALUE;
		KalturaLiveStreamListResponse results = client.getLiveStreamService()
				.list(filter);
		if (results.totalCount == 0)
			return result;

		for (KalturaLiveStreamEntry entry : results.objects) {
			result.add(entry.id);
		}
		return result;
	}
	
	public class Downloader extends TimerTask {
		
		private static final String PREFIX = "http://kalsegsec-a.akamaihd.net/dc-1/m/ny-live-publish1/kLive/smil:";
		private static final String SUFFIX = "_all.smil/playlist.m3u8";
		private final String downloadPath;// = "/root/output";
		
		private String entryId;
		private boolean useDvr;
		
		Downloader (String entryId, boolean useDvr, String destinationPath) {
			this.entryId = entryId;
			this.useDvr = useDvr;
			this.downloadPath = destinationPath;
		}

		@Override
		public void run() {
			String manifestUrl = PREFIX + entryId + SUFFIX;
			if(this.useDvr)
				manifestUrl += "?DVR";
			
			String downloadDir = downloadPath + "/" + entryId;
			if(this.useDvr)
				downloadDir += "_DVR";
						
			HLSDownloader d = new HLSDownloader();
			try {
				d.downloadFiles(manifestUrl, downloadDir);
			} catch (Exception e) {
				System.out.println("Failed to download content");
				e.printStackTrace();
			}
		}
	}
}
