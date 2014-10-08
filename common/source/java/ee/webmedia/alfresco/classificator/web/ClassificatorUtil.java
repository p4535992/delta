<<<<<<< HEAD
package ee.webmedia.alfresco.classificator.web;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;

/**
 * @author Priit Pikk
 */
public class ClassificatorUtil {
    public static <T extends ClassificatorValue> ClassificatorOrderModifier<T> getClassificatorReorderHelper() {
        return new ClassificatorOrderModifier<T>();
    }
}
=======
package ee.webmedia.alfresco.classificator.web;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;

public class ClassificatorUtil {
    public static <T extends ClassificatorValue> ClassificatorOrderModifier<T> getClassificatorReorderHelper() {
        return new ClassificatorOrderModifier<T>();
    }
}
>>>>>>> develop-5.1
