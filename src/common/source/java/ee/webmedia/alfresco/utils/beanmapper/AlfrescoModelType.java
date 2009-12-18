/**
 * 
 */
package ee.webmedia.alfresco.utils.beanmapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ats.uiboupin
 */
@Target(value=ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AlfrescoModelType {
    /**
     * namespaceURI of QName<br>
     * Must not be empty when annotating interface, but can be empty for classes( see comments on {@link BeanPropertyMapper}) 
     * @return URI that is used for mapping fields to Map&lt;org.alfresco.service.namespace.QName, Serializable&gt;
     */
    String uri();
}
