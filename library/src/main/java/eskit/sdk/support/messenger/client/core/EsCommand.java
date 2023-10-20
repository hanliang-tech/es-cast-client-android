package eskit.sdk.support.messenger.client.core;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 命令参数
 */
public class EsCommand {

    // 启动快应用指令
    public static final String ACTION_START_ES_APP = "start_es";
    public static final String ACTION_START_NT_APP = "start_app";
    public static final String K_ACTION_ES_CMD = "es_cmd";

    // 不显示加载页
    public static final int SPLASH_NONE = -1;
    // 显示默认加载页
    public static final int SPLASH_DEFAULT = 0;
    // 显示没有icon的加载页
    public static final int SPLASH_NO_ICON = 1;

    public static final int FLAG_CLEAR_TASK = 1;

    private JSONObject mJson;
    /**
     * 打印debug信息
     **/
    private boolean debug;
    private boolean custom;

    private EsCommand(String actionName) {
        mJson = new JSONObject();
        put("action", actionName);
    }

    private void put(String key, Object value) {
        try {
            mJson.putOpt(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动快应用
     **/
    public static EsCommand makeEsAppCommand(String pkg) {
        return new EsCommand(ACTION_START_ES_APP).setPkgName(pkg);
    }

    /**
     * 启动原生应用
     **/
    public static EsCommand makeNativeAppCommand(String pkg) {
        return new EsCommand(ACTION_START_NT_APP).setPkgName(pkg);
    }

    public static EsCommand makeCmdCloseCommand(String... packages) {
        EsCommand command = new EsCommand(K_ACTION_ES_CMD);
        try {
            JSONArray ja = new JSONArray();
            for (String p : packages) {
                ja.put(p);
            }
            CmdContent cmd = new CmdContent("es_close");
            cmd.putData("pkgs", ja);
            command.setEventData(cmd.args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return command;
    }

    public static EsCommand makeCmdKeyEventCommand(int keyCode) {
        EsCommand command = new EsCommand(K_ACTION_ES_CMD);
        CmdContent cmd = new CmdContent("es_remote_control");
        cmd.putData("keycode", keyCode);
        command.setEventData(cmd.args);
        return command;
    }

    public static EsCommand makeCustomCommand(String action) {
        return new EsCommand(action).setCustom(true);
    }

    /**
     * 设置应用包名
     **/
    public EsCommand setPkgName(String pkgName) {
        put("pkg", pkgName);
        return this;
    }

    /**
     * 设置指令参数
     **/
    public EsCommand setEventData(Object eventData) {
        assertEventData(eventData);
        if (eventData instanceof CmdArgs) {
            put("args", ((CmdArgs) eventData).getJsonObject());
        } else {
            put("args", eventData);
        }
        return this;
    }

    public EsCommand flagClearTask() {
        put("flags", FLAG_CLEAR_TASK);
        return this;
    }

    /**
     * 关闭启动页
     **/
    public EsCommand splashNone() {
        put("splash", SPLASH_NONE);
        return this;
    }

    /**
     * 启动页只显示Loading
     **/
    public EsCommand splashNoIcon() {
        put("splash", SPLASH_NO_ICON);
        return this;
    }

    public EsCommand setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    boolean isDebug() {
        return this.debug;
    }

    boolean isStartEsApp() {
        return ACTION_START_ES_APP.equals(mJson.optString("action"));
    }

    boolean isCustom() {
        return custom;
    }

    EsCommand setCustom(boolean custom) {
        this.custom = custom;
        return this;
    }

    public JSONObject getJsonObject() {
        return mJson;
    }

    private void assertEventData(Object eventData) {
        if (eventData == null) return;
        if (eventData instanceof CmdArgs) return;
        if (eventData instanceof Number) return;
        if (eventData instanceof String) return;
        if (eventData instanceof JSONObject) return;
        throw new RuntimeException("不支持的数据类型: " + eventData.getClass().getName());
    }

    public static class CmdArgs {
        private String url = "";
        private final JSONObject params = new JSONObject();

        public CmdArgs(String url) {
            this.url = url;
        }

        public CmdArgs put(String key, Object value) {
            try {
                params.put(key, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return this;
        }

        public JSONObject getJsonObject() {
            JSONObject jo = new JSONObject();
            try {
                jo.put("url", url);
                jo.put("params", params);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return jo;
        }
    }

    private static class CmdContent {
        JSONObject args = new JSONObject();
        JSONObject data = new JSONObject();

        public CmdContent(String intention) {
            put(args, "intention", intention);
            put(args, "data", data);
        }

        public void putData(String key, Object value) {
            put(data, key, value);
        }

        private void put(JSONObject jo, String key, Object value) {
            try {
                jo.putOpt(key, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
