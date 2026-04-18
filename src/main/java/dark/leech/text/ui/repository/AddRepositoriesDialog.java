package dark.leech.text.ui.repository;

import static dark.leech.text.util.TextUtils.getClipboard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;

import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMTextField;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.Http;
import dark.leech.text.util.TextUtils;

public class AddRepositoriesDialog extends JMDialog {

    private final Gson gson = new Gson();

    private JMTextField textUrl;
    private BasicButton ok;
    private BasicButton cancel;

    @Getter private List<RepositoryEntity> repositoryList;

    public AddRepositoriesDialog() {
        this(new ArrayList<>());
    }

    public AddRepositoriesDialog(List<RepositoryEntity> repositoryList) {
        this.repositoryList = repositoryList;
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        JLabel labelSrc = new JLabel();
        textUrl = new JMTextField();
        textUrl.setFont(FontUtils.TEXT_NORMAL);
        textUrl.setText(getClipboard());
        container.add(textUrl);
        textUrl.setBounds(10, 35, 280, 37);

        ok = new BasicButton();
        ok.setText("XONG");
        ok.addActionListener(e -> check());
        container.add(ok);
        ok.setBounds(105, 120, 90, 30);

        labelSrc.setText("Nhập URL");
        labelSrc.setFont(FontUtils.TEXT_NORMAL);
        container.add(labelSrc);
        labelSrc.setBounds(10, 0, 280, 25);

        cancel = new BasicButton();
        cancel.setText("HỦY");
        cancel.addActionListener(e -> close());
        container.add(cancel);
        cancel.setBounds(200, 120, 90, 30);

        this.setSize(300, 170);
    }

    private void check() {
        if (StringUtils.isBlank(textUrl.getText())) {
            textUrl.addError("Nội dung không được để trống!");
            return;
        }

        String url = convertToRawUrl(textUrl.getText().trim());

        // Try to parse as array of repositories (for bulk import)
        String js;
        try {
            js = Http.request(url).string();
        } catch (Exception e) {
            textUrl.addError("URL không thể truy cập!");
            return;
        }

        var listType = TypeToken.getParameterized(List.class, RepositoryEntity.class).getType();
        List<RepositoryEntity> list = new ArrayList<>();

        try {
            List<RepositoryEntity> parsed = gson.fromJson(js, listType);
            if (parsed != null && !parsed.isEmpty()) {
                list = parsed;
            } else {
                // Empty array - create single repository entity
                RepositoryEntity repo = new RepositoryEntity();
                repo.setLink(url);
                repo.setEnabled(true);
                list.add(repo);
            }
        } catch (Exception e) {
            // Not an array - create single repository entity from URL
            RepositoryEntity repo = new RepositoryEntity();
            repo.setLink(url);
            repo.setEnabled(true);
            list.add(repo);
        }

        if (CollectionUtils.isNotEmpty(list)) {
            list =
                    list.stream()
                            .peek(
                                    repo -> {
                                        repo.setUuid(
                                                TextUtils.getUUID(
                                                        repo.getLink(), repo.getAuthor()));
                                        repo.setEnabled(true);
                                    })
                            .collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(list)) {
            textUrl.addError("Không tìm thấy repository!");
            return;
        }

        repositoryList.addAll(list);

        close();
    }

    private String convertToRawUrl(String url) {
        if (url.contains("github.com") && url.contains("/blob/")) {
            return url.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/");
        }
        return url;
    }
}
