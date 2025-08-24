package dark.leech.text.lua.loader;

import dark.leech.text.enities.ChapterEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.lua.api.Lua;
import dark.leech.text.lua.api.LuaScriptEngine;
import dark.leech.text.util.TextUtils;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;

public class TocLoader {
    private PluginEntity plugin;
    private Listener callback;

    public static TocLoader with(PluginEntity plugin) {
        return new TocLoader(plugin);
    }

    private TocLoader(PluginEntity plugin) {
        this.plugin = plugin;
    }

    public List<ChapterEntity> load(String url) {
        List<ChapterEntity> chapterList = new ArrayList<>();
        List<String> pageList = loadPage(url);
        int index = 0;
        if (pageList == null) return new ArrayList<>();
        int currentPage = 0;
        for (String pageUrl : pageList) {
            ++currentPage;
            List<ChapterEntity> chapterLoader = loadChapterList(pageUrl);
            if (chapterLoader == null || chapterLoader.size() == 0) return new ArrayList<>();
            List<ChapterEntity> pageChapterList = new ArrayList<>();

            for (ChapterEntity chapter : chapterLoader) {
                ChapterEntity entity = new ChapterEntity();
                entity.setName(chapter.getName());
                entity.setUrl(chapter.getUrl());
                entity.setId(index);
                pageChapterList.add(entity);
                index++;
            }
            // translate(pageChapterList);
            if (callback != null)
                callback.onUpdate(pageChapterList, (float) currentPage / pageList.size());
            chapterList.addAll(pageChapterList);
        }
        return chapterList;
    }

    private List<String> loadPage(String url) {
        if (!TextUtils.isEmpty(plugin.getPageGetter())) {
            Globals globals = LuaScriptEngine.getInstance().getGlobals();
            LuaValue chuck;
            try {
                chuck = globals.load(plugin.getPageGetter());
            } catch (LuaError error) {
                return null;
            }
            LuaValue result = chuck.call(LuaValue.valueOf(url));
            if (result instanceof LuaTable) {
                final List<String> urlList = new ArrayList<>();
                Lua.forEach((LuaTable) result, new Lua.TableAction() {
                    @Override
                    public void action(String key, LuaValue value) {
                        urlList.add(value.tojstring());
                    }
                });
                return urlList;
            }
        } else {
            List<String> urlList = new ArrayList<>();
            urlList.add(url);
            return urlList;
        }
        return null;
    }

    private List<ChapterEntity> loadChapterList(String url) {
        for (int i = 0; i < 3; i++) {
            if (plugin.getChapGetter() != null) {
                Globals globals = LuaScriptEngine.getInstance().getGlobals();
                final List<ChapterEntity> list = new ArrayList<>();
                LuaValue chuck;
                try {
                    chuck = globals.load(plugin.getTocGetter());
                } catch (LuaError error) {
                    return null;
                }
                LuaValue result = chuck.call(LuaValue.valueOf(url));
                if (result instanceof LuaTable) {
                    Lua.forEach((LuaTable) result, new Lua.TableAction() {
                        @Override
                        public void action(String key, LuaValue value) {
                            ChapterEntity chapter = new ChapterEntity();
                            chapter.setName(value.get("name").tojstring());
                            chapter.setUrl(value.get("url").tojstring());
                            list.add(chapter);
                        }
                    });

                }

                if (list.size() > 0)
                    return list;
            }
        }
        return null;
    }

    public TocLoader listener(Listener callback) {
        this.callback = callback;
        return this;
    }

    public interface Listener {
        void onUpdate(List<ChapterEntity> chapterList, float percent);
    }


}
