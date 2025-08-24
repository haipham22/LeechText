package dark.leech.text.get;

import dark.leech.text.models.Post;

import java.util.ArrayList;

/**
 * Created by Dark on 1/21/2017.
 */
public interface PageGetter {
    public ArrayList<Post> getter(String url);

}
