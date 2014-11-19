package ee.webmedia.alfresco.template.model;

import java.io.Serializable;

/**
 * Data structure for processed e-mail template results
 */
public class ProcessedEmailTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    private String subject;
    private String content;

    public ProcessedEmailTemplate(String subject, String content) {
        this.subject = subject;
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
