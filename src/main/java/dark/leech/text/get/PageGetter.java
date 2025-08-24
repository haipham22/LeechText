package dark.leech.text.get;

import java.util.ArrayList;

import dark.leech.text.models.Post;

/** Created by Dark on 1/21/2017. */
public interface PageGetter {
    ArrayList<Post> getter(String url);
}
