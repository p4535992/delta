package ee.webmedia.alfresco.notification.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> toEmails;
    private List<String> toNames;
    private String templateName;
    private String senderEmail;
    private String subject;
    private boolean attachFiles;
    private boolean failOnError;
    
    public void clearRecipients() {
        setToEmails(null);
        setToNames(null);
    }

    public void addRecipient(String name, String email) {
        if (toNames == null)
            toNames = new ArrayList<String>();
        if (toEmails == null)
            toEmails = new ArrayList<String>();
        toNames.add(name);
        toEmails.add(email);
    }

    public List<String> getToEmails() {
        return toEmails;
    }

    public void setToEmails(List<String> toEmails) {
        this.toEmails = toEmails;
    }

    public List<String> getToNames() {
        return toNames;
    }

    public void setToNames(List<String> toNames) {
        this.toNames = toNames;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isAttachFiles() {
        return attachFiles;
    }

    public void setAttachFiles(boolean attachFiles) {
        this.attachFiles = attachFiles;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @Override
    public String toString() {
        return "Notification [attachFiles=" + attachFiles + ", senderEmail=" + senderEmail + ", subject=" + subject + ", templateName=" + templateName
                + ", toEmails=" + toEmails + ", toNames=" + toNames + "]";
    }
}
