package ee.webmedia.alfresco.log;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.log.model.LogEntry;

/**
 * Helper class that gathers and stores data about the source of incoming request.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> develop-5.1
 */
public class LogHelper {

    private static final ThreadLocal<String[]> USER_INFO_LOCAL = new ThreadLocal<String[]>();
    private static boolean useClientIpFromXForwardedForHttpHeader = false;

    public static void setUseClientIpFromXForwardedForHttpHeader(boolean useClientIpFromXForwardedForHttpHeader) {
        LogHelper.useClientIpFromXForwardedForHttpHeader = useClientIpFromXForwardedForHttpHeader;
    }

    /**
     * Reads request source information and keeps it within current thread.
     * 
     * @param request The incoming request.
     */
    public static void gatherUserInfo(HttpServletRequest request) {
        String computerIp = null;
        String computerName = null;
        if (useClientIpFromXForwardedForHttpHeader) {
            computerIp = request.getHeader("X-Forwarded-For");
            // http://httpd.apache.org/docs/2.2/mod/mod_proxy.html#x-headers
            // X-Forwarded-For = The IP address of the client. It will contain more than one (comma-separated) value if the original request already contained one of these headers.
            // Even if Apache config contains HostnameLookups On, then this header still contains IP address, not hostname.
            // So this header may contain "192.168.40.44" or "foobar, 192.168.40.44" or ", foobar,, 192.168.40.44"
            if (StringUtils.contains(computerIp, ',')) {
                computerIp = computerIp.substring(computerIp.lastIndexOf(',') + 1).trim();
            }
        }
        if (StringUtils.isBlank(computerIp)) {
            computerIp = request.getRemoteAddr();
            computerName = request.getRemoteHost();
            if (StringUtils.equals(computerIp, computerName)) {
                computerName = null;
            }
        }
        setUserInfo(computerIp, computerName);
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
            // TODO if computerName isBlank, then perform name resolving here, but only if session doesn't contain previously resolved name
            logEntry.setComputerName(info[1]);
        }
    }

    public static void setUserInfo(String computerIp, String computerName) {
        String[] info = new String[2];
        info[0] = computerIp;
        info[1] = computerName;
        USER_INFO_LOCAL.set(info);
    }

    public static void resetUserInfo() {
        USER_INFO_LOCAL.remove();
    }
}
