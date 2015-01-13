package ee.webmedia.alfresco.orgstructure.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = OrganizationStructureModel.URI)
public class OrganizationStructure implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int unitId;
    private int superUnitId;
    private List<String> organizationPath;

    @AlfrescoModelProperty(isMappable = false)
    private String organizationDisplayPath;

    @AlfrescoModelProperty(isMappable = false)
    private String superValueName;

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

    public void setOrganizationDisplayPath(String organizationDisplayPath) {
        this.organizationDisplayPath = organizationDisplayPath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nName = " + name + "\n");
        sb.append("unitId = " + unitId + "\n");
        sb.append("superUnitId = " + superUnitId + "\n");
        return sb.toString();
    }

    public static class NameComparator implements Comparator<OrganizationStructure> {

        @Override
        public int compare(OrganizationStructure o1, OrganizationStructure o2) {
            return AppConstants.DEFAULT_COLLATOR.compare(o1.getName(), o2.getName());
        }

    }

}
