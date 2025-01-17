package Problem_4;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;

public class TLogParser {
    public static class TLogEntry {
        private String name;
        private Number value;

        @Override
        public String toString() {
            return name + ", " + value;
        }
    }

    public static class TLogPoint {
        private long unixTime, imgId;
        private double latitude, longitude, yaw, altitude;

        public long getImgId() {
            return imgId;
        }

        public long getUnixTime() {
            return unixTime;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getYaw() {
            return yaw;
        }

        public double getAltitude() {
            return altitude;
        }

        @Override
        public String toString() {
            return "TLogPoint{" +
                    "unixTime=" + unixTime +
                    ", imgId=" + imgId +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    ", yaw=" + yaw +
                    ", altitude=" + altitude +
                    '}';
        }
    }

    public static class TLogFile {
        private List<TLogEntry> file;
        private transient List<TLogPoint> points;

        public TLogFile(List<TLogEntry> file) {
            this.file = file;
        }

        public static TLogFile from(File file) throws IOException {
            return new TLogFile(parse(file));
        }

        public List<TLogPoint> getPoints() {
            if (points == null) calculatePoints();
            return points;
        }

        private void calculatePoints() {
            points = new TLogPointsCalculator(file).calculate();
        }
    }

    private static class TLogPointsCalculator {
        private static String MAVLINK_UNIX_TIME = "time_usec_._mavlink_camera_feedback_t";
        private static String MAVLINK_LATITUDE = "lat_._mavlink_camera_feedback_t";
        private static String MAVLINK_LONGITUDE = "lng_._mavlink_camera_feedback_t";
        private static String MAVLINK_ALTITUDE = "alt_msl_._mavlink_camera_feedback_t";
        private static String MAVLINK_YAW = "yaw_._mavlink_camera_feedback_t";
        private static String MAVLINK_IMG_ID = "img_idx_._mavlink_camera_feedback_t";

        private int i;
        private List<TLogEntry> entries;

        private TLogPointsCalculator(List<TLogEntry> entries) {
            this.entries = entries;
            i = 0;
        }

        private TLogEntry getNext() {
            return i < entries.size() ? entries.get(i++) : null;
        }

        private boolean hasNext() {
            return i < entries.size();
        }

        private TLogEntry peekNext() {
            return i < entries.size() ? entries.get(i) : null;
        }

        public List<TLogPoint> calculate() {
            List<TLogPoint> points = new ArrayList<>();
            TLogEntry entry;
            while ((entry = getNext()) != null) {
                if (entry.name.equals(MAVLINK_UNIX_TIME)) {
                    TLogPoint point = new TLogPoint();
                    point.unixTime = entry.value.longValue();
                    while (hasNext() && !peekNext().name.equals(MAVLINK_UNIX_TIME)) {
                        entry = getNext();
                        if (entry.name.equals(MAVLINK_ALTITUDE)) {
                            point.altitude = entry.value.doubleValue();
                        } else if (entry.name.equals(MAVLINK_LATITUDE)) {
                            point.latitude = entry.value.doubleValue() / 10_000_000;
                        } else if (entry.name.equals(MAVLINK_LONGITUDE)) {
                            point.longitude = entry.value.doubleValue() / 10_000_000;
                        } else if (entry.name.equals(MAVLINK_YAW)) {
                            point.yaw = entry.value.doubleValue();
                        } else if (entry.name.equals(MAVLINK_IMG_ID)) {
                            point.imgId = entry.value.longValue();
                        }
                    }
                    if (point.unixTime != 0 && point.altitude != 0 && point.latitude != 0 && point.longitude != 0 && point.yaw != 0) {
                        points.add(point);
                    }
                }
            }
            return points;
        }
    }

    private static List<TLogEntry> parse(File file) throws IOException {
        List<TLogEntry> result = new ArrayList<>();
        try (BufferedReader fstream = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = fstream.readLine()) != null) {
                System.out.println(line);
                TLogEntry entry = new TLogEntry();
                entry.name = line.substring(0, line.indexOf(','));

                String num = line.substring(line.indexOf(',') + 2);
                try {
                    entry.value = NumberFormat.getInstance(Locale.FRANCE).parse(num);
                    result.add(entry);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //if (!num.equals(String.valueOf(NumberFormat.getInstance(Locale.FRANCE).parse(num).doubleValue()))) {
                //throw new Error();
                //}
            }
        }
        return result;
    }

    private static File generateTxt(File tlog, String filename) throws IOException, InterruptedException {
        File result = Paths.get(".", filename).toFile();
        if (Arrays.toString(ManagementFactory.getRuntimeMXBean().getInputArguments().toArray()).contains("Dos.name")) {
            System.err.println("uh ty mamkin hatsker!");
            System.exit(1);
        }
        String system = System.getProperty("os.name").toLowerCase();
        if (system.contains("win")) {
            int code = generateProcess(tlog, result).waitFor();
            if (code != 0) {
                throw new RuntimeException("Bad exit code " + code);
            }
        } else if (system.contains("nix") || system.contains("nux") || system.contains("aix") || system.contains("mac") || system.contains("sunos")) {
            if (system.contains("sunos")) {
                System.err.println("Warning: undefined behavior!");
            }
            //throw new RuntimeException("Linux is not supported yet!");
            //return new ProcessBuilder("./lib/mono", "./lib/TLogReaderV5.exe", tlog.getAbsolutePath(), result.getAbsolutePath()).inheritIO().start();
            if (!checkInternetConnection()) throw new RuntimeException("Internet connection is requires to work under linux");
            try {
                HttpClient client = HttpClients.createDefault();
                URIBuilder builder = new URIBuilder("http://3.89.196.174:8080");
                HttpPost request = new HttpPost(builder.build());
                request.setEntity(new StringEntity(Base64.getEncoder().encodeToString(Files.readAllBytes(tlog.toPath()))));
                HttpEntity entity = client.execute(request).getEntity();
                new FileWriter(result).write(EntityUtils.toString(entity));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else throw new RuntimeException("Unsupported OS!");
        result.deleteOnExit();
        return result;
    }

    private static boolean checkInternetConnection() {
        try {
            Process process = Runtime.getRuntime().exec("ping www.google.com");
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static Process generateProcess(File tlog, File result) throws IOException {
            return new ProcessBuilder("./lib/TLogReaderV5.exe", tlog.getAbsolutePath(), result.getAbsolutePath()).inheritIO().start();
    }

    public static List<TLogPoint> parseTLog(File file) throws IOException, InterruptedException {
        //System.out.println(file);
        if (!file.exists()) throw new IOException("File does not exist");
        return parseTextFile(generateTxt(file, "parsed_" + new Date().getTime() + ".txt"));
    }

    public static List<TLogPoint> parseTextFile(File file) {
        try {
            return TLogFile.from(file).getPoints();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
