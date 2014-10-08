<<<<<<< HEAD
package ee.webmedia.alfresco.template.exception;

/**
 * @author Kaarel Jõgeva
 */
public class ExistingFileFromTemplateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String templateName;

    public ExistingFileFromTemplateException(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

}
=======
package ee.webmedia.alfresco.template.exception;

public class ExistingFileFromTemplateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String templateName;

    public ExistingFileFromTemplateException(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

}
>>>>>>> develop-5.1
