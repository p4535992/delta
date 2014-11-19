<<<<<<< HEAD
package ee.webmedia.alfresco.importer.excel.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {
    char value() default '-';

    /** Excel column letter */
    int colNr() default Integer.MIN_VALUE;// default value of annotation can't be null, using Integer.MIN_VALUE to denote it
}
=======
package ee.webmedia.alfresco.importer.excel.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {
    char value() default '-';

    /** Excel column letter */
    int colNr() default Integer.MIN_VALUE;// default value of annotation can't be null, using Integer.MIN_VALUE to denote it
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
