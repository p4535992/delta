package ee.webmedia.alfresco.log;

import javax.servlet.http.HttpServletRequest;

import ee.webmedia.alfresco.log.model.LogEntry;

/**
 * Helper class that gathers and stores data about the source of incoming request.
 * 
 * @author Martti Tamm
 */
public class LogHelper {

    private static final ThreadLocal<String[]> USER_INFO_LOCAL = new ThreadLocal<String[]>();

    /**
     * Reads request source information and keeps it within current thread.
     * 
     * @param request The incoming request.
     */
    public static void gatherUserInfo(HttpServletRequest request) {
        String[] info = new String[2];
        info[0] = request.getRemoteHost();
        info[1] = request.getRemoteAddr();
        USER_INFO_LOCAL.set(info);
    }

    /**
     * Updates log entry with information stored in current thread.
     * 
     * @param logEntry The log entry to update.
     */
    public static void update(LogEntry logEntry) {
        String[] info = USER_INFO_LOCAL.get();
        if (info != null) {
            logEntry.setComputerIp(info[0]);
            logEntry.setComputerName(info[1]);
        }
    }

    public static void resetUserInfo() {
        USER_INFO_LOCAL.remove();
    }
}
