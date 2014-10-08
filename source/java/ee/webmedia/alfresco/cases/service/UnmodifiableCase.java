package ee.webmedia.alfresco.cases.service;

import static ee.webmedia.alfresco.utils.RepoUtil.getProp;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;

public class UnmodifiableCase implements Serializable, Comparable<UnmodifiableCase> {

    private static final long serialVersionUID = 1L;
    private final String title;
    private final String status;
    private final int containingDocsCount;
    private final String caseLabel;
    private final boolean isClosed;
    private final NodeRef nodeRef;

    public UnmodifiableCase(Node node) {
        title = getProp(CaseModel.Props.TITLE, node);
        status = getProp(CaseModel.Props.STATUS, node);
        Integer docCount = getProp(CaseModel.Props.CONTAINING_DOCS_COUNT, node);
        containingDocsCount = docCount != null ? docCount : 0;
        caseLabel = StringUtils.trim(title);
        isClosed = DocListUnitStatus.CLOSED.getValueName().equals(status);
        nodeRef = node.getNodeRef();
    }

    public String getTitle() {
        return title;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public String getStatus() {
        return status;
    }

    public String getCaseLabel() {
        return caseLabel;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public long getContainingDocsCount() {
        return containingDocsCount;
    }

    @Override
    public int compareTo(UnmodifiableCase other) {
        return AppConstants.getNewCollatorInstance().compare(title, other.getTitle());
    }

}
