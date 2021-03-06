package ee.webmedia.alfresco.orgstructure.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureServiceImpl;

public class OrganizationStructureConverter extends MultiSelectConverterBase {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OrganizationStructureServiceImpl.class);

    private transient OrganizationStructureService organizationStructureService;

    @Override
    protected String convertSelectedValueToString(Object value) {
        try {
            String unitId = DefaultTypeConverter.INSTANCE.convert(String.class, value);
            OrganizationStructure os = getOrganizationStructureService().getOrganizationStructure(unitId);
            if (os == null) {
                return value.toString();
            }
            return os.getName();
        } catch (NumberFormatException e) {
            log.debug("Conversion failed, input cannot be parsed as integer: '" + value.toString() + "' " + value.getClass().getCanonicalName());
            return value.toString();
        }
    }

    protected OrganizationStructureService getOrganizationStructureService() {
        if (organizationStructureService == null) {
            organizationStructureService = (OrganizationStructureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(OrganizationStructureService.BEAN_NAME);
        }
        return organizationStructureService;
    }

}
