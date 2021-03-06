package ee.webmedia.alfresco.notification.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.webmedia.alfresco.common.web.WmNode;

public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> toEmails;
    private List<String> toNames;
    private String templateName;
    private String senderEmail;
    private String subject;
    private boolean attachFiles;
    private boolean failOnError;
    private boolean toPerson;
    private Map<String, String> additionalFormulas;

    public void clearRecipients() {
        setToEmails(null);
        setToNames(null);
    }

    public void addRecipient(String name, String email) {
        if (toNames == null) {
            toNames = new ArrayList<String>();
        }
        if (toEmails == null) {
            toEmails = new ArrayList<String>();
        }
        toNames.add(name);
        toEmails.add(email);
    }

    public void addAdditionalFomula(String formulaKey, String formulaValue) {
        getAdditionalFormulas().put(formulaKey, formulaValue);
    }

    public List<String> getToEmails() {
        if (toEmails == null) {
            toEmails = new ArrayList<String>();
        }
        return toEmails;
    }

    public void setToEmails(List<String> toEmails) {
        this.toEmails = toEmails;
    }

    public List<String> getToNames() {
        if (toNames == null) {
            toNames = new ArrayList<String>();
        }
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

    public Map<String, String> getAdditionalFormulas() {
        if (additionalFormulas == null) {
            additionalFormulas = new HashMap<String, String>();
        }
        return additionalFormulas;
    }

    public void setAdditionalFormulas(Map<String, String> additionalFormulas) {
        this.additionalFormulas = additionalFormulas;
    }

    public boolean isToPerson() {
        return toPerson;
    }

    public void setToPerson(boolean toPerson) {
        this.toPerson = toPerson;
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  toEmails=" + WmNode.toString(getToEmails()) + "\n  toNames=" + WmNode.toString(getToNames()) + "\n  templateName="
                + getTemplateName() + "\n  senderEmail=" + getSenderEmail() + "\n  subject=" + getSubject() + "\n  attachFiles=" + isAttachFiles()
                + "\n  failOnError=" + isFailOnError() + "\n]";
    }

}
