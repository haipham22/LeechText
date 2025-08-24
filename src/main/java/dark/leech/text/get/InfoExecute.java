package dark.leech.text.get;

import dark.leech.text.enities.BookEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.lua.loader.DetailLoader;
import dark.leech.text.models.Properties;
import dark.leech.text.util.SyntaxUtils;

import javax.swing.*;

/**
 * Created by Dark on 1/18/2017.
 */
public class InfoExecute extends SwingWorker {
    private DetailLoader loader;
    private ChangeListener changeListener;
    private Properties properties;
    private boolean success;

    public InfoExecute plugin(PluginEntity pluginEntity) {
        loader = DetailLoader.with(pluginEntity);
        return this;
    }


    public InfoExecute listener(ChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    public InfoExecute applyTo(Properties properties) {
        this.properties = properties;
        return this;
    }


    @Override
    protected Void doInBackground() {
        try {
            BookEntity book = loader.load(properties.getUrl());
            if (book != null) {
                properties.setAuthor(book.getAuthor());
                properties.setGioiThieu(book.getIntroduce());
                properties.setUrl(book.getUrl());
                properties.setCover(book.getCover());
                properties.setName(book.getName());
            }
            success = true;
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    protected void done() {
        if (success)
            if (properties.getGioiThieu() != null)
                properties.setGioiThieu("<p>" + SyntaxUtils.Optimize(properties.getGioiThieu()).replace("\n", "</p>\n<p>") + "</p>");
        changeListener.doChanger();
    }
}
