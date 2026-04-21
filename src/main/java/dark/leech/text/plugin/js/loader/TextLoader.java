package dark.leech.text.plugin.js.loader;

import org.mozilla.javascript.NativeObject;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;

/**
 * JavaScript chapter content loader for vBook plugins using Rhino. Executes chapGetter script and
 * returns chapter text as String.
 */
public class TextLoader extends AbstractLoader<String> {

    private TextLoader(PluginEntity plugin) {
        super(plugin);
    }

    public static TextLoader with(PluginEntity plugin) {
        return new TextLoader(plugin);
    }

    @Override
    protected String getScript() {
        return plugin.getChapGetter();
    }

    @Override
    protected LoaderType getLoaderType() {
        return LoaderType.TEXT;
    }

    @Override
    protected String processResult(Object result, String url) {
        // Check for Response wrapper (vBooks compatibility)
        if (!Response.isSuccess(result)) {
            String errorMsg = Response.getErrorMessage(result);
            Log.add("[TextLoader] Error response: " + errorMsg);
            return "";
        }

        Object data = Response.getData(result);

        // Handle different return types:
        // NativeObject with properties
        if (data instanceof NativeObject) {
            NativeObject obj = (NativeObject) data;

            Object body = obj.get("body", obj);
            String bodyStr = JSResponse.getString(body);
            if (bodyStr != null) {
                return bodyStr;
            }

            Object content = obj.get("content", obj);
            String contentStr = JSResponse.getString(content);
            if (contentStr != null) {
                return contentStr;
            }

            Object text = obj.get("text", obj);
            String textStr = JSResponse.getString(text);
            if (textStr != null) {
                return textStr;
            }
        }

        // Direct string result
        return JSResponse.getString(data);
    }
}
