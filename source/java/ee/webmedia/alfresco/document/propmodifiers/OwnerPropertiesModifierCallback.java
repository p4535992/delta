package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.utils.UserUtil;

public class OwnerPropertiesModifierCallback extends PropertiesModifierCallback {

    private PersonService personService;
    private AuthenticationService authenticationService;
    private NodeService nodeService;
    private OrganizationStructureService organizationStructureService;

    @Override
    public QName getAspectName() {
        return DocumentCommonModel.Aspects.OWNER;
    }

    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
        // same logic as MetadataBlockBean#setOwner

        NodeRef person = personService.getPerson(authenticationService.getCurrentUserName());
        Map<QName, Serializable> personProps = nodeService.getProperties(person);

        properties.put(DocumentCommonModel.Props.OWNER_ID, personProps.get(ContentModel.PROP_USERNAME));
        properties.put(DocumentCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(personProps));
        properties.put(DocumentCommonModel.Props.OWNER_JOB_TITLE, personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = organizationStructureService.getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        properties.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, orgstructName);
        properties.put(DocumentCommonModel.Props.OWNER_EMAIL, personProps.get(ContentModel.PROP_EMAIL));
        properties.put(DocumentCommonModel.Props.OWNER_PHONE, personProps.get(ContentModel.PROP_TELEPHONE));
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

}
