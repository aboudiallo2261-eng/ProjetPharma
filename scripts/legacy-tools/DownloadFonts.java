import java.io.*;
import java.net.*;

public class DownloadFonts {
    public static void main(String[] args) throws Exception {
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        File dir = new File("src/main/resources/fonts");
        if (!dir.exists()) dir.mkdirs();
        
        System.out.println("Downloading Inter-Regular...");
        download("https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Regular.ttf", "src/main/resources/fonts/Inter-Regular.ttf");
        System.out.println("Downloading Inter-Bold...");
        download("https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Bold.ttf", "src/main/resources/fonts/Inter-Bold.ttf");
        System.out.println("Done.");
    }

    private static void download(String urlStr, String file) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setInstanceFollowRedirects(true);
        int responseCode = httpConn.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = httpConn.getHeaderField("Location");
            httpConn = (HttpURLConnection) new URL(newUrl).openConnection();
        }
        
        InputStream is = httpConn.getInputStream();
        FileOutputStream fos = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int length;
        while ((length = is.read(buffer)) > 0) fos.write(buffer, 0, length);
        fos.close();
        is.close();
    }
}
