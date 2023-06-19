package dark.leech.text.image;

import javax.swing.*;
import java.io.InputStream;

/**
 * Created by Long on 1/6/2017.
 */
public class ImageLabel extends JLabel {
    private String urlImage;
    private String pathImage;
    private InputStream imageStream;


    public ImageLabel path(String pathImage) {
        this.pathImage = pathImage;
        return this;
    }

    public ImageLabel url(String urlImage) {
        this.urlImage = urlImage;
        return this;
    }

    public ImageLabel input(InputStream imageStream) {
        this.imageStream = imageStream;
        return this;
    }

    public void load() {
        new ImageLoader().url(urlImage)
                .path(pathImage)
                .input(imageStream)
                .loadTo(this)
                .execute();
    }
}
