package dark.leech.text.action;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.get.ChapExecute;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.listeners.DownloadListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Pager;
import dark.leech.text.models.Properties;
import dark.leech.text.plugin.PluginManager;

import java.util.List;

import static dark.leech.text.util.SettingUtils.MAX_CONN;

public class Download implements ChangeListener {
    public static final int DOWNLOADING = 0, PAUSE = 1, COMPLETED = 2, CHECKING = 3, CANCEL = 4, ERROR = 5;
    private DownloadListener downloadListener;
    private List<Chapter> chapList;
    private List<Pager> pageList;
    private PluginEntity pluginGetter;
    private Properties properties;
    private int downloaded;
    private int status;
    private int size;
    private int next;

    public Download(Properties properties) {
        PluginManager pluginManager = PluginManager.getManager();
        this.pluginGetter = pluginManager.get(properties.getUrl());
        this.properties = properties;
        this.chapList = properties.getChapList();
        this.pageList = properties.getPageList();
        this.size = properties.getSize();
    }

    public void addDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    // Status action
    public void pause() {
        status = PAUSE;
        downloadListener.updateDownload(downloaded, status);
    }

    public void cancel() {
        status = CANCEL;
        downloadListener.updateDownload(downloaded, status);
    }

    public void resume() {
        status = DOWNLOADING;
        startDownload();
        downloadListener.updateDownload(downloaded, status);
    }

    // Bắt đầu quá trình
    public void startDownload() {
        status = DOWNLOADING;
        next = next + MAX_CONN - 1;
        update();
        for (int i = 0; i < MAX_CONN; i++)
            if (properties.isForum())
                forum(downloaded + i);
            else
                web(downloaded + i);

    }


    private void forum(final int index) {
        if (index >= size) {
            update();
            return;
        }
//        new PageExecute()
//                .clazz(pluginGetter)
//                .listener(this)
//                .charset(properties.getCharset())
//                .path(properties.getSavePath())
//                .applyTo(pageList.get(index))
//                .execute();
    }

    private void web(final int index) {
        if (index >= size) {
            update();
            return;
        }
        new ChapExecute()
                .plugin(pluginGetter)
                .listener(this)
                .charset(properties.getCharset())
                .path(properties.getSavePath())
                .applyTo(chapList.get(index))
                .execute();
    }

    private void update() {
        if (downloaded >= size) {
            downloaded = size;
            status = CHECKING;
            check();
        }
        downloadListener.updateDownload(downloaded, status);
    }


    private void check() {
        downloadListener.updateDownload(downloaded, status);
        for (int i = 0; i < size; i++) {
            if (properties.isForum())
                if (!pageList.get(i).isCompleted())
                    forum(i);
                else if (!chapList.get(i).isCompleted())
                    web(i);

        }
        status = COMPLETED;
    }

    @Override
    public synchronized void doChanger() {
        if (status != CHECKING) {
            downloaded++;
            next++;
        }
        if (status == DOWNLOADING) {
            update();
            if (next < size) {
                if (properties.isForum())
                    forum(next);
                else
                    web(next);
            }
        }
    }
}

