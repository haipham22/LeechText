package dark.leech.text.ui.repository;

import static dark.leech.text.util.TextUtils.getClipboard;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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

        var js = Http.request(textUrl.getText()).string();
        var listType = TypeToken.getParameterized(List.class, RepositoryEntity.class).getType();

        List<RepositoryEntity> list = gson.fromJson(js, listType);

        if (CollectionUtils.isEmpty(list)) {
            textUrl.addError("Không tìm thấy repository!");
            return;
        }

        repositoryList.addAll(list);

        close();
    }
}
