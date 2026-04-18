package dark.leech.text.plugin.js.loader;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.*;

import dark.leech.text.action.Log;
import dark.leech.text.enities.ChapterEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.js.api.JSList;

/**
 * JavaScript chapter list loader for vBook plugins using Rhino. Executes the 'toc' script and
 * parses result into ChapterEntity list.
 */
public class ListLoader {

    private final PluginEntity plugin;

    private ListLoader(PluginEntity plugin) {
        this.plugin = plugin;
    }

    public static ListLoader with(PluginEntity plugin) {
        return new ListLoader(plugin);
    }

    public List<ChapterEntity> load(String url) {
        Log.add("[ListLoader] Loading chapters from: " + url);

        if (plugin == null || plugin.getTocGetter() == null) {
            Log.add(
                    "[ListLoader] Plugin check failed: plugin="
                            + plugin
                            + ", tocGetter="
                            + (plugin != null ? plugin.getTocGetter() : "null"));
            return null;
        }

        Context ctx = null;
        try {
            // Enter Rhino context
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1); // Interpretation mode for security
            ctx.setLanguageVersion(200); // ES6 support for vBook plugins
            Scriptable scope = ctx.initStandardObjects();

            // Extract base URL from plugin source
            String baseUrl = plugin.getSource();
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = VBookApiSetup.extractBaseUrl(url);
            }
            Log.add("[ListLoader] Base URL: " + baseUrl);

            // Setup vBook API bindings
            VBookApiSetup.setupVBookApi(ctx, scope, baseUrl, url);

            // Get the toc script
            String tocScript = plugin.getTocGetter();
            Log.add("[ListLoader] Executing toc script...");

            // Execute the toc script
            ctx.evaluateString(scope, tocScript, "tocGetter", 1, null);

            // Call the execute function
            Object functionObj = scope.get("execute", scope);
            if (!(functionObj instanceof Function)) {
                Log.add("[ListLoader] ERROR: execute function not found or not executable");
                return null;
            }

            Log.add("[ListLoader] Calling execute() with URL: " + url);
            Object result = ((Function) functionObj).call(ctx, scope, scope, new Object[] {url});

            if (result == null || result == org.mozilla.javascript.Undefined.instance) {
                Log.add(
                        "[ListLoader] ERROR: execute() returned null - response failed or no"
                                + " chapters found");
                return null;
            }

            Log.add("[ListLoader] Result type: " + result.getClass().getName());
            List<ChapterEntity> chapters = extractChapterList(result);
            Log.add("[ListLoader] Extracted " + chapters.size() + " chapters");

            return chapters;

        } catch (Exception e) {
            Log.add("JavaScript execution error in list loader: " + e.getMessage());
            return null;
        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<ChapterEntity> extractChapterList(Object result) {
        List<ChapterEntity> chapterList = new ArrayList<>();

        try {
            Log.add("[ListLoader] extractChapterList: Starting extraction...");

            // Handle NativeArray (JavaScript array)
            if (result instanceof NativeArray array) {
                int size = (int) array.getLength();
                Log.add("[ListLoader] Result is NativeArray, size: " + size);

                for (int i = 0; i < size; i++) {
                    Object chapterValue = array.get(i, array);
                    ChapterEntity entity = extractChapter(chapterValue, i);
                    if (entity != null) {
                        chapterList.add(entity);
                    }
                }
            }
            // Handle NativeObject (JavaScript object with custom iterator)
            else if (result instanceof NativeObject obj) {

                // Check for length property (array-like)
                Object lengthProp = obj.get("length", obj);
                if (lengthProp instanceof Number) {
                    int size = ((Number) lengthProp).intValue();
                    Log.add("[ListLoader] Result has 'length' property: " + size);

                    for (int i = 0; i < size; i++) {
                        Object chapterValue = obj.get(i, obj);
                        ChapterEntity entity = extractChapter(chapterValue, i);
                        if (entity != null) {
                            chapterList.add(entity);
                        }
                    }
                } else {
                    Log.add(
                            "[ListLoader] Result is NativeObject without numeric length - checking"
                                    + " individual properties");
                    // Try to iterate over object properties
                    Object[] ids = obj.getIds();
                    for (Object id : ids) {
                        if (id instanceof Number) {
                            int index = ((Number) id).intValue();
                            Object chapterValue = obj.get(index, obj);
                            ChapterEntity entity = extractChapter(chapterValue, index);
                            if (entity != null) {
                                chapterList.add(entity);
                            }
                        }
                    }
                }
            }
            // Handle JSList wrapper
            else if (result instanceof JSList jsList) {
                int size = jsList.size();
                Log.add("[ListLoader] Result is JSList, size: " + size);

                for (int i = 0; i < size; i++) {
                    Object chapterValue = jsList.get(i);
                    ChapterEntity entity = extractChapter(chapterValue, i);
                    if (entity != null) {
                        chapterList.add(entity);
                    }
                }
            }
            // Handle List directly
            else if (result instanceof List<?> list) {
                Log.add("[ListLoader] Result is List, size: " + list.size());

                int i = 0;
                for (Object item : list) {
                    ChapterEntity entity = extractChapter(item, i++);
                    if (entity != null) {
                        chapterList.add(entity);
                    }
                }
            } else {
                Log.add("[ListLoader] Unknown result type: " + result.getClass().getName());
            }

            Log.add("[ListLoader] Extraction complete: " + chapterList.size() + " chapters found");
        } catch (Exception e) {
            Log.add("Error parsing chapter list: " + e.getMessage());
        }

        return chapterList;
    }

    private ChapterEntity extractChapter(Object chapterValue, int index) {
        if (chapterValue == null || chapterValue == org.mozilla.javascript.Undefined.instance) {
            return null;
        }

        ChapterEntity entity = new ChapterEntity();
        entity.setId(index);

        if (chapterValue instanceof NativeObject obj) {

            Object name = obj.get("name", obj);
            if (name != null && name != org.mozilla.javascript.Undefined.instance) {
                entity.setName(name.toString());
            }

            Object url = obj.get("url", obj);
            if (url != null && url != org.mozilla.javascript.Undefined.instance) {
                entity.setUrl(url.toString());
            }
        } else {
            // String format - just the name
            entity.setName(chapterValue.toString());
        }

        return entity;
    }
}
