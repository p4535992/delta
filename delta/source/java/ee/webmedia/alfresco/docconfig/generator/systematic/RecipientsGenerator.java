package ee.webmedia.alfresco.docconfig.generator.systematic;

import static org.alfresco.web.ui.common.StringUtils.encode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookMainViewDialog;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class RecipientsGenerator extends BaseSystematicFieldGenerator implements FieldGroupGenerator {

    public static final QName[] recipientFields = new QName[] {
            DocumentCommonModel.Props.RECIPIENT_NAME,
            DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME,
            DocumentCommonModel.Props.RECIPIENT_EMAIL,
            DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL,
            DocumentDynamicModel.Props.RECIPIENT_PERSON_NAME,
            DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_PERSON_NAME,
            DocumentDynamicModel.Props.RECIPIENT_STREET_HOUSE,
            DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_STREET_HOUSE,
            DocumentDynamicModel.Props.RECIPIENT_POSTAL_CITY,
            DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_POSTAL_CITY };

    private NamespaceService namespaceService;

    @Override
    protected QName[] getFieldIds() {
        return recipientFields;
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        if ((getFieldIds()[0].equals(field.getFieldId()) || getFieldIds()[1].equals(field.getFieldId())) && field.getParent() instanceof FieldGroup) {
            ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
            item.setComponentGenerator("MultiValueEditorGenerator");
            item.setShowInViewMode(false);
            item.setAddLabelId("document_add_recipient");
            item.setStyleClass("");
            List<String> props = new ArrayList<String>();
            List<QName> propNames = new ArrayList<QName>();
            FieldGroup group = (FieldGroup) field.getParent();
            for (Field recipientField : group.getFields()) {
                props.add(recipientField.getFieldId().toPrefixString(namespaceService) + "¤TextAreaGenerator¤styleClass=expand19-200");
                propNames.add(recipientField.getFieldId());
            }
            item.setPropsGeneration(StringUtils.join(props, ","));
            item.setPickerCallback("#{AddressbookDialog.searchContacts}");
            String stateHolderKey = field.getFieldId().toPrefixString(namespaceService);
            item.setSetterCallback(getBindingName("getContactData", stateHolderKey));
            String viewModePropName = RepoUtil.createTransientProp(field.getFieldId().getLocalName() + "Label").toString();
            ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(viewModePropName, group.getReadonlyFieldsName());
            viewModeItem.setComponentGenerator("UnescapedOutputTextGenerator");
            generatorResults.addStateHolder(stateHolderKey, new RecipientsState(propNames, viewModePropName));
        }
    }

    @Override
    public void generateFieldGroup(FieldGroup fieldGroup, GeneratorResults generatorResults) {
        // Do nothing
    }

    // ===============================================================================================================================

    public static class RecipientsState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final List<QName> propNames;
        private final String viewModePropName;

        public RecipientsState(List<QName> propNames, String viewModePropName) {
            this.propNames = propNames;
            this.viewModePropName = viewModePropName;
        }

        public List<String> getContactData(String nodeRef) {
            Map<QName, Serializable> props = BeanHelper.getNodeService().getProperties(new NodeRef(nodeRef));
            QName type = BeanHelper.getNodeService().getType(new NodeRef(nodeRef));
            List<String> list = new ArrayList<String>();
            for (QName propName : propNames) {
                String value = "";
                if (propName.equals(DocumentCommonModel.Props.RECIPIENT_NAME) || propName.equals(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME)) {
                    value = AddressbookMainViewDialog.getContactFullName(props, type);
                } else if (propName.equals(DocumentCommonModel.Props.RECIPIENT_EMAIL) || propName.equals(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL)) {
                    value = (String) props.get(AddressbookModel.Props.EMAIL);
                } else if (propName.equals(DocumentDynamicModel.Props.RECIPIENT_PERSON_NAME) || propName.equals(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_PERSON_NAME)) {
                } else if (propName.equals(DocumentDynamicModel.Props.RECIPIENT_STREET_HOUSE) || propName.equals(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_STREET_HOUSE)) {
                } else if (propName.equals(DocumentDynamicModel.Props.RECIPIENT_POSTAL_CITY) || propName.equals(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_POSTAL_CITY)) {
                } else {
                    throw new RuntimeException("Unknown property: " + propName);
                }
                list.add(value);
            }
            return list;
        }

        @Override
        protected void reset(boolean inEditMode) {
            final Node document = dialogDataProvider.getNode();
            if (!inEditMode) {
                int size = 0;
                List<List<String>> all = new ArrayList<List<String>>();
                for (QName propName : propNames) {
                    @SuppressWarnings("unchecked")
                    List<String> columnValues = (List<String>) document.getProperties().get(propName);
                    if (columnValues == null) {
                        columnValues = new ArrayList<String>();
                    }
                    size = Math.max(columnValues.size(), size);
                    all.add(columnValues);
                }

                List<String> rows = new ArrayList<String>(size);
                for (int i = 0; i < size; i++) {
                    List<String> rowValues = new ArrayList<String>();
                    for (List<String> columnValues : all) {
                        if (i < columnValues.size()) {
                            String value = StringUtils.trim(columnValues.get(i));
                            if (StringUtils.isNotBlank(value)) {
                                rowValues.add(encode(value));
                            }
                            // TODO email link??
                        }
                    }
                    if (!rowValues.isEmpty()) {
                        rows.add(StringUtils.join(rowValues, ", "));
                    }
                }
                document.getProperties().put(viewModePropName, StringUtils.join(rows, "<br/>"));
            }
        }

    }

    // ===============================================================================================================================

    // START: setters
    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }
    // END: setters

}
