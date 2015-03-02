package actions.streamdownloader.hls;

import actions.encoders.ImageUtils;
import actions.utils.MultiBitrateResults;
import actions.utils.QRCodeReader;
import com.google.zxing.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by asher.saban on 2/25/2015.
 */
public class TsFilesComparator {

    private static final Logger log = Logger.getLogger(TsFilesComparator.class);
    private static final long THRESHOLD_IN_MS = 500;

    private static long convertToMs(String code) {
        Pattern pattern = Pattern.compile("(\\d\\d):(\\d\\d):(\\d\\d\\.\\d*)$");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            Integer hours = Integer.valueOf(matcher.group(1));
            Integer minutes = Integer.valueOf(matcher.group(2));
            Double milliseconds = Double.valueOf(matcher.group(3)) * 1000;
            return (long) (milliseconds + minutes * 60 * 1000 + hours * 60 * 60 * 1000);
        }

        //TODO error handling
        return 0;
    }

    private static long getQRCodeFromFile(File tsFile, String jpegFile) throws Exception {
        //save first frame to file:
        ImageUtils.saveFirstFrame(tsFile, new File(jpegFile));   //TODO, be consistent with File objects

        //take QR code:
        try {
            String text = QRCodeReader.readQRCode(new File(jpegFile));
            return convertToMs(text);
        } catch (IOException | NotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static Integer extractTsNumber(String tsName) {
        Pattern pattern = Pattern.compile("\\D(\\d+)\\.ts");
        Matcher matcher = pattern.matcher(tsName);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

    private static boolean analyzeResult(MultiBitrateResults r) {
        boolean success = true;
        long diff = r.getMaxValue() - r.getMinValue();
        log.info("ts with id: " + r.getTsNumber() + ", diff: " + diff + ", min: " + r.getMinValue() + ", max: " + r.getMaxValue() + " . num comparisons: " + r.getNumComparisons());

        if (r.getNumComparisons() < 3) {
            success = false;
            log.error("missing ts files");
        }
        if (diff >= THRESHOLD_IN_MS) {
            success = false;
            log.error("Ts files are not in sync");
        }
        return success;
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

    public static boolean compareFiles(File folderPath, Map<Integer, MultiBitrateResults> results) {

        boolean success = true;
        Collection files = FileUtils.listFiles(folderPath, new String[]{"ts"}, true);
        List<File> sortedFiles = getSortedFilesList(files);

//        HashMap<Integer, MultiBitrateResults> results = new HashMap<>();

        //initialize first
        File prevTsFile = sortedFiles.get(0);  //TODO, index out of bounds
        int prevTsNumber = extractTsNumber(prevTsFile.getName());
        MultiBitrateResults r = new MultiBitrateResults(prevTsNumber);

        for (File currentTsFile : sortedFiles) {
            int currentTsNumber = extractTsNumber(currentTsFile.getName());

            //finished with previous ts file, save it to map and start a new result
            if (currentTsNumber != prevTsNumber) {

                if (!analyzeResult(r)) {
                    success = false;
                }

                results.put(prevTsNumber, r);
                r = new MultiBitrateResults(currentTsNumber);
                prevTsNumber = currentTsNumber;
            }
            String jpegName = currentTsFile.getAbsolutePath() + ".jpeg";

            //delete file if exists
//            File jpegFile = new File(jpegName);
//            /*boolean isDeleted = */jpegFile.delete();  //TODO, be consistent and work only with File obj. what if false? DEADLOCK. solved with -y in ffmpeg
            try {
                r.updateValues(getQRCodeFromFile(currentTsFile, jpegName));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //add the last ones
        results.put(prevTsNumber, r);
        if (!analyzeResult(r)){
            success = false;
        }
        return success;
    }
}
