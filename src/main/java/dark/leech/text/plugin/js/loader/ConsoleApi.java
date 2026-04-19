package dark.leech.text.plugin.js.loader;

import dark.leech.text.action.Log;

/**
 * Console API for JavaScript plugin scripts. Matches vBooks JSLog behavior - provides log() method
 * for plugin debugging.
 */
public class ConsoleApi {

    /**
     * Log a message to the application log. Called from JavaScript as console.log() or
     * Console.log()
     *
     * @param msg The message to log
     */
    public void log(Object msg) {
        if (msg != null) {
            String msgStr = JSResponse.getString(msg);
            if (msgStr != null) {
                Log.add(msgStr);
            }
        }
    }
}
