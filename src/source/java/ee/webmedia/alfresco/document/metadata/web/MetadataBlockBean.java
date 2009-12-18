package ee.webmedia.alfresco.document.metadata.web;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.propertysheet.relateddropdown.RelatedDropdown;
import ee.webmedia.alfresco.common.propertysheet.relateddropdown.RelatedDropdownGenerator;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.TransientProps;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Alar Kvell
 */
public class MetadataBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;
    private transient PersonService personService;
    private transient NodeService nodeService;
    private transient OrganizationStructureService organizationStructureService;
    private transient DocumentTypeService documentTypeService;
    private transient FunctionsService functionsService;
    private transient SeriesService seriesService;
    private transient VolumeService volumeService;
    private transient DocumentTemplateService documentTemplateService;
    private transient UIPropertySheet propertySheet;

    private Node document;
    private boolean inEditMode;
    private DateFormat dateFormat;
    private String documentTypeName;

    public MetadataBlockBean() {
        String datePattern = Application.getMessage(FacesContext.getCurrentInstance(), "date_pattern");
        dateFormat = new SimpleDateFormat(datePattern);
    }

    public void setOwner(String userName) {
        NodeRef person = getPersonService().getPerson(userName);
        Map<QName, Serializable> personProps = getNodeService().getProperties(person);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentCommonModel.Props.OWNER_ID.toString(), personProps.get(ContentModel.PROP_USERNAME));
        docProps.put(DocumentCommonModel.Props.OWNER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentCommonModel.Props.OWNER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        docProps.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.toString(), orgstructName);
        docProps.put(DocumentCommonModel.Props.OWNER_EMAIL.toString(), personProps.get(ContentModel.PROP_EMAIL));
        docProps.put(DocumentCommonModel.Props.OWNER_PHONE.toString(), personProps.get(ContentModel.PROP_TELEPHONE));
    }

    public void setSigner(String userName) {
        NodeRef person = getPersonService().getPerson(userName);
        Map<QName, Serializable> personProps = getNodeService().getProperties(person);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentCommonModel.Props.SIGNER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentCommonModel.Props.SIGNER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
    }

    protected void afterModeChange() {
        if (!inEditMode) {
            Map<String, Object> props = document.getProperties();

            if (document.hasAspect(DocumentCommonModel.Aspects.ACCESS_RIGHTS)) {
                Date accessRestrictionBeginDate = (Date) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
                Date accessRestrictionEndDate = (Date) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE);

                String accessRestrictionDate = "";
                if (accessRestrictionBeginDate != null || accessRestrictionEndDate != null) {
                    if (accessRestrictionBeginDate == null) {
                        accessRestrictionDate = "...";
                    } else {
                        accessRestrictionDate = dateFormat.format(accessRestrictionBeginDate);
                    }
                    accessRestrictionDate += " - ";
                    if (accessRestrictionEndDate == null) {
                        accessRestrictionDate += "...";
                    } else {
                        accessRestrictionDate += dateFormat.format(accessRestrictionEndDate);
                    }
                }
                props.put("accessRestrictionDate", accessRestrictionDate);
            }

            if (document.hasAspect(DocumentCommonModel.Aspects.OWNER)) {
                String owner = Utils.encode((String) props.get(DocumentCommonModel.Props.OWNER_NAME));
                List<String> ownerProps = new ArrayList<String>(4);
                String ownerJobTitle = (String) props.get(DocumentCommonModel.Props.OWNER_JOB_TITLE);
                if (!StringUtils.isBlank(ownerJobTitle)) {
                    ownerProps.add(Utils.encode(ownerJobTitle));
                }
                String ownerOrgStructUnit = (String) props.get(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT);
                if (!StringUtils.isBlank(ownerOrgStructUnit)) {
                    ownerProps.add(Utils.encode(ownerOrgStructUnit));
                }
                String ownerEmail = (String) props.get(DocumentCommonModel.Props.OWNER_EMAIL);
                if (!StringUtils.isBlank(ownerEmail)) {
                    ownerProps.add(generateEmailLink(ownerEmail));
                }
                String ownerPhone = (String) props.get(DocumentCommonModel.Props.OWNER_PHONE);
                if (!StringUtils.isBlank(ownerPhone)) {
                    ownerProps.add(Utils.encode(ownerPhone));
                }

                String ownerDetails = StringUtils.join(ownerProps.iterator(), ", ");
                props.put("owner", joinStringAndStringWithParentheses(owner, ownerDetails));
            }

            if (document.hasAspect(DocumentCommonModel.Aspects.COMMON)) {
                String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
                Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
                props.put("regNumberDate", joinStringAndDateWithComma(regNumber, regDateTime));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.SENDER)) {
                String senderRegNumber = (String) props.get(DocumentSpecificModel.Props.SENDER_REG_NUMBER);
                Date senderRegDateTime = (Date) props.get(DocumentSpecificModel.Props.SENDER_REG_DATE);
                props.put("senderRegNumberDate", joinStringAndDateWithComma(senderRegNumber, senderRegDateTime));
            }

            if (document.hasAspect(DocumentCommonModel.Aspects.SIGNER)) {
                String signer = (String) props.get(DocumentCommonModel.Props.SIGNER_NAME);
                String signerJobTitle = (String) props.get(DocumentCommonModel.Props.SIGNER_JOB_TITLE);
                props.put("signer", joinStringAndStringWithParentheses(signer, signerJobTitle));
            }

            if (document.hasAspect(DocumentCommonModel.Aspects.RECIPIENT)) {
                @SuppressWarnings("unchecked")
                List<String> recipientNames = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_NAME);
                @SuppressWarnings("unchecked")
                List<String> recipientEmails = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL);
                props.put("recipients", generateNameAndEmailTable(recipientNames, recipientEmails));
            }

            if (document.hasAspect(DocumentCommonModel.Aspects.ADDITIONAL_RECIPIENT)) {
                @SuppressWarnings("unchecked")
                List<String> recipientNames = (List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
                @SuppressWarnings("unchecked")
                List<String> recipientEmails = (List<String>) props.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL);
                props.put("additionalRecipients", generateNameAndEmailTable(recipientNames, recipientEmails));
            }
        }
    }
    
    /**
     * Query callback method executed by the component generated by GeneralSelectorGenerator.
     * This method is part of the contract to the GeneralSelectorGenerator, it is up to the backing bean
     * to execute whatever query is appropriate and populate <code>selectComponent</code> with selection items.<br>
     * 
     * @param context - FacesContext for creating selection items
     * @param selectComponent - selectComponent that will be rendered(use <code>selectComponent.getChildren()</code> to add selection items)
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public void findDocumentTemplates(FacesContext context, HtmlSelectOneMenu selectComponent) {
        List<DocumentTemplate> docTemplates = getDocumentTemplateService().getTemplates();
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();
        for (DocumentTemplate tmpl : docTemplates) {
            if (document.getType().toString().equals(tmpl.getDocTypeId())) {  //FIXME documenttemplate.doctypeid must be qname
                UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
                selectItem.setItemLabel(FilenameUtils.removeExtension(tmpl.getName()));
                selectItem.setItemValue(tmpl.getNodeRef().toString());
                selectOptions.add(selectItem);
            }
        }
        // empty default selection
        UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
        selectItem.setItemLabel("");
        selectItem.setItemValue("");
        selectOptions.add(0, selectItem);
    }

    /**
     * Query callback method executed by the component generated by {@link RelatedDropdown} or {@link RelatedDropdownGenerator}.
     * This method is part of the contract to the {@link RelatedDropdownGenerator}, it is up to the backing bean
     * to execute whatever query is appropriate and populate <code>selectComponent</code> with selection items.<br>
     * 
     * @param context - FacesContext for creating selection items
     * @param selectComponent - selectComponent that will be rendered(use <code>selectComponent.getChildren()</code> to add selection items)
     * @param submittedValue - value submitted by the previous {@link RelatedDropdown} (RelatedDropdown, that belongs to the same group and has order equal to order of given <code>selectComponent.order -1 </code>) in the same group.
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public void findAllFunctions(FacesContext context, HtmlSelectOneMenu selectComponent, Object submittedValue) {
        List<Function> functions = getFunctionsService().getAllFunctions(DocListUnitStatus.OPEN);
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();
        for (Function function : functions) {
            List<Series> allSeries = getSeriesService().getAllSeriesByFunction(function.getNodeRef(), DocListUnitStatus.OPEN, document.getType());
            if (allSeries.size() == 0) {
                continue;
            }
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(function.getMark() + " " + function.getTitle());
            selectItem.setItemValue(function.getNodeRef().toString());
            selectOptions.add(selectItem);
        }
    }

    /**
     * Query callback method executed by the component generated by {@link RelatedDropdown} or {@link RelatedDropdownGenerator}.
     * This method is part of the contract to the {@link RelatedDropdownGenerator}, it is up to the backing bean
     * to execute whatever query is appropriate and populate <code>selectComponent</code> with selection items.<br>
     * 
     * @param context - FacesContext for creating selection items
     * @param selectComponent - selectComponent that will be rendered(use <code>selectComponent.getChildren()</code> to add selection items)
     * @param submittedValue - value submitted by the previous {@link RelatedDropdown} (RelatedDropdown, that belongs to the same group and has order equal to order of given <code>selectComponent.order -1 </code>) in the same group.
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public void findAllSeries(FacesContext context, HtmlSelectOneMenu selectComponent, Object submittedValue) {
        String sVal = (String) submittedValue;
        if(StringUtils.isBlank(sVal)) {
            return;
        }
        final NodeRef functionNodeRef = new NodeRef(sVal);
        List<Series> allSeries = getSeriesService().getAllSeriesByFunction(functionNodeRef, DocListUnitStatus.OPEN, document.getType());
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();
        selectOptions.removeAll(selectOptions);
        for (Series series : allSeries) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(series.getSeriesIdentifier() + " " + series.getTitle());
            selectItem.setItemValue(series.getNode().getNodeRef().toString());
            selectOptions.add(selectItem);
        }
        UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
        selectItem.setItemLabel("");
        selectItem.setItemValue("");
        selectOptions.add(0, selectItem);
    }

    /**
     * Query callback method executed by the component generated by {@link RelatedDropdown} or {@link RelatedDropdownGenerator}.
     * This method is part of the contract to the {@link RelatedDropdownGenerator}, it is up to the backing bean
     * to execute whatever query is appropriate and populate <code>selectComponent</code> with selection items.<br>
     * 
     * @param context - FacesContext for creating selection items
     * @param selectComponent - selectComponent that will be rendered(use <code>selectComponent.getChildren()</code> to add selection items)
     * @param submittedValue - value submitted by the previous {@link RelatedDropdown} (RelatedDropdown, that belongs to the same group and has order equal to order of given <code>selectComponent.order -1 </code>) in the same group.
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public void findAllVolumes(FacesContext context, HtmlSelectOneMenu selectComponent, Object submittedValue) {
        String sVal = (String) submittedValue;
        if(StringUtils.isBlank(sVal)) {
            return;
        }
        final NodeRef seriesNodeRef = new NodeRef(sVal);
        List<Volume> volumes = getVolumeService().getAllValidVolumesBySeries(seriesNodeRef);
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();
        selectOptions.removeAll(selectOptions);
        for (Volume volume : volumes) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(volume.getVolumeMark() + " " + volume.getTitle());
            selectItem.setItemValue(volume.getNode().getNodeRef().toString());
            selectOptions.add(selectItem);
        }
        // empty default selection
        UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
        selectItem.setItemLabel("");
        selectItem.setItemValue("");
        selectOptions.add(0, selectItem);
    }

    protected String generateNameAndEmailTable(List<String> names, List<String> emails) {
        int size = 0;
        if (names != null) {
            size = names.size();
        } else if (emails != null) {
            size = emails.size();
        }
        List<String> rows = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            String name = null;
            if (names != null && i < names.size()) {
                name = names.get(i);
            }
            String email = null;
            if (emails != null && i < emails.size()) {
                email = emails.get(i);
            }
            String row = joinStringAndStringWithComma(Utils.encode(name), generateEmailLink(email));
            if (!StringUtils.isBlank(row)) {
                rows.add(row);
            }
        }
        return StringUtils.join(rows.iterator(), "<br/>");
    }

    protected String generateEmailLink(String email) {
        if (StringUtils.isBlank(email)) {
            return email;
        }
        return "<a href=\"mailto:" + Utils.encode(email) + "\">" + Utils.encode(email) + "</a>";
    }

    protected String joinStringAndDateWithComma(String value1, Date date) {
        String value2 = "";
        if (date != null) {
            value2 = dateFormat.format(date);
        }
        return joinStringAndStringWithComma(value1, value2);
    }

    protected String joinStringAndStringWithComma(String value1, String value2) {
        String result = "";
        if (!StringUtils.isBlank(value1)) {
            result += value1;
        }
        if (value2 != null) {
            if (!StringUtils.isBlank(result)) {
                result += ", ";
            }
            result += value2;
        }
        return result;
    }

    protected String joinStringAndStringWithParentheses(String value1, String value2) {
        String result = "";
        if (!StringUtils.isBlank(value1)) {
            result += value1;
        }
        if (!StringUtils.isBlank(value2)) {
            if (!StringUtils.isBlank(result)) {
                result += " ";
            }
            result += "(" + value2 + ")";
        }
        return result;
    }

    public void init(NodeRef nodeRef, boolean created) {
        document = getDocumentService().getDocument(nodeRef);
        inEditMode = created;
        propertySheet = null;
        DocumentType documentType = getDocumentTypeService().getDocumentType(document.getType());
        documentTypeName = documentType != null ? documentType.getName() : null;
        afterModeChange();
        reloadTransientProperties();
    }

    public void reset() {
        document = null;
        inEditMode = false;
        propertySheet = null;
        documentTypeName = null;
        reloadTransientProperties();
    }

    public void edit(ActionEvent event) {
        if (inEditMode) {
            throw new RuntimeException("Document metadata block is already in edit mode");
        }
        document = getDocumentService().getDocument(document.getNodeRef());
        inEditMode = true;
        propertySheet.setMode(getMode());
        propertySheet.getChildren().clear();
        reloadTransientProperties();
        afterModeChange();
    }

    public void save() {
        if (!inEditMode) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }
        if (validate()) {
            document = getDocumentService().updateDocument(document);
            inEditMode = false;
            reloadTransientProperties();
            propertySheet.setMode(getMode());
            propertySheet.getChildren().clear();
            afterModeChange();
        }
    }

    private boolean validate() throws ValidatorException {
        NodeRef targetParentRef = null;
        try {
            final String volumeNodeRef = (String) document.getProperties().get(TransientProps.VOLUME_NODEREF);
            targetParentRef = new NodeRef(volumeNodeRef);
        } catch (Exception e) {
        }
        if (targetParentRef == null) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_validationMsg_mandatory_functionSeriesVolume");
            return false;
        }
        return true;
    }

    public void cancel() {
        if (!inEditMode) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }
        document = getDocumentService().getDocument(document.getNodeRef());
        inEditMode = false;
        propertySheet.setMode(getMode());
        propertySheet.getChildren().clear();
        reloadTransientProperties();
        afterModeChange();
    }

    public String getMode() {
        return inEditMode ? UIPropertySheet.EDIT_MODE : UIPropertySheet.VIEW_MODE;
    }

    private void reloadTransientProperties() {
        if (document == null) {
            return;
        }
        final Map<String, Object> props = document.getProperties();
        Node[] parentNodes = getDocumentService().getAncestorNodesByDocument(document.getNodeRef());
        Node volumeNode = parentNodes[0];
        Node seriesNode = parentNodes[1];
        Node functionNode = parentNodes[2];
        if (inEditMode) {
            // put props with empty values if missing, otherwise use existing values
            props.put(TransientProps.FUNCTION_NODEREF, functionNode != null ? functionNode.getNodeRef().toString() : null);
            props.put(TransientProps.SERIES_NODEREF, seriesNode != null ? seriesNode.getNodeRef().toString() : null);
            props.put(TransientProps.VOLUME_NODEREF, volumeNode != null ? volumeNode.getNodeRef().toString() : null);
        } else {
            String volumeLbl = volumeNode != null ? volumeNode.getProperties().get(VolumeModel.Props.MARK).toString() //
                    + " " + volumeNode.getProperties().get(VolumeModel.Props.TITLE).toString() : " ";
            String seriesLbl = seriesNode != null ? seriesNode.getProperties().get(SeriesModel.Props.SERIES_IDENTIFIER).toString() //
                    + " " + seriesNode.getProperties().get(SeriesModel.Props.TITLE).toString() : " ";
            String functionLbl = functionNode != null ? functionNode.getProperties().get(FunctionsModel.Props.MARK).toString() //
                    + " " + functionNode.getProperties().get(FunctionsModel.Props.TITLE).toString() : " ";
            props.put(TransientProps.FUNCTION_NODEREF, functionLbl);
            props.put(TransientProps.SERIES_NODEREF, seriesLbl);
            props.put(TransientProps.VOLUME_NODEREF, volumeLbl);
        }
    }

    public boolean isEditAllowed() {
        return true;
    }

    // START: getters / setters
    public String getDocumentTypeName() {
        return documentTypeName;
    }

    public Node getDocument() {
        return document;
    }

    public boolean isInEditMode() {
        return inEditMode;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
        propertySheet.setMode(getMode());
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    protected PersonService getPersonService() {
        if (personService == null) {
            personService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPersonService();
        }
        return personService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    protected NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
        }
        return nodeService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

    protected OrganizationStructureService getOrganizationStructureService() {
        if (organizationStructureService == null) {
            organizationStructureService = (OrganizationStructureService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(OrganizationStructureService.BEAN_NAME);
        }
        return organizationStructureService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }
    
    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }
    
    protected DocumentTemplateService getDocumentTemplateService() {
        if (documentTemplateService == null) {
            documentTemplateService = (DocumentTemplateService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(DocumentTemplateService.BEAN_NAME);
        }
        return documentTemplateService;
    }

    protected DocumentTypeService getDocumentTypeService() {
        if (documentTypeService == null) {
            documentTypeService = (DocumentTypeService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    DocumentTypeService.BEAN_NAME);
        }
        return documentTypeService;
    }

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }

    protected SeriesService getSeriesService() {
        if (seriesService == null) {
            seriesService = (SeriesService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(SeriesService.BEAN_NAME);
        }
        return seriesService;
    }

    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }
    // END: getters / setters
}
