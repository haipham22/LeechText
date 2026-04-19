package dark.leech.text.plugin.js.loader;

import org.mozilla.javascript.NativeObject;

import dark.leech.text.enities.BookEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.util.TextUtils;

/**
 * JavaScript detail loader for vBook plugins using Rhino. Sets up vBook API environment (BASE_URL,
 * load, fetch, Response, Html).
 */
public class DetailLoader extends AbstractLoader<BookEntity> {

    private DetailLoader(PluginEntity plugin) {
        super(plugin);
    }

    public static DetailLoader with(PluginEntity plugin) {
        return new DetailLoader(plugin);
    }

    @Override
    protected String getScript() {
        return TextUtils.isEmpty(plugin.getDetailGetter()) ? null : plugin.getDetailGetter();
    }

    @Override
    protected LoaderType getLoaderType() {
        return LoaderType.DETAIL;
    }

    @Override
    protected BookEntity processResult(Object result, String url) {
        // Defensive: ensure URL is valid before calling execute
        String baseUrl = plugin.getSource();
        String safeUrl =
                (url == null || url.isEmpty() || url.contains("NOT_FOUND")) ? baseUrl : url;
        return extractBookEntity(result, safeUrl);
    }

    private BookEntity extractBookEntity(Object result, String fallbackUrl) {
        BookEntity entity = new BookEntity();

        if (result instanceof NativeObject obj) {

            // Use JSResponse to safely extract string values from NativeObject
            Object name = obj.get("name", obj);
            String nameStr = JSResponse.getString(name);
            if (nameStr != null) {
                entity.setName(nameStr);
            }

            Object url = obj.get("url", obj);
            String urlStr = JSResponse.getString(url);
            String resultUrl = fallbackUrl;
            if (urlStr != null && !urlStr.isEmpty()) {
                resultUrl = urlStr;
            }
            entity.setUrl(resultUrl);

            Object detail = obj.get("detail", obj);
            String detailStr = JSResponse.getString(detail);
            if (detailStr != null) {
                entity.setDetail(detailStr.trim().replaceAll("\n+", "\n"));
            }

            Object description = obj.get("description", obj);
            String descStr = JSResponse.getString(description);
            if (descStr != null) {
                entity.setIntroduce(descStr);
            }

            Object cover = obj.get("cover", obj);
            String coverStr = JSResponse.getString(cover);
            if (coverStr != null) {
                entity.setCover(coverStr);
            }

            Object author = obj.get("author", obj);
            String authorStr = JSResponse.getString(author);
            if (authorStr != null) {
                entity.setAuthor(authorStr);
            }
        } else if (result != null) {
            // Handle plain string result
            String detailStr = JSResponse.getString(result);
            if (detailStr != null) {
                entity.setDetail(detailStr);
            }
        }

        entity.setWebSource(plugin.getName());
        return entity;
    }
}
