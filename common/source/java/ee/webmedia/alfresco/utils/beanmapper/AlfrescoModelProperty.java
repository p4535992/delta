<<<<<<< HEAD
package ee.webmedia.alfresco.utils.beanmapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AlfrescoModelProperty {

    boolean isMappable() default true;

}
=======
package ee.webmedia.alfresco.utils.beanmapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AlfrescoModelProperty {

    boolean isMappable() default true;

}
>>>>>>> develop-5.1
