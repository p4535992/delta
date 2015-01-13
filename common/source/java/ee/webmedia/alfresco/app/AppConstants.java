package ee.webmedia.alfresco.app;

import java.io.Serializable;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Locale;

import org.springframework.beans.factory.BeanFactory;

public abstract class AppConstants {
    public static final String CHARSET = "UTF-8";
    private static BeanFactory beanFactory;
    public static final Collator DEFAULT_COLLATOR;

    public static class SerializableDefaultCollatorDelegate extends Collator implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(String source, String target) {
            return DEFAULT_COLLATOR.compare(source, target);
        }

        @Override
        public CollationKey getCollationKey(String source) {
            return DEFAULT_COLLATOR.getCollationKey(source);
        }

        @Override
        public int hashCode() {
            return DEFAULT_COLLATOR.hashCode();
        }

    }

    static {
        Collator tmp_collator = Collator.getInstance(getDefaultLocale());
        tmp_collator.setStrength(Collator.SECONDARY);
        tmp_collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        DEFAULT_COLLATOR = tmp_collator;
    }

    public static BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public static Locale getDefaultLocale() {
        return new Locale("et", "EE", "");
    }

    public static void setBeanFactory(BeanFactory beanFactory) {
        if (AppConstants.beanFactory != null) {
            throw new IllegalStateException("BeanFactory is already set to " + AppConstants.beanFactory + "\n - this method shouldn't be called multiple times!");
        }
        AppConstants.beanFactory = beanFactory;
    }

}
