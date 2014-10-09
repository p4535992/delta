package ee.webmedia.alfresco.casefile.service;

import java.io.Serializable;
import java.text.Collator;
import java.util.Date;

import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

public class DocumentToCompoundWorkflow implements Comparable<DocumentToCompoundWorkflow>, Serializable {

    private static final long serialVersionUID = 1L;
    private final Document document;
    private final CompoundWorkflow compoundWorkflow;
    private String compoundWorkflowState;
    private final static Collator collator = AppConstants.getNewCollatorInstance();

    public DocumentToCompoundWorkflow(Document document, CompoundWorkflow compoundWorkflow) {
        Assert.notNull(compoundWorkflow);
        Assert.notNull(document);
        this.document = document;
        this.compoundWorkflow = compoundWorkflow;
    }

    public Document getDocument() {
        return document;
    }

    public Date getRegDateTime() {
        return document.getRegDateTime();
    }

    public String getRegNumber() {
        return document.getRegNumber();
    }

    public String getDocName() {
        return document.getDocName();
    }

    public String getDocumentTypeName() {
        return document.getDocumentTypeName();
    }

    public String getCompoundWorkflowTitle() {
        return compoundWorkflow.getTitle();
    }

    public String getCompoundWorkflowState() {
        if (compoundWorkflowState == null) {
            compoundWorkflowState = WorkflowUtil.getCompoundWorkflowState(compoundWorkflow);
        }
        return compoundWorkflowState;
    }

    @Override
    public int compareTo(DocumentToCompoundWorkflow o) {
        if (o == null) {
            return 1;
        }
        int stateComparision = collator.compare(getCompoundWorkflowState(), o.getCompoundWorkflowState());
        if (stateComparision != 0) {
            return stateComparision;
        }
        return o.getDocument().getCreated().compareTo(document.getCreated()); // descending
    }
}
