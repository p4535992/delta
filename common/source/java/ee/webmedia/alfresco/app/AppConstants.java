package ee.webmedia.alfresco.app;

import java.io.Serializable;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Locale;

import org.springframework.beans.factory.BeanFactory;

public abstract class AppConstants {
    public static final String CHARSET = "UTF-8";
    public static final String DEVICE_DETECTION_OVERRIDE = "deviceDetectionOverride";
    public static final Locale DEFAULT_LOCALE = new Locale("et", "EE", "");
    private static BeanFactory beanFactory;

    public static class SerializableDefaultCollatorDelegate extends Collator implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final Collator collator = getNewCollatorInstance();

        @Override
        public int compare(String source, String target) {
            return collator.compare(source, target);
        }

        @Override
        public CollationKey getCollationKey(String source) {
            return collator.getCollationKey(source);
        }

        @Override
        public int hashCode() {
            return collator.hashCode();
        }

    }

    /**
     * Case-insensitive
     */
    public static Collator getNewCollatorInstance() {
        Collator collatorInstance = Collator.getInstance(getDefaultLocale());
        collatorInstance.setStrength(Collator.SECONDARY); // case-insensitive
        collatorInstance.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        return collatorInstance;
    }

    public static BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public static Locale getDefaultLocale() {
        return DEFAULT_LOCALE;
    }

    public static void setBeanFactory(BeanFactory beanFactory) {
        if (AppConstants.beanFactory != null) {
            throw new IllegalStateException("BeanFactory is already set to " + AppConstants.beanFactory + "\n - this method shouldn't be called multiple times!");
        }
        AppConstants.beanFactory = beanFactory;
    }

}
