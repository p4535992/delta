package ee.webmedia.alfresco.orgstructure.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

<<<<<<< HEAD
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = OrganizationStructureModel.URI)
public class OrganizationStructure implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
<<<<<<< HEAD
    private String unitId;
    private String superUnitId;
    private List<String> organizationPath;
    private String institutionRegCode;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef nodeRef;
    @AlfrescoModelProperty(isMappable = false)
    private String superValueName;

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

=======
    private int unitId;
    private int superUnitId;
    private List<String> organizationPath;

    @AlfrescoModelProperty(isMappable = false)
    private String organizationDisplayPath;

    @AlfrescoModelProperty(isMappable = false)
    private String superValueName;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSuperValueName() {
        return superValueName;
    }

    public void setSuperValueName(String superValueName) {
        this.superValueName = superValueName;
    }

<<<<<<< HEAD
    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getSuperUnitId() {
        return superUnitId;
    }

    public void setSuperUnitId(String superUnitId) {
=======
    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

    public int getSuperUnitId() {
        return superUnitId;
    }

    public void setSuperUnitId(int superUnitId) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        this.superUnitId = superUnitId;
    }

    public void setOrganizationPath(List<String> organizationPath) {
        this.organizationPath = organizationPath;
    }

    public List<String> getOrganizationPath() {
        return organizationPath;
    }

    public String getOrganizationDisplayPath() {
        return UserUtil.getDisplayUnit(organizationPath);
    }

<<<<<<< HEAD
    public String getInstitutionRegCode() {
        return institutionRegCode;
    }

    public void setInstitutionRegCode(String institutionRegCode) {
        this.institutionRegCode = institutionRegCode;
=======
    public void setOrganizationDisplayPath(String organizationDisplayPath) {
        this.organizationDisplayPath = organizationDisplayPath;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public String toString() {
<<<<<<< HEAD
        StringBuilder sb = new StringBuilder("OrganizationStructure[");
        sb.append("\n  nodeRef=").append(nodeRef);
        sb.append("\n  unitId=").append(unitId);
        sb.append("\n  superUnitId=").append(superUnitId);
        sb.append("\n  superValueName=").append(superValueName);
        sb.append("\n  name=").append(name);
        sb.append("\n  organizationPath=").append(organizationPath == null ? null : StringUtils.join(organizationPath, '|'));
        sb.append("\n  institutionRegCode=").append(institutionRegCode);
        sb.append("\n]");
=======
        StringBuilder sb = new StringBuilder();
        sb.append("\nName = " + name + "\n");
        sb.append("unitId = " + unitId + "\n");
        sb.append("superUnitId = " + superUnitId + "\n");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        return sb.toString();
    }

    public static class NameComparator implements Comparator<OrganizationStructure> {

        @Override
        public int compare(OrganizationStructure o1, OrganizationStructure o2) {
            return AppConstants.DEFAULT_COLLATOR.compare(o1.getName(), o2.getName());
        }

    }

}
