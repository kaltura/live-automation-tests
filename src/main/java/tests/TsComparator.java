package tests;

import actions.comparators.imagemagik.ImageMagikComparator;
import actions.configurations.ConfigurationReader;
import actions.configurations.TestConfig;
import actions.encoders.ImageUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by asher.saban on 6/10/2015.
 */
public class TsComparator {

	private static final Logger log = Logger.getLogger(TsComparator.class);
	private static ExecutorService executor = Executors.newFixedThreadPool(20);

	private static File getFirstFrameFromFile(File ts) throws Exception {

		File dest = new File(ts.getAbsolutePath() + ".jpeg");

		//save first frame to file
		ImageUtils.saveFirstFrame(ts, dest);

		return dest;
	}
	private static void compareFiles(List<File> sortedFiles) {

		boolean success = true;

		if (sortedFiles.size() == 0) {
			log.error("Sorted list is empty");
			return;
		}
		File first = sortedFiles.get(0);
		try {
			File firstImage = getFirstFrameFromFile(first);
			for (int i = 1; i < sortedFiles.size(); i++) {

				//get first frame from second file
				final File second = sortedFiles.get(i);
				final File secondImage = getFirstFrameFromFile(second);

				//extract ts numbers
				final int firstNum = extractTsNumber(first.getName());
				int secondNum = extractTsNumber(second.getName());

				if (firstNum == secondNum) {
					final File finalFirstImage = firstImage;
					final File finalFirst = first;
					executor.submit(new Runnable() {
						@Override
						public void run() {
							//compare the pair of images
							ImageMagikComparator imComparator = new ImageMagikComparator(10.0,"c:\\ASHER\\diff"+firstNum+".jpg");
							if (!imComparator.isSimilar(finalFirstImage, secondImage)) {
								log.error("TS files are different: " + finalFirst.getName() + " , " + second.getName());
							}

						}
					});
				}
				first = second;
				firstImage = secondImage;
			}

			executor.shutdown();

		} catch (Exception e) {
			log.error("Failed to extract first frame from " + first.getAbsolutePath(), e);
		}
	}

	private static Integer extractTsNumber(String tsName) {
		Pattern pattern = Pattern.compile("\\D(\\d+)\\.ts");
		Matcher matcher = pattern.matcher(tsName);
		if (matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		}
		return null;
	}

	private static List<File> getSortedFilesList(Collection<File> files) {
		List<File> newList = new ArrayList<>(files);
		Collections.sort(newList, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				Integer num1 = extractTsNumber(o1.getName());
				Integer num2 = extractTsNumber(o2.getName());
				//TODO, NullPointerException
				return num1.compareTo(num2);
			}
		});
		return newList;
	}

	public static boolean compareFiles(File folderPath) {
		boolean success = true;
		Collection files = FileUtils.listFiles(folderPath, new String[]{"ts"}, true);
		List<File> sortedFiles = getSortedFilesList(files);
		compareFiles(sortedFiles);
		return success;
	}

	private static TestConfig getTestConfiguration(String configFileName) throws Exception {
		// read configuration file:
		URL u = TsComparator.class.getResource("/" + configFileName);
		if (u == null) {
			throw new Exception("Configuration file: " + configFileName
					+ " not found.");
		}
		return ConfigurationReader.getTestConfigurations(u.getPath());
	}

	public static void main(String[] args) throws Exception {

		args = new String[]{"C:\\ASHER\\hls-streams\\my-chunks2", "c:/ffmpeg/bin/ffmpeg.exe"};
		if (args.length != 2) {
			System.out.println("Usage: [files destination] [path to ffmpeg]");
			return;
		}

		log.info("Path to ffmpeg: " + args[1]);
		log.info("Folder: " + args[0]);

		ImageUtils.initializeImageUtils(args[1]);
		compareFiles(new File(args[0]));
	}
}

