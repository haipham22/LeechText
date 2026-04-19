package dark.leech.text.plugin.js.loader;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import dark.leech.text.action.Log;
import dark.leech.text.enities.ChapterEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.js.api.JSList;

/**
 * JavaScript chapter list loader for vBook plugins using Rhino. Executes the 'toc' script and
 * parses result into ChapterEntity list.
 */
public class ListLoader extends AbstractLoader<List<ChapterEntity>> {

    private ListLoader(PluginEntity plugin) {
        super(plugin);
    }

    public static ListLoader with(PluginEntity plugin) {
        return new ListLoader(plugin);
    }

    @Override
    protected String getScript() {
        return plugin.getTocGetter();
    }

    @Override
    protected LoaderType getLoaderType() {
        return LoaderType.LIST;
    }

    @Override
    protected List<ChapterEntity> processResult(Object result, String url) {
        Log.add("[ListLoader] Loading chapters from: " + url);
        Log.add("[ListLoader] Result type: " + result.getClass().getName());
        List<ChapterEntity> chapters = extractChapterList(result);
        Log.add("[ListLoader] Extracted " + chapters.size() + " chapters");
        return chapters;
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
            String nameStr = JSResponse.getString(name);
            if (nameStr != null) {
                entity.setName(nameStr);
            }

            Object url = obj.get("url", obj);
            String urlStr = JSResponse.getString(url);
            if (urlStr != null) {
                entity.setUrl(urlStr);
            }
        } else {
            // String format - just the name
            String nameStr = JSResponse.getString(chapterValue);
            if (nameStr != null) {
                entity.setName(nameStr);
            }
        }

        return entity;
    }
}
