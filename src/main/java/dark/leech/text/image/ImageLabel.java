package dark.leech.text.image;

import dark.leech.text.action.Log;
import dark.leech.text.ui.CircleWait;
import dark.leech.text.util.FileUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
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

class ImageLoader extends SwingWorker<InputStream, Void> {
    private String urlImage;
    private String pathImage;
    private InputStream imageStream;
    private JLabel label;

    public ImageLoader path(String pathImage) {
        this.pathImage = pathImage;
        return this;
    }

    public ImageLoader url(String urlImage) {
        this.urlImage = urlImage;
        return this;
    }

    public ImageLoader input(InputStream imageStream) {
        this.imageStream = imageStream;
        return this;
    }

    public ImageLoader loadTo(JLabel label) {
        this.label = label;
        return this;
    }

    @Override
    protected InputStream doInBackground() throws Exception {
        if (urlImage != null)
            if (pathImage != null) {
                CircleWait circleWait = new CircleWait(label.getPreferredSize());
                JLayer<JPanel> layer = circleWait.getJlayer();
                label.add(layer);
                layer.setBounds(0, 0, label.getWidth(), label.getHeight());
                circleWait.startWait();
                FileUtils.url2file(urlImage, pathImage);
                circleWait.stopWait();
            }

        if (pathImage != null) {
            if (new File(pathImage).exists())
                if (new File(pathImage).isFile())
                    imageStream = new FileInputStream(pathImage);
        }
        if (imageStream == null)
            imageStream = ImageLoader.class.getResourceAsStream("/dark/leech/res/img/nocover.png");
        return imageStream;
    }

    @Override
    protected void done() {

        try {
            setImage(get());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setImage(InputStream in) {
        try {
            BufferedImage image = ImageIO.read(in);
            int x = label.getSize().width;
            int y = label.getSize().height;
            int ix = image.getWidth();
            int iy = image.getHeight();
            int dx = 0;
            int dy = 0;
            if (x / y > ix / iy) {
                dy = y;
                dx = dy * ix / iy;
            } else {
                dx = x;
                dy = dx * iy / ix;
            }
            ImageIcon icon = new ImageIcon(image.getScaledInstance(dx, dy,
                    BufferedImage.SCALE_SMOOTH));
            label.setIcon(icon);
        } catch (Exception ex) {
            Log.add(ex);
        }
    }
}
