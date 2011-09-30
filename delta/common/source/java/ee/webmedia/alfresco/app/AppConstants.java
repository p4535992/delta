package ee.webmedia.alfresco.app;

import java.text.Collator;
import java.util.Locale;

import org.springframework.beans.factory.BeanFactory;

public abstract class AppConstants {
    public static final String CHARSET = "UTF-8";
    private static BeanFactory beanFactory;
    public static final Collator DEFAULT_COLLATOR;

    static {
        Collator tmp_collator = Collator.getInstance(getDefaultLocale());
        tmp_collator.setStrength(Collator.SECONDARY);
        tmp_collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        DEFAULT_COLLATOR = tmp_collator;
    }

    public static BeanFactory getBeanFactory() {
        return beanFactory;
    }

    private static Locale getDefaultLocale() {
        return new Locale("et", "EE", "");
    }

    public static void setBeanFactory(BeanFactory beanFactory) {
        if (AppConstants.beanFactory != null) {
            throw new IllegalStateException("BeanFactory is already set to " + AppConstants.beanFactory + "\n - this method shouldn't be called multiple times!");
        }
        AppConstants.beanFactory = beanFactory;
    }

}
