package ee.webmedia.alfresco.app;

import org.springframework.beans.factory.BeanFactory;

public abstract class AppConstants {
    public static final String CHARSET = "UTF-8";
    private static BeanFactory beanFactory;

    public static BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public static void setBeanFactory(BeanFactory beanFactory) {
        if (AppConstants.beanFactory != null) {
            throw new IllegalStateException("BeanFactory is already set to " + AppConstants.beanFactory + "\n - this method shouldn't be called multiple times!");
        }
        AppConstants.beanFactory = beanFactory;
    }

}
