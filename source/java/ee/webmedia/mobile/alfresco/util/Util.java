package ee.webmedia.mobile.alfresco.util;

import java.util.Arrays;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import ee.webmedia.alfresco.app.AppConstants;

@Component
public class Util {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(Util.class);

    private Util() {
        // Disable instantiation from outside of this class
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> T[] toArray(T... items) {
        if (items == null || items.length < 1) {
            return (T[]) new Object[0];
        }

        return items;
    }

    public static String translate(MessageSource messages, String translationKey, Object... placeholderValues) {
        if (messages == null) {
            LOG.error("Provided messageSource is null! \n" + Arrays.toString(Thread.currentThread().getStackTrace()));
            return translationKey;
        }

        return messages.getMessage(translationKey, placeholderValues, AppConstants.getDefaultLocale());
    }

}
