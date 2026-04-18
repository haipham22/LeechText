package dark.leech.text.action;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

import lombok.Setter;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.get.ChapExecute;
import dark.leech.text.listeners.BlurListener;
import dark.leech.text.listeners.TableListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.SettingUtils;

public class Config {
    private final List<Chapter> chapList;
    @Setter private String path;
    @Setter private TableListener tableListener;
    private int index;
    private PluginEntity pluginGetter;
    @Setter private BlurListener blurListener;
    private int errorCount = 0;

    public Config(List<Chapter> chapList) {
        this.chapList = chapList;
    }

    public List<Chapter> checkError() {
        ArrayList<Chapter> imgErr = new ArrayList<Chapter>();
        for (Chapter chapter : chapList) {
            if (!chapter.isError()) {
                continue;
            }
            imgErr.add(chapter);
        }
        return imgErr;
    }

    public List<Chapter> checkImg() {
        ArrayList<Chapter> imgList = new ArrayList<Chapter>();
        for (Chapter chapter : chapList) {
            if (!chapter.isImageChapter()) {
                continue;
            }
            imgList.add(chapter);
        }
        return imgList;
    }

    public ArrayList<Chapter> checkName() {
        ArrayList<Chapter> nameList = new ArrayList<Chapter>();
        for (Chapter c : chapList)
            if (findMatchs(c.getChapName(), "^(Ch..ng|H.i) \\d+([\\+\\.-]\\d+|)") != 1)
                nameList.add(c);
            else if (findMatchs(c.getChapName(), "Ch..ng \\d+") > 1) nameList.add(c);
        return nameList;
    }

    private void splitPartName(Chapter chapter) throws Exception {
        if (chapter.getPartName().length() > 1) return;
        String regex = "((Quy.n |Q.|Q)\\d+\\s*[:-](.*?)*)\\s*(([Cc]h..ng|Hồi)\\s+\\d+)";
        if (findMatchs(chapter.getChapName(), "(Quy.n |Q\\.|Q)\\d+([\\+\\.-]\\d+|)") == 1) {
            String partName =
                    splitMatchs(chapter.getChapName(), regex, 1).replaceAll("\\s*[:-]\\s*$", "");
            String chapName = chapter.getChapName();
            chapName = chapName.replace(partName, "").replaceAll("^\\s*[:-]\\s*", "");
            partName = partName.replaceAll("Q\\.|Q(\\d+)", "Quyển $1");
            chapter.setChapName(chapName);
            chapter.setPartName(partName);
        }
    }

    public void autoFixName() {
        for (int i = 0; i < chapList.size(); i++) {
            try {
                Chapter chapter =
                        new Chapter(
                                chapList.get(i).getUrl(),
                                chapList.get(i).getId(),
                                chapList.get(i).getPartName(),
                                chapList.get(i).getChapName());
                splitPartName(chapter);
                chapter.setChapName(fixName(chapter.getChapName()));
                chapter.setPartName(fixName(chapter.getPartName()));
                tableListener.updateData(i, chapter);
            } catch (Exception e) {
            }
        }
    }

    private String fixName(String name) {
        if (name == null) return "";
        if (name.isEmpty()) return name;
        name =
                name.replaceAll("Chương \\d+\\s*[:-]\\s*(Chương \\d+.*?$)", "$1")
                        .replaceAll("^([hH]ồi|[đĐ]ệ) (\\d+)", "Chương $1")
                        .replaceAll("(\\d+) [Cc]h..ng", "Chương $1")
                        .replaceAll("\\s+", " ")
                        .replaceAll("Chương (\\d+)\\s*[-\\+:]\\s*(\\d+)", "Chương $1+$2")
                        .replaceAll("(Chương \\d+)\\s*[;:-]+\\s*", "$1: ")
                        .replaceAll("(Chương \\d+\\+\\d+)\\s*[;:-]+\\s*", "$1: ");
        return name;
    }

    private String upperFirst(String name) {
        name = name.toLowerCase();
        char[] ch = name.toCharArray();
        ch[0] = Character.toUpperCase(ch[0]);
        for (int i = 1; i < ch.length; i++) {
            if (Character.toLowerCase(ch[i - 1]) == Character.toUpperCase(ch[i - 1]))
                ch[i] = Character.toUpperCase(ch[i]);
        }
        return new String(ch);
    }

    public void Optimize() {
        for (int i = 0; i < chapList.size(); i++) {
            try {
                Chapter chapter =
                        new Chapter(
                                chapList.get(i).getUrl(),
                                chapList.get(i).getId(),
                                chapList.get(i).getPartName(),
                                chapList.get(i).getChapName());
                chapter.setChapName(Optimize(chapter.getChapName()));
                chapter.setPartName(Optimize(chapter.getPartName()));
                tableListener.updateData(i, chapter);
            } catch (Exception e) {
            }
        }
    }

    private String Optimize(String name) {
        if (name.isEmpty()) return name;
        name = upperFirst(name);
        name = fixName(name);
        return name;
    }

    public void downloadChap(Properties properties) {
        pluginGetter = PluginManager.getManager().get(properties.getUrl());
        errorCount = 0;
        index = 0;
        download(properties);
    }

    private synchronized void download(final Properties properties) {
        AppUtils.pause(SettingUtils.DELAY);
        final Chapter chapter = chapList.get(index);
        if (!chapter.isError()) {
            tableListener.updateData(index, chapList.get(index));
            index++;
            download(properties);
            return;
        }
        new ChapExecute()
                .plugin(pluginGetter)
                .listener(
                        () -> {
                            if (chapter.isError()) {
                                errorCount++;
                            } else {
                                chapter.setError(false);
                                tableListener.updateData(index, chapter);
                            }
                            index++;
                            if (index >= chapList.size()) {
                                History.getHistory().save(properties);
                                if (errorCount > 0) {
                                    final ConfirmDialog dialog = new ConfirmDialog();
                                    dialog.setConfirmListener(
                                            new ConfirmListener() {
                                                @Override
                                                public void confirm() {
                                                    downloadChap(properties);
                                                    dialog.close();
                                                }

                                                @Override
                                                public void cancel() {
                                                    dialog.close();
                                                }
                                            });
                                    dialog.open();
                                }
                            } else download(properties);
                        })
                .charset(properties.getCharset())
                .path(properties.getSavePath())
                .applyTo(chapter)
                .execute();
    }

    public void downloadImg() {
        for (int i = 0; i < chapList.size(); i++) {
            try {
                downloadImg(chapList.get(i));
                tableListener.updateData(i, chapList.get(i));
            } catch (Exception e) {
            }
        }
    }

    private void downloadImg(Chapter chapter) {
        String text = FileUtils.file2string(path + "/raw/" + chapter.getId() + ".txt");
        ArrayList<String> imgList = new ArrayList<String>();
        Pattern r = Pattern.compile("<img.*?src=\"(.*?)\"", Pattern.MULTILINE);
        Matcher m = r.matcher(text);
        while (m.find()) imgList.add(m.group(1));
        for (int i = 0; i < imgList.size(); i++) {
            String imgPath = imgList.get(i);
            if (!imgPath.startsWith("http")) continue;
            String img = imgPath.substring(imgPath.lastIndexOf(".")).toLowerCase(); // TÁch đuôi
            text =
                    text.replace(imgPath, "../Images/" + chapter.getId() + "_" + i + img)
                            .replace("\">", "\"/>");
            img = path + "/data/Images/" + chapter.getId() + "_" + i + img;
            FileUtils.url2file(imgList.get(i), img);
        }
        FileUtils.string2file(text, path + "/raw/" + chapter.getId() + ".txt");
    }

    private int findMatchs(String src, String regex) {
        if (src == null) return 0;
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(src);
        int match = 0;
        while (m.find()) {
            match++;
        }
        return match;
    }

    private String splitMatchs(String src, String regex, int group) {
        if (src == null) return "";
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(src);
        String math = "";
        if (m.find()) math = m.group(group);
        return math;
    }

    public void addTableListener(TableListener tableListener) {
        this.tableListener = tableListener;
    }

    public interface ConfirmListener {
        void confirm();

        void cancel();
    }

    class ConfirmDialog extends JMDialog {
        private BasicButton btConfirm;
        private BasicButton btCancel;
        @Setter private ConfirmListener confirmListener;

        public ConfirmDialog() {
            setSize(300, 150);
            runOnUiThread(this::onCreate);
        }

        @Override
        protected void onCreate() {
            super.onCreate();
            btConfirm = new BasicButton();
            btCancel = new BasicButton();
            JLabel label = new JLabel("Còn " + errorCount + " chương lỗi");
            JLabel label1 = new JLabel("Tải tiếp các chương lỗi???");

            btConfirm.setText("XÁC NHẬN");
            btCancel.setText("HỦY");

            container.add(label);
            label.setBounds(50, 10, 200, 30);
            container.add(label1);
            label1.setBounds(30, 50, 200, 30);
            container.add(btConfirm);
            btConfirm.setBounds(10, 100, 100, 35);
            container.add(btCancel);
            btCancel.setBounds(190, 100, 100, 35);

            btConfirm.addActionListener(
                    e -> {
                        if (confirmListener != null) confirmListener.confirm();
                        close();
                    });
            btCancel.addActionListener(
                    e -> {
                        if (confirmListener != null) confirmListener.cancel();
                        close();
                    });
        }
    }
}
