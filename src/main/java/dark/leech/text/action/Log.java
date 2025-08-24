package dark.leech.text.action;

import dark.leech.text.util.FileUtils;
import dark.leech.text.util.SettingUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Long on 8/20/2016.
 */
public class Log {
    private static File log;

    public static void gen() {
        Date todaysDate = new Date();
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String date = "Logs_" + df.format(todaysDate) + ".txt";
        File dir = new File(FileUtils.validate(SettingUtils.WORKPATH + "/tools/logs"));
        if (!dir.exists())
            dir.mkdir();
        log = new File(FileUtils.validate(SettingUtils.WORKPATH + "/tools/logs/" + date));
        if (!log.exists())
            try {
                log.createNewFile();
            } catch (IOException e) {
            }
    }

    public static void add(String logStr) {
        System.out.println(logStr);
        if (log == null) gen();
        Date todaysDate = new Date();
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        FileUtils.add2file("\n----------------" + df.format(todaysDate) + "----------------\n", log);
        FileUtils.add2file(logStr, log);
    }

    public static void add(Exception e) {
        e.printStackTrace();
        if (log == null) gen();
        Date todaysDate = new Date();
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        FileUtils.add2file("\n----------------" + df.format(todaysDate) + "----------------\n", log);
        FileUtils.add2file(Arrays.toString(e.getStackTrace()), log);
    }
}
