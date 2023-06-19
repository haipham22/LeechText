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


class ImageLoader
        extends SwingWorker<InputStream, Void> {
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


    protected InputStream doInBackground() throws Exception {

        if (this.urlImage != null &&
                this.pathImage != null) {

            CircleWait circleWait = new CircleWait(this.label.getPreferredSize());

            JLayer<JPanel> layer = circleWait.getJlayer();

            this.label.add(layer);

            layer.setBounds(0, 0, this.label.getWidth(), this.label.getHeight());

            circleWait.startWait();

            FileUtils.url2file(this.urlImage, this.pathImage);

            circleWait.stopWait();
        }


        if (this.pathImage != null && (
                new File(this.pathImage)).exists() && (
                new File(this.pathImage)).isFile()) {

            this.imageStream = new FileInputStream(this.pathImage);
        }

        if (this.imageStream == null)
            this.imageStream = ImageLoader.class.getResourceAsStream("/dark/leech/res/img/nocover.png");

        return this.imageStream;
    }


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
            int x = (this.label.getSize()).width;
            int y = (this.label.getSize()).height;
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
            ImageIcon icon = new ImageIcon(image.getScaledInstance(dx, dy, 4));

            this.label.setIcon(icon);
        } catch (Exception ex) {
            Log.add(ex);
        }
    }
}



