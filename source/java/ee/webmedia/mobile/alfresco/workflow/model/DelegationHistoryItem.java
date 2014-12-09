package ee.webmedia.mobile.alfresco.workflow.model;

import static ee.webmedia.alfresco.workflow.web.DelegationHistoryGenerator.TMP_STYLE_CLASS;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.web.DelegationHistoryGenerator;

public class DelegationHistoryItem {

    private final String creatorName;
    private final String mainOwner;
    private final String coOwner;
    private final String resolution;
    private final Date dueDate;
    private final String styleClass;

    public DelegationHistoryItem(Map<String, Object> props) {
        creatorName = (String) props.get(WorkflowCommonModel.Props.CREATOR_NAME);
        mainOwner = (String) props.get(DelegationHistoryGenerator.TMP_MAIN_OWNER);
        coOwner = (String) props.get(DelegationHistoryGenerator.TMP_CO_OWNER);
        resolution = (String) props.get(WorkflowSpecificModel.Props.RESOLUTION);
        dueDate = (Date) props.get(WorkflowSpecificModel.Props.DUE_DATE);
        styleClass = StringUtils.trimToEmpty((String) props.get(TMP_STYLE_CLASS));
    }

    public String getCreatorName() {
        return creatorName;
    }

    public String getMainOwner() {
        return mainOwner;
    }

    public String getCoOwner() {
        return coOwner;
    }

    public String getResolution() {
        return resolution;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public String getStyleClass() {
        return styleClass;
    }

}