package actions.utils;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class QRCodeReader {

    public static String readQRCode(File filePath) throws IOException, NotFoundException {

        Map<DecodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<>();
        return readQRCode(filePath, hintMap);
    }

    public static String readQRCode(File filePath, Map<DecodeHintType, ?> hintMap) throws IOException, NotFoundException {
        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                    new BufferedImageLuminanceSource(
                            ImageIO.read(is))));
            Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap, hintMap);
            return qrCodeResult.getText();
        } finally {
            //close the stream, otherwise it will lock all the jpeg files
            if (is != null) {
                is.close();
            }
        }
    }
}
