package ee.webmedia.mobile.alfresco.common;

import java.beans.Introspector;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.util.ClassUtils;

/**
 * Generates bean names from class names.
 */
public class StandardBeanNameGenerator extends AnnotationBeanNameGenerator {
    private static final String IMPL_SUFFIX = "Impl";

    /**
     * {@inheritDoc}
     * 
     * @see org.springframework.context.annotation.AnnotationBeanNameGenerator#buildDefaultBeanName(org.springframework.beans.factory.config.BeanDefinition)
     */
    @Override
    protected String buildDefaultBeanName(BeanDefinition definition) {
        String name = definition.getBeanClassName();
        if (name.endsWith(IMPL_SUFFIX)) {
            name = name.substring(0, name.indexOf(IMPL_SUFFIX));
        }

        String shortClassName = ClassUtils.getShortName(name);
        return Introspector.decapitalize(shortClassName);
    }
}
