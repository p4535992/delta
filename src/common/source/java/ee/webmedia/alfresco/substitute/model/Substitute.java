package ee.webmedia.alfresco.substitute.model;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.time.DateUtils;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Romet Aidla
 */
@AlfrescoModelType(uri = SubstituteModel.URI)
public class Substitute implements Serializable {
    private static final long serialVersionUID = 0L;

    private String substituteName;
    private String substituteId;
    private Date substitutionStartDate;
    private Date substitutionEndDate;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef nodeRef;
    @AlfrescoModelProperty(isMappable = false)
    private String replacedPersonUserName;

    public Substitute() {
    }

    public Substitute(Substitute sub) {
        this.substituteName = sub.substituteName;
        this.substituteId = sub.substituteId;
        this.substitutionStartDate = sub.substitutionStartDate;
        this.substitutionEndDate = sub.substitutionEndDate;
        this.nodeRef = sub.nodeRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Substitute that = (Substitute) o;

        if (nodeRef != null ? !nodeRef.equals(that.nodeRef) : that.nodeRef != null) return false;
        if (replacedPersonUserName != null ? !replacedPersonUserName.equals(that.replacedPersonUserName) : that.replacedPersonUserName != null) return false;
        if (substituteId != null ? !substituteId.equals(that.substituteId) : that.substituteId != null) return false;
        if (substituteName != null ? !substituteName.equals(that.substituteName) : that.substituteName != null) return false;
        if (substitutionEndDate != null ? !substitutionEndDate.equals(that.substitutionEndDate) : that.substitutionEndDate != null) return false;
        if (substitutionStartDate != null ? !substitutionStartDate.equals(that.substitutionStartDate) : that.substitutionStartDate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = substituteName != null ? substituteName.hashCode() : 0;
        result = 31 * result + (substituteId != null ? substituteId.hashCode() : 0);
        result = 31 * result + (substitutionStartDate != null ? substitutionStartDate.hashCode() : 0);
        result = 31 * result + (substitutionEndDate != null ? substitutionEndDate.hashCode() : 0);
        result = 31 * result + (nodeRef != null ? nodeRef.hashCode() : 0);
        result = 31 * result + (replacedPersonUserName != null ? replacedPersonUserName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Substitute{" +
                "substituteName='" + substituteName + '\'' +
                ", substituteId='" + substituteId + '\'' +
                ", substitutionStartDate=" + substitutionStartDate +
                ", substitutionEndDate=" + substitutionEndDate +
                ", nodeRef=" + nodeRef +
                ", replacedPersonUserName='" + replacedPersonUserName + '\'' +
                '}';
    }

    public String getSubstituteName() {
        return substituteName;
    }

    public void setSubstituteName(String substituteName) {
        this.substituteName = substituteName;
    }

    public String getSubstituteId() {
        return substituteId;
    }

    public void setSubstituteId(String substituteId) {
        this.substituteId = substituteId;
    }

    public Date getSubstitutionStartDate() {
        return substitutionStartDate;
    }

    public void setSubstitutionStartDate(Date substitutionStartDate) {
        this.substitutionStartDate = substitutionStartDate;
    }

    public Date getSubstitutionEndDate() {
        return substitutionEndDate;
    }

    public void setSubstitutionEndDate(Date substitutionEndDate) {
        this.substitutionEndDate = substitutionEndDate;
    }

    public boolean isReadOnly() {
        if (substitutionEndDate == null) return false;

        Date currentDate = DateUtils.truncate(new Date(), Calendar.DATE);
        return substitutionEndDate.before(currentDate);
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getReplacedPersonUserName() {
        return replacedPersonUserName;
    }

    public void setReplacedPersonUserName(String replacedPersonUserName) {
        this.replacedPersonUserName = replacedPersonUserName;
    }
}
