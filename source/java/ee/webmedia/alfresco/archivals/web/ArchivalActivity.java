package ee.webmedia.alfresco.archivals.web;

import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Riina Tens
 */
public class ArchivalActivity extends NodeBaseVO implements Comparable<ArchivalActivity> {

    private static final long serialVersionUID = 1L;
    private final NodeRef docRef;
    private final String documentTitle;
    private final List<File> files;

    public ArchivalActivity(WmNode node, String documentTitle, NodeRef docRef, List<File> files) {
        this.node = node;
        this.documentTitle = documentTitle;
        this.docRef = docRef;
        this.files = files;
    }

    public Date getCreated() {
        return getProp(ArchivalsModel.Props.CREATED);
    }

    public String getActivityType() {
        String activityTypeStr = getProp(ArchivalsModel.Props.ACTIVITY_TYPE);
        ActivityType activityType = StringUtils.isNotBlank(activityTypeStr) ? ActivityType.valueOf(activityTypeStr) : null;
        return activityType != null ? MessageUtil.getMessage(activityType) : "";
    }

    public String getCreatorName() {
        return getProp(ArchivalsModel.Props.CREATOR_NAME);
    }

    public String getStatus() {
        return getProp(ArchivalsModel.Props.STATUS);
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public NodeRef getDocumentNodeRef() {
        return docRef;
    }

    public boolean getHasDocument() {
        return docRef != null;
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    public int compareTo(ArchivalActivity otherArchivalActivity) {
        Date date1 = getCreated();
        Date date2 = otherArchivalActivity.getCreated();
        if (date1 == null && date2 == null) {
            return 0;
        }
        if (date1 == null) {
            return 1;
        }
        if (date2 == null) {
            return -1;
        }
        return date1.compareTo(date2);
    }

}
