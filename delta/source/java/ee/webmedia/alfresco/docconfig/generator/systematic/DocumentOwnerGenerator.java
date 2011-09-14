package ee.webmedia.alfresco.docconfig.generator.systematic;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * @author Alar Kvell
 */
public class DocumentOwnerGenerator extends BaseSystematicFieldGenerator implements FieldGroupGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentOwnerGenerator.class);

    @Override
    protected QName[] getFieldIds() {
        return new QName[] { DocumentCommonModel.Props.OWNER_NAME };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (DocumentCommonModel.Props.OWNER_NAME.equals(field.getFieldId())) {
            item.setComponentGenerator("UserSearchGenerator");
            item.setUsernameProp(DocumentCommonModel.Props.OWNER_ID.toPrefixString(BeanHelper.getNamespaceService()));
            item.setPickerCallback("#{UserListDialog.searchUsers}");
            item.setSetterCallback(getBindingName("setOwner"));
            item.setEditable(false);
            item.setAjaxParentLevel(1);

            // TODO in view mode, use code from MetadataBlockBean ("owner")

            return;
        }
        throw new RuntimeException("Unsupported field: " + field);
    }

    @Override
    public void generateFieldGroup(FieldGroup fieldGroup, GeneratorResults generatorResults) {
        generatorResults.addStateHolder(getStateHolderKey(), new DocumentOwnerState());
    }

    // ===============================================================================================================================

    public static class DocumentOwnerState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        public void setOwner(String userName) {
            Map<QName, Serializable> personProps = getPersonProps(userName);
            Map<String, Object> docProps = dialogDataProvider.getNode().getProperties();
            docProps.put(DocumentCommonModel.Props.OWNER_ID.toString(), personProps.get(ContentModel.PROP_USERNAME));
            docProps.put(DocumentCommonModel.Props.OWNER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
            docProps.put(DocumentCommonModel.Props.OWNER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
            String orgstructName = BeanHelper.getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
            docProps.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.toString(), orgstructName);
            docProps.put(DocumentCommonModel.Props.OWNER_EMAIL.toString(), personProps.get(ContentModel.PROP_EMAIL));
            docProps.put(DocumentCommonModel.Props.OWNER_PHONE.toString(), personProps.get(ContentModel.PROP_TELEPHONE));
        }

        private Map<QName, Serializable> getPersonProps(String userName) {
            NodeRef person = BeanHelper.getPersonService().getPerson(userName);
            Map<QName, Serializable> personProps = BeanHelper.getNodeService().getProperties(person);
            return personProps;
        }

    }

    // ===============================================================================================================================

}
