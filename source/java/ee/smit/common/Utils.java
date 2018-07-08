package ee.smit.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class Utils {
    private static final Log logger = LogFactory.getLog(Utils.class);

    private Utils(){}

    @SuppressWarnings("unchecked")
    public static <T> T castToAnything(Object obj) {
        return (T) obj;
    }


}
