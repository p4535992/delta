package ee.webmedia.alfresco.document.metadata.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.utils.TextUtil.joinStringAndStringWithComma;
import static ee.webmedia.alfresco.utils.TextUtil.joinStringAndStringWithParentheses;
import static ee.webmedia.alfresco.utils.TextUtil.joinStringAndStringWithSpace;
import static org.alfresco.web.ui.common.StringUtils.encode;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import com.ibm.icu.util.GregorianCalendar;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookMainViewDialog;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.propertysheet.converter.DoubleCurrencyConverter;
import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.ClearStateNotificationHandler.ClearStateListener;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.sendout.web.DocumentSendOutDialog;
import ee.webmedia.alfresco.document.service.DocLockService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.TransientProps;
import ee.webmedia.alfresco.document.service.EventsLoggingHelper;
import ee.webmedia.alfresco.document.service.InMemoryChildNodeHelper;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.dvk.service.ExternalReviewException;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.user.web.UserListDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Alar Kvell
 */
public class MetadataBlockBean implements ClearStateListener {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MetadataBlockBean.class);
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "MetadataBlockBean";

    private transient DocumentService documentService;
    private transient PersonService personService;
    private transient NodeService nodeService;
    private transient OrganizationStructureService organizationStructureService;
    private transient DocumentTypeService documentTypeService;
    private transient FunctionsService functionsService;
    private transient SeriesService seriesService;
    private transient VolumeService volumeService;
    private transient CaseService caseService;
    private transient DocLockService docLockService;
    private transient GeneralService generalService;
    private transient DocumentTemplateService documentTemplateService;
    private transient ParametersService parametersService;
    private transient AddressbookService addressbookService;
    private transient UserService userService;
    private transient WorkflowService workflowService;
    private transient DvkService dvkService;
    private transient ClassificatorService classificatorService;
    private UserListDialog userListDialog;
    private transient UIPropertySheet propertySheet;
    private transient InMemoryChildNodeHelper inMemoryChildNodeHelper;
    private final SelectItem[] contactOrUserSearchFilters;

    private Node document;
    private Node propertySheetControlDocument;
    private boolean inEditMode;
    private boolean isDraft;
    private DateFormat dateFormat;
    private String documentTypeName;
    private DocumentDialog documentDialog;

    /** timeOut in seconds how often lock should be refreshed to avoid expiring */
    private Integer lockRefreshTimeout;
    private NodeRef nodeRef;
    private boolean skipInvoiceMessages;

    public MetadataBlockBean() {
        String datePattern = Application.getMessage(FacesContext.getCurrentInstance(), "date_pattern");
        dateFormat = new SimpleDateFormat(datePattern);
        contactOrUserSearchFilters = new SelectItem[] {
                new SelectItem(0, MessageUtil.getMessage("task_owner_users")),
                new SelectItem(1, MessageUtil.getMessage("task_owner_contacts")),
        };
    }

    public String getLeavingLetterContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"message\">");
        sb.append(getParametersService().getStringParameter(Parameters.LEAVING_LETTER_CONTENT));
        sb.append("</div>");
        return sb.toString();
    }

    public void setApplicantName(String userName, Node applicantNode) {
        setApplicantName(userName, applicantNode.getProperties(), applicantNode.getAspects());
    }

    public Map<QName, Serializable> setApplicantName(String userName, Map<String, Object> targetProps, Set<QName> docAspects) {
        Map<QName, Serializable> personProps = getPersonProps(userName);
        targetProps.put(DocumentSpecificModel.Props.APPLICANT_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        targetProps.put(DocumentSpecificModel.Props.APPLICANT_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        targetProps.put(DocumentSpecificModel.Props.APPLICANT_STRUCT_UNIT_NAME.toString(), orgstructName);

        // Set properties related to certain aspects if needed
        if (docAspects == null || docAspects.isEmpty()) {
            return RepoUtil.toQNameProperties(targetProps);
        }

        if (StringUtils.isNotBlank(orgstructName) && docAspects.contains(DocumentSpecificModel.Aspects.ERRAND_ORDER_APPLICANT_ABROAD_V2)) {
            targetProps.put(DocumentSpecificModel.Props.COST_MANAGER.toString(), getActiveClassificatorValue("errandOrderAbroadCostManager", orgstructName));
            targetProps.put(DocumentSpecificModel.Props.EXPENDITURE_ITEM.toString(), getActiveClassificatorValue("errandOrderExpenditureItem", orgstructName));
        }

        if (StringUtils.isNotBlank(orgstructName) && docAspects.contains(DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2)) {
            targetProps.put(DocumentSpecificModel.Props.COST_MANAGER.toString(),
                    getActiveClassificatorValue("errandApplicationDomesticCostManager", orgstructName));
            targetProps.put(DocumentSpecificModel.Props.COST_ELEMENT.toString(),
                    getActiveClassificatorValue("errandApplicationDomesticCostElement", orgstructName));
        }

        return RepoUtil.toQNameProperties(targetProps);
    }

    private String getActiveClassificatorValue(String classificatorName, String classificatorComment) {
        final List<ClassificatorValue> activeClassificatorValues = getClassificatorService().getActiveClassificatorValues(
                getClassificatorService().getClassificatorByName(classificatorName));
        for (ClassificatorValue classificatorValue : activeClassificatorValues) {
            if (StringUtils.equalsIgnoreCase(classificatorValue.getClassificatorDescription(), classificatorComment)) {
                return classificatorValue.getValueName();
            }
        }
        return "";
    }

    public void setPartyName(String nodeRefStr, Node partyNode) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        Map<String, Object> partyProps = partyNode.getProperties();
        partyProps.put(DocumentSpecificModel.Props.PARTY_NAME.toString(), getAddressbookOrgOrName(nodeRef));
        partyProps.put(DocumentSpecificModel.Props.PARTY_EMAIL.toString(), getNodeService().getProperty(nodeRef, AddressbookModel.Props.EMAIL));
    }

    public void setErrandSubstituteName(String userName, Node applicantNode) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = applicantNode.getProperties();
        docProps.put(DocumentSpecificModel.Props.ERRAND_SUBSTITUTE_NAME.toString(), UserUtil.getPersonFullName1(personProps));
    }

    public void setSkipInvoiceMessages(boolean skipInvoiceMessages) {
        this.skipInvoiceMessages = skipInvoiceMessages;
    }

    private String getAddressbookOrgOrName(NodeRef nodeRef) {
        String result = "";
        Map<QName, Serializable> props = getNodeService().getProperties(nodeRef);
        if (AddressbookModel.Types.ORGANIZATION.equals(getNodeService().getType(nodeRef))) {
            result = (String) props.get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else {
            result = UserUtil.getPersonFullName(props.get(AddressbookModel.Props.PERSON_FIRST_NAME).toString(), props.get(
                    AddressbookModel.Props.PERSON_LAST_NAME).toString());
        }
        return result;
    }

    private void setRecipient(String nodeRefStr, QName name, QName email) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        Map<String, Object> docProps = document.getProperties();
        docProps.put(name.toString(), getAddressbookOrgOrName(nodeRef));
        docProps.put(email.toString(), getNodeService().getProperty(nodeRef, AddressbookModel.Props.EMAIL));
    }

    public void setSecondParty(String nodeRefStr) {
        setRecipient(nodeRefStr, DocumentSpecificModel.Props.SECOND_PARTY_NAME, DocumentSpecificModel.Props.SECOND_PARTY_EMAIL);
    }

    public void setThirdParty(String nodeRefStr) {
        setRecipient(nodeRefStr, DocumentSpecificModel.Props.THIRD_PARTY_NAME, DocumentSpecificModel.Props.THIRD_PARTY_EMAIL);
    }

    public void setFirstParty(String nodeRefStr) {
        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.FIRST_PARTY_NAME.toString(), getAddressbookOrgOrName(new NodeRef(nodeRefStr)));
    }

    public void setMinutesDirector(String userName) {
        Map<String, Object> docProps = document.getProperties();
        Map<QName, Serializable> personProps = getPersonProps(userName);
        docProps.put(DocumentSpecificModel.Props.MINUTES_DIRECTOR.toString(), UserUtil.getPersonFullName1(personProps));
    }

    public void setMinutesRecorder(String userName) {
        Map<String, Object> docProps = document.getProperties();
        Map<QName, Serializable> personProps = getPersonProps(userName);
        docProps.put(DocumentSpecificModel.Props.MINUTES_RECORDER.toString(), UserUtil.getPersonFullName1(personProps));
    }

    public void setSenderSigner(String nodeRefStr) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.SENDER_SIGNER.toString(), getAddressbookOrgOrName(nodeRef));
    }

    public void setSenderWriter(String nodeRefStr) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.SENDER_WRITER.toString(), getAddressbookOrgOrName(nodeRef));
    }

    public void setResponsible(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.RESPONSIBLE_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        docProps.put(DocumentSpecificModel.Props.RESPONSIBLE_STRUCT_UNIT.toString(), orgstructName);
    }

    public void setOrganization(String nodeRefStr) {
        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.RESPONSIBLE_ORGANIZATION.toString(), getAddressbookOrgOrName(new NodeRef(nodeRefStr)));
    }

    public void setRapporteur(String nodeRefStr) {
        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.RAPPORTEUR_NAME.toString(), getAddressbookOrgOrName(new NodeRef(nodeRefStr)));
    }

    public void setOwnerCurrentUser() {
        setOwner(AuthenticationUtil.getRunAsUser());
    }

    public void setOwner(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentCommonModel.Props.OWNER_ID.toString(), personProps.get(ContentModel.PROP_USERNAME));
        docProps.put(DocumentCommonModel.Props.OWNER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentCommonModel.Props.OWNER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        docProps.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.toString(), orgstructName);
        docProps.put(DocumentCommonModel.Props.OWNER_EMAIL.toString(), personProps.get(ContentModel.PROP_EMAIL));
        docProps.put(DocumentCommonModel.Props.OWNER_PHONE.toString(), personProps.get(ContentModel.PROP_TELEPHONE));
    }

    private Map<QName, Serializable> getPersonProps(String userName) {
        NodeRef person = getPersonService().getPerson(userName);
        Map<QName, Serializable> personProps = getNodeService().getProperties(person);
        return personProps;
    }

    public void setWhom(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.WHOM_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentSpecificModel.Props.WHOM_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
    }

    public void setProcurementOfficialResponsible(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);
        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.PROCUREMENT_OFFICIAL_RESPONSIBLE.toString(), UserUtil.getPersonFullName1(personProps));
    }

    public void setSigner(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentCommonModel.Props.SIGNER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentCommonModel.Props.SIGNER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
    }

    public void setReportMvRapporteur(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);
        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.RAPPORTEUR_NAME.toString(), UserUtil.getPersonFullName1(personProps));
    }

    public void setDeliverer(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.DELIVERER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentSpecificModel.Props.DELIVERER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        docProps.put(DocumentSpecificModel.Props.DELIVERER_STRUCT_UNIT.toString(), orgstructName);
    }

    public void setCompensationApplicantName(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.COMPENSATION_APPLICANT_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentSpecificModel.Props.COMPENSATION_APPLICANT_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        docProps.put(DocumentSpecificModel.Props.COMPENSATION_APPLICANT_STRUCT_UNIT_NAME.toString(), orgstructName);
    }

    public void setReceiver(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.RECEIVER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentSpecificModel.Props.RECEIVER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        docProps.put(DocumentSpecificModel.Props.RECEIVER_STRUCT_UNIT.toString(), orgstructName);
    }

    public void setSender(String nodeRefStr) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        String name = getOrgOrPersonName(nodeRef);
        document.getProperties().put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME.toString(), name);
    }

    private String getOrgOrPersonName(NodeRef nodeRef) {
        Map<QName, Serializable> props = getNodeService().getProperties(nodeRef);
        QName contactType = getNodeService().getType(nodeRef);
        return AddressbookMainViewDialog.getContactFullName(props, contactType);
    }

    public void setApplicantInstitution(String nodeRefStr) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        document.getProperties().put(DocumentSpecificModel.Props.APPLICANT_INSTITUTION.toString(), getOrgOrPersonName(nodeRef));
    }

    public void setApplicantPerson(String searchResult) {
        String name = getUserOrContactName(searchResult);
        document.getProperties().put(DocumentSpecificModel.Props.APPLICANT_PERSON.toString(), name);
    }

    public void setSellerPartyName(String nodeRefStr) {
        Map<QName, Serializable> props = getNodeService().getProperties(new NodeRef(nodeRefStr));
        document.getProperties().put(DocumentSpecificModel.Props.SELLER_PARTY_NAME.toString(), props.get(AddressbookModel.Props.ORGANIZATION_NAME));
        document.getProperties().put(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER.toString(), props.get(AddressbookModel.Props.ORGANIZATION_CODE));
        document.getProperties().put(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT.toString(), props.get(AddressbookModel.Props.SAP_ACCOUNT));
    }

    public String getUserOrContactName(String searchResult) {
        String name = null;
        if (StringUtils.isBlank(searchResult)) {
            return name;
        }
        if (searchResult.indexOf('/') > -1) { // contact
            NodeRef contact = new NodeRef(searchResult);
            Map<QName, Serializable> resultProps = getNodeService().getProperties(contact);
            QName resultType = getNodeService().getType(contact);
            if (resultType.equals(Types.ORGANIZATION)) {
                name = (String) resultProps.get(AddressbookModel.Props.ORGANIZATION_NAME);
            } else {
                name = UserUtil.getPersonFullName((String) resultProps.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) resultProps
                        .get(AddressbookModel.Props.PERSON_LAST_NAME));
            }
        } else { // user
            Map<QName, Serializable> personProps = getUserService().getUserProperties(searchResult);
            name = UserUtil.getPersonFullName1(personProps);
        }
        return name;
    }

    public void setCoApplicantInstitution(String nodeRefStr) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        document.getProperties().put(DocumentSpecificModel.Props.CO_APPLICANT_INSTITUTION.toString(), getOrgOrPersonName(nodeRef));
    }

    public void setCoApplicantPerson(String searchResult) {
        String name = getUserOrContactName(searchResult);
        document.getProperties().put(DocumentSpecificModel.Props.CO_APPLICANT_PERSON.toString(), name);
    }

    public List<String> setVacationSubstitute(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        List<String> list = new ArrayList<String>(3);
        list.add(UserUtil.getPersonFullName1(personProps));
        list.add((String) personProps.get(ContentModel.PROP_JOBTITLE));
        list.add(null);
        list.add(null);
        return list;
    }

    public List<String> setProcurementApplicantName(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);
        List<String> list = new ArrayList<String>(3);
        list.add(UserUtil.getPersonFullName1(personProps));
        list.add((String) personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        list.add(orgstructName);
        return list;
    }

    public void setApplicationRecipient(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.APPLICATION_RECIPIENT.toString(), UserUtil.getPersonFullName1(personProps));
    }

    public boolean isShowVacationAdd() {
        return StringUtils.isNotBlank((String) document.getProperties().get("{temp}vacationAddText"));
    }

    public boolean isShowVacationChange() {
        return StringUtils.isNotBlank((String) document.getProperties().get("{temp}vacationChangeText"));
    }

    private String formatDateOrEmpty(Date date) {
        if (date == null) {
            return "";
        }
        return dateFormat.format(date);
    }

    private String formatIntegerOrEmpty(Integer integer) {
        if (integer == null) {
            return "";
        }
        return Integer.toString(integer);
    }

    private String formatDoubleOrEmpty(Double dbl) {
        if (dbl == null) {
            return "";
        }
        return EInvoiceUtil.getInvoiceNumberFormat().format(dbl.doubleValue());
    }

    protected void afterModeChange() {
        Map<String, Object> props = document.getProperties();
        if (!inEditMode) {

            String comment = (String) props.get(DocumentCommonModel.Props.COMMENT);
            if (comment != null) {
                comment = StringUtils.replace(encode(comment), "\n", "<br/>");
            }
            props.put("{temp}comment", comment);

            if (document.hasAspect(DocumentSpecificModel.Aspects.CONTRACT_SIM_DETAILS)) {
                props.put("{temp}secondPartyContractDate", props.get(DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_DATE));
            }

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
                // TODO move this code to DocumentOwnerGenerator

                String owner = encode((String) props.get(DocumentCommonModel.Props.OWNER_NAME));
                List<String> ownerProps = new ArrayList<String>(4);
                String ownerJobTitle = (String) props.get(DocumentCommonModel.Props.OWNER_JOB_TITLE);
                if (!StringUtils.isBlank(ownerJobTitle)) {
                    ownerProps.add(encode(ownerJobTitle));
                }
                String ownerOrgStructUnit = (String) props.get(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT);
                if (!StringUtils.isBlank(ownerOrgStructUnit)) {
                    ownerProps.add(encode(ownerOrgStructUnit));
                }
                String ownerEmail = (String) props.get(DocumentCommonModel.Props.OWNER_EMAIL);
                if (!StringUtils.isBlank(ownerEmail)) {
                    ownerProps.add(generateEmailLink(ownerEmail));
                }
                String ownerPhone = (String) props.get(DocumentCommonModel.Props.OWNER_PHONE);
                if (!StringUtils.isBlank(ownerPhone)) {
                    ownerProps.add(encode(ownerPhone));
                }

                String ownerDetails = StringUtils.join(ownerProps.iterator(), ", ");
                String ownerId = (String) props.get(DocumentCommonModel.Props.OWNER_ID);
                String substitutionInfo = UserUtil.getSubstitute(ownerId);
                String finalOwnerDetails = joinStringAndStringWithParentheses(owner, ownerDetails);
                if (!StringUtils.isBlank(substitutionInfo)) {
                    finalOwnerDetails = joinStringAndStringWithSpace(finalOwnerDetails, "<span class=\"fieldExtraInfo\">" + substitutionInfo + "</span>");
                }
                props.put("owner", finalOwnerDetails);
            }

            if (document.hasAspect(DocumentCommonModel.Aspects.COMMON)) {
                String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
                Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
                props.put("regNumberDate", joinStringAndDateWithComma(regNumber, regDateTime));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.DELIVERER)) {
                String delivererName = (String) props.get(DocumentSpecificModel.Props.DELIVERER_NAME);
                String delivererJobTitle = (String) props.get(DocumentSpecificModel.Props.DELIVERER_JOB_TITLE);
                String delivererStructUnit = (String) props.get(DocumentSpecificModel.Props.DELIVERER_STRUCT_UNIT);
                props.put("deliverer", joinStringAndStringWithParentheses(delivererName, joinStringAndStringWithComma(delivererJobTitle, delivererStructUnit)));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.RECEIVER)) {
                String receiverName = (String) props.get(DocumentSpecificModel.Props.RECEIVER_NAME);
                String receiverJobTitle = (String) props.get(DocumentSpecificModel.Props.RECEIVER_JOB_TITLE);
                String receiverStructUnit = (String) props.get(DocumentSpecificModel.Props.RECEIVER_STRUCT_UNIT);
                props.put("receiver", joinStringAndStringWithParentheses(receiverName, joinStringAndStringWithComma(receiverJobTitle, receiverStructUnit)));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.PERSONAL_VEHICLE_USAGE_COMPENSATION_MV)) {
                String applicantName = (String) props.get(DocumentSpecificModel.Props.COMPENSATION_APPLICANT_NAME);
                String applicantJobTitle = (String) props.get(DocumentSpecificModel.Props.COMPENSATION_APPLICANT_JOB_TITLE);
                String applicantStructUnit = (String) props.get(DocumentSpecificModel.Props.COMPENSATION_APPLICANT_STRUCT_UNIT_NAME);
                props.put("{temp}compensationApplicant",
                        joinStringAndStringWithParentheses(applicantName, joinStringAndStringWithComma(applicantJobTitle, applicantStructUnit)));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.PROJECT_APPLICATION)) {
                String applicantName = (String) props.get(DocumentSpecificModel.Props.APPLICANT_PERSON);
                String applicantInstitution = (String) props.get(DocumentSpecificModel.Props.APPLICANT_INSTITUTION);
                String coApplicantName = (String) props.get(DocumentSpecificModel.Props.APPLICANT_PERSON);
                String coApplicantInstitution = (String) props.get(DocumentSpecificModel.Props.APPLICANT_INSTITUTION);
                props.put("{temp}applicantInstitutionPerson", joinStringAndStringWithComma(applicantInstitution, applicantName));
                props.put("{temp}coApplicantInstitutionPerson", joinStringAndStringWithComma(coApplicantInstitution, coApplicantName));

                @SuppressWarnings("unchecked")
                List<String> financerInstitutions = (List<String>) props.get(DocumentSpecificModel.Props.CO_FINANCER_INSTITUTION);
                @SuppressWarnings("unchecked")
                List<String> financerPersons = (List<String>) props.get(DocumentSpecificModel.Props.CO_FINANCER_PERSONA);
                @SuppressWarnings("unchecked")
                List<Double> financerSums = (List<Double>) props.get(DocumentSpecificModel.Props.CO_FINANCER_SUM);
                props.put("{temp}coFinancers", generateCoFinancerTable(financerInstitutions, financerPersons, financerSums));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.SECOND_PARTY_REG)) {
                String secondPartyRegNumber = (String) props.get(DocumentSpecificModel.Props.SECOND_PARTY_REG_NUMBER);
                Date secondPartyRegDate = (Date) props.get(DocumentSpecificModel.Props.SECOND_PARTY_REG_DATE);
                props.put("secondPartyRegNumberDate", joinStringAndDateWithComma(secondPartyRegNumber, secondPartyRegDate));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.SENDER)) {
                String senderRegNumber = (String) props.get(DocumentSpecificModel.Props.SENDER_REG_NUMBER);
                Date senderRegDateTime = (Date) props.get(DocumentSpecificModel.Props.SENDER_REG_DATE);
                props.put("senderRegNumberDate", joinStringAndDateWithComma(senderRegNumber, senderRegDateTime));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.SENDER_DETAILS)) {
                String senderName = (String) props.get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
                if (!document.getType().equals(DocumentSubtypeModel.Types.INCOMING_LETTER_MV)) {
                    String senderEmail = (String) props.get(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL);
                    props.put("senderNameEmail", joinStringAndStringWithComma(senderName, senderEmail));
                } else {
                    props.put("senderNameEmail", senderName);
                }

            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.SENDER_DETAILS_MV)) {
                String senderAddress1 = (String) props.get(DocumentSpecificModel.Props.SENDER_ADDRESS1);
                String senderAddress2 = (String) props.get(DocumentSpecificModel.Props.SENDER_ADDRESS2);
                String senderPostalCode = (String) props.get(DocumentSpecificModel.Props.SENDER_POSTAL_CODE);
                String senderName = (String) props.get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
                String senderEmail = (String) props.get(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL);
                String senderPhone = (String) props.get(DocumentSpecificModel.Props.SENDER_PHONE);
                props.put("{temp}senderAddressPostalCode",
                        joinStringAndStringWithComma(joinStringAndStringWithComma(senderAddress1, senderAddress2), senderPostalCode));
                props.put("{temp}senderWriterEmailPhone", joinStringAndStringWithComma(joinStringAndStringWithComma(senderName, senderEmail), senderPhone));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.INVOICE)) {
                String sellerName = (String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
                String sellerRegNumber = (String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER);
                String sellerSapAccount = (String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT);
                props.put("{temp}invoiceSeller",
                        MessageUtil.getMessage("document_invoiceSellerParty_text", sellerName, sellerRegNumber, sellerSapAccount == null ? "" : sellerSapAccount));

                String sellerContactName = (String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_CONTACT_NAME);
                String sellerContactPhone = (String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_CONTACT_PHONE_NUMBER);
                String sellerContactEmail = (String) props.get(DocumentSpecificModel.Props.SELLER_PARTY_CONTACT_EMAIL_ADDRESS);
                props.put("{temp}invoiceSellerContact", joinStringAndStringWithComma(joinStringAndStringWithComma(sellerContactName, sellerContactPhone), sellerContactEmail));

                Date dueDate = (Date) props.get(DocumentSpecificModel.Props.INVOICE_DUE_DATE);
                String paymentTerm = (String) props.get(DocumentSpecificModel.Props.PAYMENT_TERM);
                props.put("{temp}invoiceDueDatePaymentTerm", joinDateAndStringWithComma(dueDate, paymentTerm));

                String invoiceNumber = (String) props.get(DocumentSpecificModel.Props.INVOICE_NUMBER);
                Date invoiceDate = (Date) props.get(DocumentSpecificModel.Props.INVOICE_DATE);
                props.put("{temp}invoiceNumberDate", joinStringAndDateWithComma(invoiceNumber, invoiceDate));

                String currency = (String) props.get(DocumentSpecificModel.Props.CURRENCY);
                if (currency == null) {
                    currency = "";
                }
                String totalSumStr = formatDoubleOrEmpty((Double) props.get(DocumentSpecificModel.Props.TOTAL_SUM));
                String sumWithoutVatStr = formatDoubleOrEmpty((Double) props.get(DocumentSpecificModel.Props.INVOICE_SUM));
                String vatStr = formatDoubleOrEmpty((Double) props.get(DocumentSpecificModel.Props.VAT));
                props.put("{temp}invoiceTotalSum", MessageUtil.getMessage("document_invoice_total_sum_text", totalSumStr, currency, sumWithoutVatStr, currency, vatStr, currency));

                addEntrySapDateAndNumber(props);

                props.put("{temp}xxlInvoice", Boolean.TRUE.equals(props.get(DocumentSpecificModel.Props.XXL_INVOICE)) ? MessageUtil.getMessage("document_invoiceXxlInvoice") : "");
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.WHOM)) {
                String whomName = (String) props.get(DocumentSpecificModel.Props.WHOM_NAME);
                String whomJobTitle = (String) props.get(DocumentSpecificModel.Props.WHOM_JOB_TITLE);
                props.put("whomNameAndJobTitle", joinStringAndStringWithParentheses(whomName, whomJobTitle));
            }

            if (document.hasAspect(DocumentCommonModel.Aspects.SIGNER)) {
                String signer = (String) props.get(DocumentCommonModel.Props.SIGNER_NAME);
                String signerJobTitle = (String) props.get(DocumentCommonModel.Props.SIGNER_JOB_TITLE);
                props.put("{temp}signer", joinStringAndStringWithParentheses(signer, signerJobTitle));
            } else if (document.hasAspect(DocumentCommonModel.Aspects.SIGNER_NAME)) {
                props.put("{temp}signer", props.get(DocumentCommonModel.Props.SIGNER_NAME));
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

            if (document.hasAspect(DocumentSpecificModel.Aspects.MANAGEMENTS_ORDER_DETAILS)) {
                String responsible = encode((String) props.get(DocumentSpecificModel.Props.RESPONSIBLE_NAME));
                String responsibleStructUnit = (String) props.get(DocumentSpecificModel.Props.RESPONSIBLE_STRUCT_UNIT);
                props.put("responsible", joinStringAndStringWithParentheses(responsible, responsibleStructUnit));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.VACATION_ORDER_COMMON)) {
                Boolean leaveAnnual = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL);
                Boolean leaveWithoutPay = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY);
                Boolean leaveChild = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CHILD);
                Boolean leaveStudy = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_STUDY);
                StringBuilder sb = new StringBuilder();
                boolean isMvVacationApp = document.getType().equals(DocumentSubtypeModel.Types.VACATION_APPLICATION);
                boolean isSmitVacationOrder = document.getType().equals(DocumentSubtypeModel.Types.VACATION_ORDER_SMIT);

                FacesContext context = FacesContext.getCurrentInstance();
                if (BooleanUtils.isTrue(leaveAnnual)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL_END_DATE));
                    if (!isMvVacationApp) {
                        String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL_DAYS));
                        String msg = document.getType().equals(DocumentSubtypeModel.Types.VACATION_ORDER_SMIT) //
                                ? "document_leaveAnnualSmit" : "document_leaveAnnual";
                        sb.append(encode(MessageUtil.getMessage(context, msg, from, to, days)));
                    } else {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveAnnualMv", from, to)));
                    }
                }
                if (BooleanUtils.isTrue(leaveWithoutPay)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_END_DATE));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    if (!isMvVacationApp) {
                        String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_DAYS));
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveWithoutPay", from, to, days)));
                    } else {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveWithoutPayMv", from, to)));
                    }
                }
                if (BooleanUtils.isTrue(leaveChild)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_END_DATE));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    if (!isMvVacationApp) {
                        String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_DAYS));
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveChild", from, to, days)));
                    } else {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveChildMv", from, to)));
                    }
                }
                if (BooleanUtils.isTrue(leaveStudy)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_END_DATE));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    if (!isMvVacationApp) {
                        String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_DAYS));
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveStudy", from, to, days)));
                    } else {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveStudyMv", from, to)));
                    }
                }
                if (StringUtils.isNotBlank(sb.toString())) {
                    props.put("{temp}vacationAddText", sb.toString());
                }

                Boolean leaveChange = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CHANGE);
                Boolean leaveCancel = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL);
                sb = new StringBuilder();
                if (BooleanUtils.isTrue(leaveChange)) {
                    if (!isSmitVacationOrder) {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveChange1")));
                    } else {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveChangeSmit1")));
                    }
                    sb.append("<br/>");
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATE));
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveChange2", from, to)));
                    sb.append("<br/>");
                    String newFrom = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE));
                    String newTo = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_NEW_END_DATE));
                    if (!isMvVacationApp) {
                        String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_NEW_DAYS));
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveChange3", newFrom, newTo, days)));
                    } else {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveChangeMv3", newFrom, newTo)));
                    }
                }
                if (BooleanUtils.isTrue(leaveCancel)) {
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    if (!isSmitVacationOrder) {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveCancel1")));
                    } else {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveCancelSmit1")));
                    }
                    sb.append("<br/>");
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE));
                    if (!isMvVacationApp) {
                        String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_DAYS));
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveCancel2", from, to, days)));
                    } else {
                        sb.append(encode(MessageUtil.getMessage(context, "document_leaveCancelMv2", from, to)));
                    }
                }
                if (StringUtils.isNotBlank(sb.toString())) {
                    props.put("{temp}vacationChangeText", sb.toString());
                }
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.VACATION_ORDER_COMMON)
                    || document.hasAspect(DocumentSpecificModel.Aspects.VACATION_ORDER_COMMON_V2)) {
                FacesContext context = FacesContext.getCurrentInstance();

                StringBuilder table = new StringBuilder("<table cellspacing='0' cellpadding='0' class='recipient padding'><thead><tr><th>");
                table.append(MessageUtil.getMessage(context, "document_vacationSubstitute2"));
                table.append("</th><th>").append(MessageUtil.getMessage(context, "user_jobtitle"));
                table.append("</th><th>").append(MessageUtil.getMessage(context, "from"));
                table.append("</th><th>").append(MessageUtil.getMessage(context, "to"));
                table.append("</th></tr></thead><tbody>");
                @SuppressWarnings("unchecked")
                List<String> names = (List<String>) props.get(DocumentSpecificModel.Props.SUBSTITUTE_NAME);
                @SuppressWarnings("unchecked")
                List<String> jobTitles = (List<String>) props.get(DocumentSpecificModel.Props.SUBSTITUTE_JOB_TITLE);
                @SuppressWarnings("unchecked")
                List<Date> begins = (List<Date>) props.get(DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE);
                @SuppressWarnings("unchecked")
                List<Date> ends = (List<Date>) props.get(DocumentSpecificModel.Props.SUBSTITUTION_END_DATE);
                if (names != null) {
                    for (int i = 0; i < names.size(); i++) {
                        String name = "";
                        if (names.get(i) != null) {
                            name = names.get(i);
                        }
                        String jobTitle = "";
                        if (jobTitles != null && jobTitles.get(i) != null) {
                            jobTitle = jobTitles.get(i);
                        }
                        String begin = "";
                        if (begins != null && i < begins.size() && begins.get(i) != null) {
                            begin = dateFormat.format(begins.get(i));
                        }
                        String end = "";
                        if (ends != null && i < ends.size() && ends.get(i) != null) {
                            end = dateFormat.format(ends.get(i));
                        }
                        if (StringUtils.isBlank(name) && StringUtils.isBlank(begin) && StringUtils.isBlank(end)) {
                            continue;
                        }
                        table.append("<tr><td>").append(encode(name)).append("</td><td>");
                        table.append(encode(jobTitle)).append("</td><td>");
                        table.append(encode(begin)).append("</td><td>");
                        table.append(encode(end)).append("</td></tr>");
                    }
                    table.append("</tbody></table>");
                    props.put("{temp}vacationSubstitute", table.toString());
                }
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.VACATION_ORDER_COMMON_V2)) {
                // Substitute classificator values with descriptions
                @SuppressWarnings("unchecked")
                List<String> leaveTypes = (List<String>) props.get(DocumentSpecificModel.Props.LEAVE_TYPE);
                if (leaveTypes != null && !leaveTypes.isEmpty()) {
                    List<ClassificatorValue> classificators = getClassificatorService().getActiveClassificatorValues(
                            getClassificatorService().getClassificatorByName("leaveType"));
                    List<String> leaveTypesDesc = new ArrayList<String>(leaveTypes.size());
                    types: for (String leaveType : leaveTypes) {
                        for (ClassificatorValue classificator : classificators) {
                            if (classificator.getValueName().equals(leaveType)) {
                                leaveTypesDesc.add(classificator.getClassificatorDescription());
                                continue types;
                            }
                        }
                    }
                    props.put(DocumentSpecificModel.Props.LEAVE_TYPE.toString(), leaveTypesDesc);
                }
            }

        } else {
            /** in Edit mode */
            NodeRef functionRef = (NodeRef) props.get(DocumentService.TransientProps.FUNCTION_NODEREF);
            NodeRef seriesRef = (NodeRef) props.get(DocumentService.TransientProps.SERIES_NODEREF);
            NodeRef volumeRef = (NodeRef) props.get(DocumentService.TransientProps.VOLUME_NODEREF);
            String caseLabel = (String) props.get(DocumentService.TransientProps.CASE_LABEL_EDITABLE);
            updateFnSerVol(functionRef, seriesRef, volumeRef, caseLabel, true);

            if (DocumentSubtypeModel.Types.LEAVING_LETTER.equals(document.getType())) {
                /** Document name must be filled for this type. */
                props.put(DocumentCommonModel.Props.DOC_NAME.toString(), documentTypeName);
            }

            if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(document.getType())) {
                props.put(DocumentSpecificModel.Props.FIRST_PARTY_NAME.toString(), MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_sim"));
            }

            if (DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(document.getType())) {
                props.put(DocumentSpecificModel.Props.FIRST_PARTY_NAME.toString(), MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_smit"));
            }
        }
    }

    private void addEntrySapDateAndNumber(Map<String, Object> props) {
        String entryDateStr = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.ENTRY_DATE));
        String entrySapNumber = (String) props.get(DocumentSpecificModel.Props.ENTRY_SAP_NUMBER);
        props.put("{temp}entrySapDateAndNumber", joinStringAndStringWithComma(entryDateStr, entrySapNumber));
    }

    public void addInvoiceMessages() {
        if (isDraft || !DocumentSubtypeModel.Types.INVOICE.equals(document.getType())) {
            return;
        }
        if (skipInvoiceMessages) {
            skipInvoiceMessages = false;
            return;
        }
        String sellerPartyRegNumber = (String) document.getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER);
        if (StringUtils.isNotBlank(sellerPartyRegNumber)) {
            List<Node> contacts = getAddressbookService().getContactsByRegNumber(sellerPartyRegNumber);
            String sellerPartyName = (String) document.getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
            for (Node contact : contacts) {
                if (getNodeService().getType(contact.getNodeRef()).equals(AddressbookModel.Types.ORGANIZATION)) {
                    String contactName = (String) contact.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME);
                    if (!(contactName == null && sellerPartyName == null)) {
                        if (!StringUtils.equalsIgnoreCase(sellerPartyName, contactName)) {
                            MessageUtil.addInfoMessage("document_invoice_different_seller_name");
                        }
                    }
                }
            }
        }
        if (document.getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT) == null) {
            MessageUtil.addInfoMessage("document_invoice_no_seller_sap_account");
        }
        for (Transaction transaction : documentDialog.getTransactionsBlockBean().getTransactions()) {
            if (StringUtils.isNotBlank(transaction.getAssetInventoryNumber())) {
                MessageUtil.addInfoMessage("document_invoice_assetInventoryNumberFilled");
                break;
            }
        }
        String regNumber = (String) document.getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER);
        String invoiceNumber = (String) document.getProperties().get(DocumentSpecificModel.Props.INVOICE_NUMBER);
        Date invoiceDate = (Date) document.getProperties().get(DocumentSpecificModel.Props.INVOICE_DATE);
        List<Document> similarDocuments = BeanHelper.getDocumentSearchService().searchSimilarInvoiceDocuments(regNumber, invoiceNumber, invoiceDate);
        if (similarDocuments != null) {
            for (Document document : similarDocuments) {
                if (!document.getNodeRef().equals(nodeRef)) {
                    MessageUtil.addInfoMessage("document_invoice_similar_document", invoiceNumber, formatDateOrEmpty(invoiceDate),
                            document.getProperties().get(DocumentSpecificModel.Props.SELLER_PARTY_NAME), regNumber);
                }
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
    public List<SelectItem> findDocumentTemplates(FacesContext context, UIInput selectComponent) {
        List<DocumentTemplate> docTemplates = getDocumentTemplateService().getDocumentTemplates(document.getType());
        List<SelectItem> selectItems = new ArrayList<SelectItem>(docTemplates.size() + 1);

        // Empty default selection
        selectItems.add(new SelectItem("", ""));

        for (DocumentTemplate tmpl : docTemplates) {
            selectItems.add(new SelectItem(tmpl.getName(), FilenameUtils.removeExtension(tmpl.getName())));
        }

        // If we have only 1 match, then preselect it
        if (selectItems.size() == 2) {
            selectComponent.setValue(selectItems.get(1).getValue());
        } else {
            WebUtil.sort(selectItems);
        }
        return selectItems;
    }

    /**
     * Called after selection has been made from series dropdown.<br>
     * If accessRestriction is not filled, then values related to accessRestriction are set according to selected series.
     * 
     * @param submittedValue
     */
    public void updateAccessRestrictionProperties(NodeRef seriesRef) {
        final Map<String, Object> docProps = document.getProperties();
        final String accessRestriction = (String) docProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
        if (StringUtils.isBlank(accessRestriction)) {
            // read serAccessRestriction-related values from series
            final Series series = getSeriesService().getSeriesByNodeRef(seriesRef);
            final Map<String, Object> seriesProps = series.getNode().getProperties();
            final String serAccessRestriction = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
            final String serAccessRestrictionReason = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString());
            final Date serAccessRestrictionBeginDate = (Date) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString());
            final Date serAccessRestrictionEndDate = (Date) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString());
            final String serAccessRestrictionEndDesc = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.toString());
            // write them to the document
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString(), serAccessRestriction);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString(), serAccessRestrictionReason);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString(), serAccessRestrictionBeginDate);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString(), serAccessRestrictionEndDate);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.toString(), serAccessRestrictionEndDesc);
        }
    }

    public void removeApplicant(ActionEvent event) {
        final Node docNode = getParentNode(event);
        final String assocIndexParam = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_ASSOC_INDEX);
        final int assocIndex = Integer.parseInt(assocIndexParam);
        getInMemoryChildNodeHelper().removeApplicant(docNode, assocIndex);
    }

    /**
     * Add applicant childNode to errand(abroad/domestic) document or trainingApplication document
     * 
     * @param event
     */
    public void addApplicant(ActionEvent event) {
        final Node docNode = getParentNode(event);
        getInMemoryChildNodeHelper().addApplicant(docNode);
    }

    public void removeErrand(ActionEvent event) {
        final Node applicantNode = getParentNode(event);
        final String assocIndexParam = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_ASSOC_INDEX);
        final int assocIndex = Integer.parseInt(assocIndexParam);
        getInMemoryChildNodeHelper().removeErrand(applicantNode, assocIndex);
    }

    public void addErrand(ActionEvent event) {
        final Node applicantNode = getParentNode(event);
        getInMemoryChildNodeHelper().addErrand(applicantNode, document);
    }

    public void addParty(ActionEvent event) {
        final Node docNode = getParentNode(event);
        getInMemoryChildNodeHelper().addParty(docNode);
    }

    public void removeParty(ActionEvent event) {
        final Node docNode = getParentNode(event);
        final String assocIndexParam = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_ASSOC_INDEX);
        final int assocIndex = Integer.parseInt(assocIndexParam);
        getInMemoryChildNodeHelper().removeParty(docNode, assocIndex);
    }

    private Node getParentNode(ActionEvent event) {
        final SubPropertySheetItem propSheet = ComponentUtil.getAncestorComponent(event.getComponent(), SubPropertySheetItem.class);
        return propSheet.getParentPropSheetNode();
    }

    // ===============================================================================================================================

    private List<SelectItem> functions;
    private List<SelectItem> series;
    private List<SelectItem> volumes;
    private List<String> cases;

    /**
     * @param context
     * @param selectComponent
     * @return dropdown items for JSP
     */
    public List<SelectItem> getFunctions(FacesContext context, UIInput selectComponent) {
        return functions;
    }

    /**
     * @param context
     * @param selectComponent
     * @return dropdown items for JSP
     */
    public List<SelectItem> getSeries(FacesContext context, UIInput selectComponent) {
        return series;
    }

    /**
     * @param context
     * @param selectComponent
     * @return dropdown items for JSP
     */
    public List<SelectItem> getVolumes(FacesContext context, UIInput selectComponent) {
        return volumes;
    }

    /**
     * @param context
     * @param selectComponent
     * @return dropdown items for JSP
     */
    public List<String> getCases(FacesContext context, UIInput selectComponent) {
        return cases;
    }

    public void functionValueChanged(ValueChangeEvent event) {
        NodeRef functionRef = (NodeRef) event.getNewValue();
        updateFnSerVol(functionRef, null, null, null, false);
    }

    public void seriesValueChanged(ValueChangeEvent event) {
        NodeRef functionRef = (NodeRef) document.getProperties().get(DocumentService.TransientProps.FUNCTION_NODEREF);
        NodeRef seriesRef = (NodeRef) event.getNewValue();
        updateFnSerVol(functionRef, seriesRef, null, null, false);
    }

    public void volumeValueChanged(ValueChangeEvent event) {
        NodeRef functionRef = (NodeRef) document.getProperties().get(DocumentService.TransientProps.FUNCTION_NODEREF);
        NodeRef seriesRef = (NodeRef) document.getProperties().get(DocumentService.TransientProps.SERIES_NODEREF);
        NodeRef volumeRef = (NodeRef) event.getNewValue();
        updateFnSerVol(functionRef, seriesRef, volumeRef, null, false);
    }

    private void updateFnSerVol(NodeRef functionRef, NodeRef seriesRef, NodeRef volumeRef, String caseLabel, boolean addIfMissing) {
        { // Function
            List<Function> allFunctions = getFunctionsService().getAllFunctions(DocListUnitStatus.OPEN);
            functions = new ArrayList<SelectItem>(allFunctions.size());
            functions.add(new SelectItem("", ""));
            boolean functionFound = false;
            for (Function function : allFunctions) {
                List<Series> openSeries = getSeriesService().getAllSeriesByFunction(function.getNodeRef(), DocListUnitStatus.OPEN, document.getType());
                if (openSeries.size() == 0) {
                    continue;
                }
                functions.add(new SelectItem(function.getNode().getNodeRef(), function.getMark() + " " + function.getTitle()));
                if (functionRef != null && functionRef.equals(function.getNode().getNodeRef())) {
                    functionFound = true;
                }
            }
            if (!functionFound) {
                if (addIfMissing && functionRef != null && getNodeService().exists(functionRef)) {
                    Function function = getFunctionsService().getFunctionByNodeRef(functionRef);
                    functions.add(1, new SelectItem(function.getNode().getNodeRef(), function.getMark() + " " + function.getTitle()));
                } else {
                    functionRef = null;
                }
            }
            // If list contains only one value, then select it right away
            if (functions.size() == 2) {
                functions.remove(0);
                if (functionRef == null) {
                    functionRef = (NodeRef) functions.get(0).getValue();
                }
            }
        }

        if (functionRef == null) {
            series = null;
            seriesRef = null;
        } else {
            List<Series> allSeries = getSeriesService().getAllSeriesByFunction(functionRef, DocListUnitStatus.OPEN, document.getType());
            series = new ArrayList<SelectItem>(allSeries.size());
            series.add(new SelectItem("", ""));
            boolean serieFound = false;
            for (Series serie : allSeries) {
                series.add(new SelectItem(serie.getNode().getNodeRef(), serie.getSeriesIdentifier() + " " + serie.getTitle()));
                if (seriesRef != null && seriesRef.equals(serie.getNode().getNodeRef())) {
                    serieFound = true;
                }
            }
            if (!serieFound) {
                if (addIfMissing && seriesRef != null && getNodeService().exists(seriesRef)) {
                    Series serie = getSeriesService().getSeriesByNodeRef(seriesRef);
                    series.add(1, new SelectItem(serie.getNode().getNodeRef(), serie.getSeriesIdentifier() + " " + serie.getTitle()));
                } else {
                    seriesRef = null;
                }
            }
            // If list contains only one value, then select it right away
            if (series.size() == 2) {
                series.remove(0);
                if (seriesRef == null) {
                    seriesRef = (NodeRef) series.get(0).getValue();
                }
            }
        }

        if (seriesRef == null) {
            volumes = null;
            volumeRef = null;
        } else {
            UIPropertySheet ps = getPropertySheetInner();
            if (ps == null) { // when metadata block is first rendered
                updateAccessRestrictionProperties(seriesRef);
            } else { // when value change event is fired
                final NodeRef finalSeriesRef = seriesRef;
                ActionEvent event = new ActionEvent(ps) {
                    private static final long serialVersionUID = 1L;

                    boolean notExecuted = true;

                    @Override
                    public void processListener(FacesListener faceslistener) {
                        notExecuted = false;
                        updateAccessRestrictionProperties(finalSeriesRef);
                    }

                    @Override
                    public boolean isAppropriateListener(FacesListener faceslistener) {
                        return notExecuted;
                    }
                };
                event.setPhaseId(PhaseId.INVOKE_APPLICATION);
                ps.queueEvent(event);
            }

            List<Volume> allVolumes = getVolumeService().getAllValidVolumesBySeries(seriesRef, DocListUnitStatus.OPEN);
            volumes = new ArrayList<SelectItem>(allVolumes.size());
            volumes.add(new SelectItem("", ""));
            boolean volumeFound = false;
            for (Volume volume : allVolumes) {
                volumes.add(new SelectItem(volume.getNode().getNodeRef(), volume.getVolumeMark() + " " + volume.getTitle()));
                if (volumeRef != null && volumeRef.equals(volume.getNode().getNodeRef())) {
                    volumeFound = true;
                }
            }
            if (!volumeFound) {
                if (addIfMissing && volumeRef != null && getNodeService().exists(volumeRef)) {
                    Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);
                    volumes.add(1, new SelectItem(volume.getNode().getNodeRef(), volume.getVolumeMark() + " " + volume.getTitle()));
                } else {
                    volumeRef = null;
                }
            }
            // If list contains only one value, then select it right away
            if (volumes.size() == 2) {
                volumes.remove(0);
                if (volumeRef == null) {
                    volumeRef = (NodeRef) volumes.get(0).getValue();
                }
            }
        }

        if (volumeRef == null) {
            cases = null;
            caseLabel = null;
        } else {
            if (getVolumeService().getVolumeByNodeRef(volumeRef).isContainsCases()) {
                List<Case> allCases = getCaseService().getAllCasesByVolume(volumeRef, DocListUnitStatus.OPEN);
                cases = new ArrayList<String>(allCases.size());
                for (Case tmpCase : allCases) {
                    cases.add(tmpCase.getTitle());
                }
                if (StringUtils.isBlank(caseLabel) && cases.size() == 1) {
                    caseLabel = cases.get(0);
                }
            } else {
                cases = null;
                caseLabel = null;
            }
        }

        if (getPropertySheet() != null) {
            @SuppressWarnings("unchecked")
            List<UIComponent> children = getPropertySheet().getChildren();
            for (UIComponent component : children) {
                if (component.getId().endsWith("_function")) {
                    HtmlSelectOneMenu functionList = (HtmlSelectOneMenu) component.getChildren().get(1);
                    ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), functionList, functions);
                    functionList.setValue(functionRef);
                } else if (component.getId().endsWith("_series")) {
                    HtmlSelectOneMenu seriesList = (HtmlSelectOneMenu) component.getChildren().get(1);
                    ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), seriesList, series);
                    seriesList.setValue(seriesRef);
                } else if (component.getId().endsWith("_volume")) {
                    HtmlSelectOneMenu volumeList = (HtmlSelectOneMenu) component.getChildren().get(1);
                    ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), volumeList, volumes);
                    volumeList.setValue(volumeRef);
                } else if (component.getId().endsWith("_case_Lbl_Editable")) {
                    UIInput caseList = (UIInput) component.getChildren().get(1);
                    SuggesterGenerator.setValue(caseList, cases);
                    caseList.setValue(caseLabel);
                    component.setRendered(cases != null);
                }
            }
        }

        // These only apply when called initially during creation of a new document
        // If called from eventlistener, then model values are updated after and thus overwritten
        document.getProperties().put(DocumentService.TransientProps.FUNCTION_NODEREF, functionRef);
        document.getProperties().put(DocumentService.TransientProps.SERIES_NODEREF, seriesRef);
        document.getProperties().put(DocumentService.TransientProps.VOLUME_NODEREF, volumeRef);
        document.getProperties().put(DocumentService.TransientProps.CASE_LABEL_EDITABLE, caseLabel);
    }

    public void ministersOrderWhoseChanged(ValueChangeEvent event) {
        String newValue = event.getNewValue().toString();
        String[] values = null;
        for (ClassificatorValue value : getClassificatorService().getActiveClassificatorValues(
                getClassificatorService().getClassificatorByName("ministersOrderWhose"))) {
            if (value.getValueName().equals(newValue) && StringUtils.isNotBlank(value.getClassificatorDescription())) {
                values = value.getClassificatorDescription().split(";");
                break;
            }
        }

        if (values == null) {
            return;
        }

        // Set volume
        if (values.length > 0 && StringUtils.isNotBlank(values[0])) {
            String mark = values[0];
            NodeRef functionRef = (NodeRef) document.getProperties().get(DocumentService.TransientProps.FUNCTION_NODEREF);
            NodeRef seriesRef = (NodeRef) document.getProperties().get(DocumentService.TransientProps.SERIES_NODEREF);
            if (functionRef != null && seriesRef != null) {
                List<Volume> allVolumes = getVolumeService().getAllValidVolumesBySeries(seriesRef, DocListUnitStatus.OPEN);
                for (Volume volume : allVolumes) {
                    if (mark.equals(volume.getVolumeMark())) {
                        updateFnSerVol(functionRef, seriesRef, volume.getNode().getNodeRef(), null, false);
                        break;
                    }
                }
            }
        }

        // Set template
        if (values.length > 1 && StringUtils.isNotBlank(values[1]) && getPropertySheet() != null) {
            String templateName = values[1];
            @SuppressWarnings("unchecked")
            List<UIComponent> children = getPropertySheet().getChildren();
            children: for (UIComponent component : children) {
                if (!component.getId().endsWith("_templateName")) {
                    continue;
                }
                HtmlSelectOneMenu templateList = (HtmlSelectOneMenu) component.getChildren().get(1);
                @SuppressWarnings({ "unchecked", "cast" })
                List<UISelectItem> templateListValues = (List<UISelectItem>) templateList.getChildren();
                for (UISelectItem select : templateListValues) {
                    String itemValue = (String) select.getItemValue();
                    if (templateName.equals(itemValue) || templateName.equals(select.getItemLabel())) {
                        document.getProperties().put(DocumentSpecificModel.Props.TEMPLATE_NAME.toString(), itemValue);
                        templateList.setValue(itemValue);
                        break children;
                    }
                }
                break; // There is only one template field
            }

        }
    }

    public SelectItem[] getUserOrContactSearchFilters() {
        return contactOrUserSearchFilters;
    }

    public SelectItem[] searchUsersOrContacts(int filterIndex, String contains) {
        log.debug("executeOwnerSearch: " + filterIndex + ", " + contains);
        if (filterIndex == 0) { // users
            return userListDialog.searchUsers(-1, contains);
        } else if (filterIndex == 1) { // contacts
            final String personLabel = MessageUtil.getMessage("addressbook_private_person").toLowerCase();
            final String organizationLabel = MessageUtil.getMessage("addressbook_org").toLowerCase();
            List<Node> nodes = getAddressbookService().search(contains);
            return AddressbookMainViewDialog.transformNodesToSelectItems(nodes, personLabel, organizationLabel);
        } else {
            throw new RuntimeException("Unknown filter index value: " + filterIndex);
        }
    }

    public boolean isShowCase() {
        if (document != null) {
            return document.getProperties().get(TransientProps.CASE_NODEREF) != null;
        }
        return false;
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
            String row = joinStringAndStringWithComma(encode(name), generateEmailLink(email));
            if (!StringUtils.isBlank(row)) {
                rows.add(row);
            }
        }
        return StringUtils.join(rows.iterator(), "<br/>");
    }

    protected String generateCoFinancerTable(List<String> institutions, List<String> persons, List<Double> sums) {
        DoubleCurrencyConverter converter = new DoubleCurrencyConverter();
        int size = 0;
        if (institutions != null) {
            size = institutions.size();
        } else if (persons != null) {
            size = persons.size();
        } else if (sums != null) {
            size = sums.size();
        }
        List<String> rows = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            String institution = null;
            if (institutions != null && i < institutions.size()) {
                institution = institutions.get(i);
            }
            String person = null;
            if (persons != null && i < persons.size()) {
                person = persons.get(i);
            }
            String sum = "";
            if (sums != null && i < sums.size()) {
                if (sums.get(i) != null) {
                    sum = converter.getAsString(sums.get(i));
                }
            }
            String row = joinStringAndStringWithComma(encode(institution), encode(person));
            if (StringUtils.isNotBlank(row) || StringUtils.isNotBlank(sum)) {
                row += " summas " + sum;
                if (!StringUtils.isBlank(row)) {
                    rows.add(row);
                }
            }
        }
        return StringUtils.join(rows.iterator(), "<br/>");
    }

    protected String generateEmailLink(String email) {
        if (StringUtils.isBlank(email)) {
            return email;
        }
        return "<a href=\"mailto:" + encode(email) + "\">" + encode(email) + "</a>";
    }

    protected String joinDateAndStringWithComma(Date date, String value2) {
        String value1 = getDateString(date);
        return joinStringAndStringWithComma(value1, value2);
    }

    protected String joinStringAndDateWithComma(String value1, Date date) {
        String value2 = getDateString(date);
        return joinStringAndStringWithComma(value1, value2);
    }

    protected String joinStringAndDateWithSpace(String value1, Date date) {
        String value2 = getDateString(date);
        return joinStringAndStringWithSpace(value1, value2);
    }

    private String getDateString(Date date) {
        String value2 = "";
        if (date != null) {
            value2 = dateFormat.format(date);
        }
        return value2;
    }

    public void init(NodeRef nodeRef, boolean created, DocumentDialog documentDialog) {
        this.nodeRef = nodeRef;
        this.documentDialog = documentDialog;
        inEditMode = created;
        isDraft = created;
        propertySheet = null;
        reloadDoc();
        DocumentType documentType = getDocumentTypeService().getDocumentType(document.getType());
        documentTypeName = documentType != null ? documentType.getName() : null;
        BeanHelper.getClearStateNotificationHandler().addClearStateListener(this);
    }

    public void reloadDoc() {
        reloadDoc(true);
    }

    public void reloadDoc(boolean addInvoiceMessages) {
        document = getDocumentService().getDocument(nodeRef);
        if (!inEditMode) {// only create lock for existing doc
            lockOrUnlockIfNeeded(inEditMode);
        }
        afterModeChange();
        if (addInvoiceMessages) {
            addInvoiceMessages();
        }
    }

    public void reloadDocAndClearPropertySheet() {
        reloadDocAndClearPropertySheet(true);
    }

    public void reloadDocAndClearPropertySheet(boolean addInvoiceMessages) {
        reloadDoc(addInvoiceMessages);
        clearPropertySheet();
    }

    public void reset() {
        inEditMode = false;
        lockOrUnlockIfNeeded(false);
        document = null;
        propertySheet = null;
        documentTypeName = null;
    }

    public void saveAndRegister(boolean isDraft, List<NodeRef> newInvoiceDocuments) {
        if (save(isDraft, newInvoiceDocuments, false)) {
            document.getProperties().put(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT, isDraft);
            EventsLoggingHelper.disableLogging(document, DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED);
            registerDocument(null);
            // We need to refresh the propertySheetgrid
            clearPropertySheet();
            afterModeChange();
        }
    }

    /**
     * @param event object from jsp
     */
    public void edit(ActionEvent event) {
        if (inEditMode) {
            throw new RuntimeException("Document metadata block is already in edit mode");
        }
        editDocument(getDocumentService().getDocument(document.getNodeRef()));
    }

    public void editNewDocument(Node docNode) {
        isDraft = true;
        editDocument(docNode);
    }

    public void editDocument(Node doc) {
        if (!doc.hasPermission(DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA)) {
            return;
        }
        document = doc;
        inEditMode = true;
        lockOrUnlockIfNeeded(isLockingAllowed());
        propertySheet.setMode(getMode());
        clearPropertySheet();
        DocumentType documentType = getDocumentTypeService().getDocumentType(document.getType());
        documentTypeName = documentType != null ? documentType.getName() : null;
        afterModeChange();
        addInvoiceMessages();
        documentDialog.notifyModeChanged();
    }

    public String editAction() {
        try {
            BaseDialogBean.validatePermission(document, DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA);
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
        }
        return null; // expecting that this action is already called when documentDialog is opened - so always stay on the same dialog
    }

    public void viewDocument(Node doc) {
        document = doc;
        inEditMode = false;
        propertySheet.setMode(getMode());
        clearPropertySheet();
        DocumentType documentType = getDocumentTypeService().getDocumentType(document.getType());
        documentTypeName = documentType != null ? documentType.getName() : null;
        afterModeChange();
        addInvoiceMessages();
        documentDialog.notifyModeChanged();
    }

    public boolean save(boolean isDraft, List<NodeRef> newInvoiveDocuments) {
        return save(isDraft, newInvoiveDocuments, true);
    }

    public boolean save(boolean isDraft, List<NodeRef> newInvoiveDocuments, boolean addInvoiceMessages) {
        log.debug("save: docNodeRef=" + document.getNodeRefAsString());
        if (!inEditMode) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }
        if (validate()) {
            removeEmptyParties();
            fillInvoiceData();
            try {
                log.debug("save: doc NodeRef=" + document.getNodeRefAsString());
                document.getProperties().put(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT, isDraft);
                if (DocumentSubtypeModel.Types.INVOICE.equals(document.getType())) {
                    document.getProperties().putAll(EInvoiceUtil.getTransSearchableProperties(BeanHelper.getEInvoiceService().getInvoiceTransactions(nodeRef)));
                }
                document = getDocumentService().updateDocument(document);
                NodeRef volume = (NodeRef) document.getProperties().get(TransientProps.VOLUME_NODEREF);
                NodeRef series = (NodeRef) document.getProperties().get(TransientProps.SERIES_NODEREF);
                NodeRef function = (NodeRef) document.getProperties().get(TransientProps.FUNCTION_NODEREF);
                for (NodeRef invoiceRef : newInvoiveDocuments) {
                    Node invoice = documentService.getDocument(invoiceRef);
                    invoice.getProperties().put(TransientProps.VOLUME_NODEREF, volume);
                    invoice.getProperties().put(TransientProps.SERIES_NODEREF, series);
                    invoice.getProperties().put(TransientProps.FUNCTION_NODEREF, function);
                    getDocumentService().updateDocument(invoice);
                }
                if (!isDraft && getWorkflowService().isSendableExternalWorkflowDoc(document.getNodeRef())) {
                    getDvkService().sendDvkTasksWithDocument(document.getNodeRef(), null, null);
                }
                inEditMode = false;
            } catch (UnableToPerformException e) {
                if (log.isDebugEnabled()) {
                    log.warn("failed to save: " + e.getMessage());
                }
                MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
                return false;
            } catch (ExternalReviewException e) {
                MessageUtil.addInfoMessage("dvk_sending_failed");
            } finally {
                lockOrUnlockIfNeeded(isLockingAllowed());
                reloadTransientProperties();
            }
            propertySheet.setMode(getMode());
            this.isDraft = false;
            clearPropertySheet();
            afterModeChange();
            if (addInvoiceMessages) {
                addInvoiceMessages();
            }
            MessageUtil.addInfoMessage("save_success");
            return true;
        }
        return false;
    }

    private void fillInvoiceData() {
        if (DocumentSubtypeModel.Types.INVOICE.equals(document.getType())) {
            // calculate invoice sums
            Map<String, Object> docProps = document.getProperties();
            Double totalSum = (Double) docProps.get(DocumentSpecificModel.Props.TOTAL_SUM);
            Double vat = (Double) docProps.get(DocumentSpecificModel.Props.VAT);
            if (vat == null) {
                vat = new Double(0);
            }
            if (totalSum != null) {
                BigDecimal sumWithoutVat = BigDecimal.valueOf(totalSum).subtract(BigDecimal.valueOf(vat));
                docProps.put(DocumentSpecificModel.Props.INVOICE_SUM.toString(), sumWithoutVat.doubleValue());
            }
            // fill (or empty) contact sap account
            String contactRegNumber = (String) docProps.get(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER);
            List<Node> contacts = BeanHelper.getAddressbookService().getContactsByRegNumber(contactRegNumber);
            if (contacts.size() == 1) {
                String contactSapAccount = (String) contacts.get(0).getProperties().get(AddressbookModel.Props.SAP_ACCOUNT);
                docProps.put(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT.toString(), contactSapAccount);
            } else {
                docProps.put(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT.toString(), null);
            }
        }
    }

    private void removeEmptyParties() {
        if (document.getType().equals(DocumentSubtypeModel.Types.CONTRACT_MV)) {
            List<Node> parties = document.getAllChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_MV_PARTIES);
            ArrayList<Node> emptyParties = new ArrayList<Node>();
            for (Node party : parties) {
                @SuppressWarnings("rawtypes")
                Map partyProps = party.getProperties();
                if (StringUtils.isBlank((String) partyProps.get(DocumentSpecificModel.Props.PARTY_NAME))
                        && StringUtils.isBlank((String) partyProps.get(DocumentSpecificModel.Props.PARTY_EMAIL))
                        && StringUtils.isBlank((String) partyProps.get(DocumentSpecificModel.Props.PARTY_SIGNER))
                        && StringUtils.isBlank((String) partyProps.get(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON))) {
                    emptyParties.add(party);
                }
            }
            if (emptyParties.size() > 0) {
                inMemoryChildNodeHelper.removeParties(document, emptyParties);
            }
        }
    }

    public void clearPropertySheet() {
        propertySheet.getChildren().clear();
        propertySheet.getClientValidations().clear();
        addEntrySapDateAndNumber(document.getProperties());
    }

    private boolean validate() throws ValidatorException {
        final Map<String, Object> props = document.getProperties();
        NodeRef functionRef = (NodeRef) props.get(TransientProps.FUNCTION_NODEREF);
        NodeRef seriesRef = (NodeRef) props.get(TransientProps.SERIES_NODEREF);
        NodeRef volumeRef = (NodeRef) props.get(TransientProps.VOLUME_NODEREF);
        if (functionRef == null || seriesRef == null || volumeRef == null) {
            if (log.isDebugEnabled()) {
                log.warn("validation failed: document_validationMsg_mandatory_functionSeriesVolume");
            }
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_validationMsg_mandatory_functionSeriesVolume");
            return false;
        }

        final List<String> messages = new ArrayList<String>(4);
        Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);

        String caseLabel = StringUtils.trimToNull((String) props.get(TransientProps.CASE_LABEL_EDITABLE));
        if (volume.isContainsCases() && StringUtils.isBlank(caseLabel)) {
            // client-side validation prevents it; but it reaches here if in-between rendering and submitting, someone else changes volume's containsCases=true
            if (log.isDebugEnabled()) {
                log.warn("validation failed: document_validationMsg_mandatory_case");
            }
            messages.add("document_validationMsg_mandatory_case");
        } else if (!volume.isContainsCases() && StringUtils.isNotBlank(caseLabel)) {
            caseLabel = null;
        }
        Case docCase = null;
        if (volume.isContainsCases() && StringUtils.isNotBlank(caseLabel)) {
            List<Case> allCases = getCaseService().getAllCasesByVolume(volumeRef);
            NodeRef caseRef = null;
            for (Case tmpCase : allCases) {
                if (StringUtils.equalsIgnoreCase(caseLabel, tmpCase.getTitle())) {
                    caseRef = tmpCase.getNode().getNodeRef();
                    docCase = tmpCase;
                    break;
                }
            }
            props.put(TransientProps.CASE_NODEREF, caseRef);
        }

        boolean isClosedUnitCheckNeeded = isClosedUnitCheckNeeded(getDocumentService().getAncestorNodesByDocument(nodeRef), volumeRef, docCase);

        if (isClosedUnitCheckNeeded && DocListUnitStatus.CLOSED.equals(getFunctionsService().getFunctionByNodeRef(functionRef).getStatus())) {
            messages.add("document_validationMsg_closed_function");
        }
        if (isClosedUnitCheckNeeded && DocListUnitStatus.CLOSED.equals(getSeriesService().getSeriesByNodeRef(seriesRef).getStatus())) {
            messages.add("document_validationMsg_closed_series");
        }
        if (isClosedUnitCheckNeeded && DocListUnitStatus.CLOSED.equals(volume.getStatus())) {
            messages.add("document_validationMsg_closed_volume");
        }
        if (isClosedUnitCheckNeeded && docCase != null && docCase.isClosed()) {
            if (log.isDebugEnabled()) {
                log.warn("validation failed: document_validationMsg_closed_case");
            }
            messages.add("document_validationMsg_closed_case");
        }

        props.put(TransientProps.CASE_LABEL_EDITABLE, caseLabel);

        if (document.getType().equals(DocumentSubtypeModel.Types.CONTRACT_MV)) {
            List<Node> parties = document.getAllChildAssociations(DocumentSpecificModel.Assocs.CONTRACT_MV_PARTIES);
            boolean hasValidPart = false;
            for (Node party : parties) {
                if (StringUtils.isNotBlank(((String) party.getProperties().get(DocumentSpecificModel.Props.PARTY_NAME)))) {
                    hasValidPart = true;
                    break;
                }
            }
            if (!hasValidPart) {
                messages.add("document_validationMsg_mandatory_party");
            }
        }

        validateErrandAbroadDailyCatering(messages);
        validateDailyAllowanceV2(messages);
        validateExpensesV2TotalSum(messages);

        if (DocumentSubtypeModel.Types.INVOICE.equals(document.getType())) {
            String paymentRefNumber = (String) props.get(DocumentSpecificModel.Props.PAYMENT_REFERENCE_NUMBER);
            if (paymentRefNumber != null && !StringUtils.isNumeric(paymentRefNumber)) {
                messages.add("document_errorMsg_payment_ref_number_not_numeric");
            }
            Date entryDate = (Date) props.get(DocumentSpecificModel.Props.ENTRY_DATE);
            if (entryDate != null) {
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTime(entryDate);
                int entryYear = calendar.get(Calendar.YEAR);
                if (currentYear != entryYear && Boolean.parseBoolean(BeanHelper.getParametersService().getStringParameter(Parameters.INVOICE_ENTRY_DATE_ONLY_IN_CURRENT_YEAR))) {
                    messages.add("document_errorMsg_entryDate_current_year");
                } else if (currentYear != entryYear && currentYear - 1 != entryYear) {
                    messages.add("document_errorMsg_entryDate_current_or_previous_year");
                }
            }
        }

        if (messages.size() > 0) {
            for (String message : messages) {
                if (log.isDebugEnabled()) {
                    log.warn("validation failed: " + message);
                }
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), message);
            }
            return false;
        }
        return true;
    }

    public boolean isClosedUnitCheckNeeded(DocumentParentNodesVO parents, NodeRef volumeRef, Case docCase) {
        return isDraft
                || !(volumeRef.equals(parents.getVolumeNode().getNodeRef())
                     && (parents.getCaseNode() == null ? docCase == null
                             : (docCase == null ? false
                                     : parents.getCaseNode().getNodeRef().equals(docCase.getNode().getNodeRef())
                              )
                         )
                     );
    }

    private void validateExpensesV2TotalSum(List<String> messages) {
        List<Node> expensesV2Nodes = new ArrayList<Node>();
        if (document.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION)) {
            expensesV2Nodes = document.getAllChildAssociations(DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS);
        } else if (document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2)) {
            List<Node> applicants = document.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2);
            for (Node applicant : applicants) {
                expensesV2Nodes.addAll(applicant.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2));
            }
        } else if (document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC_V2)) {
            List<Node> applicants = document.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2);
            for (Node applicant : applicants) {
                expensesV2Nodes.addAll(applicant.getAllChildAssociations(DocumentSpecificModel.Assocs.ERRAND_DOMESTIC_V2));
            }
        } else {
            return;
        }

        for (Node expensesV2Node : expensesV2Nodes) {
            if (!expensesV2Node.hasAspect(DocumentSpecificModel.Aspects.EXPENSES_V2)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            final List<Serializable> sums = (List<Serializable>) expensesV2Node.getProperties().get(DocumentSpecificModel.Props.EXPECTED_EXPENSE_SUM);
            BigDecimal totalSum = new BigDecimal("0.0");
            if (sums == null) {
                expensesV2Node.getProperties().put(DocumentSpecificModel.Props.EXPENSES_TOTAL_SUM.toString(), totalSum.doubleValue());
                return;
            }

            for (Iterator<Serializable> iterator = sums.iterator(); iterator.hasNext();) {
                Serializable sum = iterator.next();
                if (sum instanceof String) {
                    if (StringUtils.isNotBlank((String) sum)) {
                        totalSum = totalSum.add(new BigDecimal((String) sum));
                        continue;
                    }
                    // Remove only when there are no errors and thus user cannot edit this row any more
                    if (messages.isEmpty()) {
                        iterator.remove();
                    }
                } else if (sum instanceof Double) {
                    totalSum = totalSum.add(BigDecimal.valueOf((Double) sum));
                }
            }
            expensesV2Node.getProperties().put(DocumentSpecificModel.Props.EXPENSES_TOTAL_SUM.toString(), totalSum.doubleValue());
        }
    }

    private void validateDailyAllowanceV2(List<String> messages) {
        if (!document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2)
                && !document.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2)) {
            return;
        }

        Parameters parameter;
        QName applicantAssoc;
        QName errandAssoc;
        final QName docType = document.getType();
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(docType)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2;
            errandAssoc = DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2;
            parameter = Parameters.ERRAND_ORDER_ABROAD_DAILY_ALLOWANCE_SUM;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(docType)) {
            applicantAssoc = DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2;
            errandAssoc = null;
            parameter = Parameters.TRAINING_APPLICATION_DAILY_ALLOWANCE_SUM;
        } else {
            throw new RuntimeException("Unimplemented dailyAllowanceV2 validation on document with type '" + document.getType() + "'");
        }

        final BigDecimal dailyAllowanceSum = new BigDecimal(parametersService.getDoubleParameter(parameter).toString());
        final List<Node> applicantNodes = document.getAllChildAssociations(applicantAssoc);
        for (Node applicant : applicantNodes) {
            // Training application applicant has dailyAllowanceV2
            if (applicant.hasAspect(DocumentSpecificModel.Aspects.DAILY_ALLOWANCE_V2)) {
                validateDailyAllowanceV2Internal(messages, dailyAllowanceSum, applicant.getProperties());
                continue;
            }

            // Abroad errand order applicant has child errands that have dailyAllowanceV2
            for (Node errandNode : applicant.getAllChildAssociations(errandAssoc)) {
                validateDailyAllowanceV2Internal(messages, dailyAllowanceSum, errandNode.getProperties());
            }
        }
    }

    // Verify that daily allowance periods sum equals total errand duration
    private void validateDailyAllowanceV2Internal(List<String> messages, final BigDecimal dailyAllowanceSum, final Map<String, Object> props) {
        Date errandBegin = (Date) props.get(DocumentSpecificModel.Props.ERRAND_BEGIN_DATE.toString());
        Date errandEnd = (Date) props.get(DocumentSpecificModel.Props.ERRAND_END_DATE.toString());
        int errandDurationInDays = (int) ((errandEnd.getTime() - errandBegin.getTime()) / (1000 * 60 * 60 * 24) + 1);
        @SuppressWarnings("unchecked")
        List<Integer> allowanceDays = getIntegerList((List<Serializable>) props.get(DocumentSpecificModel.Props.DAILY_ALLOWANCE_DAYS));
        if (allowanceDays == null || allowanceDays.isEmpty()) {
            messages.add("document_errandOrderAbroad_applicant_errand_validation_mandatory_cateringExists");
            return;
        }

        int totalAllowanceDays = 0;
        for (Integer days : allowanceDays) {
            totalAllowanceDays += days;
        }

        if (errandDurationInDays != totalAllowanceDays) {
            messages.add("document_errand_dailyAllowance_days_sum_match_totalDays");
            return;
        }

        // Calculate daily allowance sums and total daily allowance sum (don't trust JS)
        final int size = allowanceDays.size();
        List<Double> dailySums = new ArrayList<Double>(size);
        BigDecimal totalDailySum = new BigDecimal("0.0");
        @SuppressWarnings("unchecked")
        final List<Integer> rates = getIntegerList((List<Serializable>) props.get(DocumentSpecificModel.Props.DAILY_ALLOWANCE_RATE));

        for (int i = 0; i < size; i++) {
            // Multiply days by parameter value and the multiply by rate percent
            final BigDecimal dailySum = dailyAllowanceSum.multiply(BigDecimal.valueOf(allowanceDays.get(i))).multiply(BigDecimal.valueOf(rates.get(i) / 100.0))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            totalDailySum = totalDailySum.add(dailySum);
            dailySums.add(dailySum.doubleValue());
        }
        props.put(DocumentSpecificModel.Props.DAILY_ALLOWANCE_SUM.toString(), dailySums);
        props.put(DocumentSpecificModel.Props.DAILY_ALLOWANCE_TOTAL_SUM.toString(), totalDailySum.doubleValue());
    }

    private void validateErrandAbroadDailyCatering(List<String> messages) {
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType()) && document.hasAspect(DocumentSpecificModel.Aspects.DAILY_ALLOWANCE)) {
            QName applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD;
            QName errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD;
            final List<Node> applicantNodes = document.getAllChildAssociations(applicantAssoc);
            for (Node applicant : applicantNodes) {
                final List<Node> errandNodes = errandAssocType == null ? null : applicant.getAllChildAssociations(errandAssocType);
                for (Node errand : errandNodes) {
                    @SuppressWarnings("unchecked")
                    List<String> cateringCounts = (List<String>) errand.getProperties().get(DocumentSpecificModel.Props.DAILY_ALLOWANCE_CATERING_COUNT);
                    if (cateringCounts == null || cateringCounts.size() == 0) {
                        messages.add("document_errandOrderAbroad_applicant_errand_validation_mandatory_cateringExists");
                        return;
                    }
                }
            }
        }
    }

    private List<Integer> getIntegerList(List<Serializable> list) {
        List<Integer> intList = new ArrayList<Integer>(list.size());
        for (Serializable item : list) {
            if (item instanceof Integer) {
                intList.add((Integer) item);
            } else if (item instanceof String && StringUtils.isNotBlank((String) item)) {
                intList.add(Integer.parseInt((String) item));
            }
        }

        return intList;
    }

    public void cancel() {
        if (!inEditMode) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }
        document = getDocumentService().getDocument(document.getNodeRef());
        inEditMode = false;
        lockOrUnlockIfNeeded(isLockingAllowed());
        propertySheet.setMode(getMode());
        clearPropertySheet();
        reloadTransientProperties();
        afterModeChange();
        addInvoiceMessages();
    }

    public String getMode() {
        return inEditMode ? UIPropertySheet.EDIT_MODE : UIPropertySheet.VIEW_MODE;
    }

    private void reloadTransientProperties() {
        if (document == null) {
            return;
        }
        DocumentParentNodesVO parentNodes = getDocumentService().getAncestorNodesByDocument(document.getNodeRef());
        getDocumentService().setTransientProperties(document, parentNodes);
    }

    /** Web-client action */
    public void registerDocument(@SuppressWarnings("unused") ActionEvent event) {
        try {
            Node document = getDocumentDialogHelperBean().getNode();
            DocumentParentNodesVO parentNodes = getDocumentService().getAncestorNodesByDocument(document.getNodeRef());
            getDocumentService().setTransientProperties(document, parentNodes);
            BaseDialogBean.validatePermission(document, DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA);
            document = getDocumentService().registerDocument(document);
            getDocumentTemplateService().updateGeneratedFilesOnRegistration(document.getNodeRef());
            ((MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME)).processTaskItems();
            MessageUtil.addInfoMessage("document_registerDoc_success");
        } catch (UnableToPerformException e) {
            if (log.isDebugEnabled()) {
                log.warn("failed to register: " + e.getMessage());
            }
            MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
        } catch (NodeLockedException e) {
            documentDialog.handleLockedNode("document_registerDoc_error_docLocked");
        }
        getDocumentDialogHelperBean().switchMode(false);
    }

    public void setCaseAssignmentNeeded(boolean showModal) {
        // shouldn't be set
    }

    /**
     * @return how often (in seconds) clients should call {@link #refreshLockClientHandler()} to refresh lock
     */
    public int getClientLockRefreshFrequency() {
        if (lockRefreshTimeout == null) {
            lockRefreshTimeout = getDocLockService().getLockTimeout() / 2;
        }
        return lockRefreshTimeout;
    }

    /**
     * AJAX: Extend lock on document (or create one)
     */
    public void refreshLockClientHandler() throws IOException {
        boolean lockSuccessfullyRefreshed = false;
        String errMsg = null;
        if (document == null) {
            errMsg = "Form is reset";
        } else {
            synchronized (document) { // to avoid extending lock after unlock(save/cancel)
                boolean lockingAllowed = isLockingAllowed();
                if (lockingAllowed) {
                    lockSuccessfullyRefreshed = lockOrUnlockIfNeeded(lockingAllowed);
                } else {
                    errMsg = "Can't refresh lock - page not in editMode";
                    log.warn(errMsg);
                }
            }
        }
        FacesContext context = FacesContext.getCurrentInstance();
        ResponseWriter out = context.getResponseWriter();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>");
        xml.append("<refresh-lock success=\"" + lockSuccessfullyRefreshed + "\"");
        xml.append(" nextReqInMs=\"" + getClientLockRefreshFrequency() * 1000 + "\"");
        if (errMsg != null) {
            xml.append(" errMsg=\"" + errMsg + "\"");
        }
        xml.append(" />");
        out.write(xml.toString());
        log.debug("returning XML: " + xml.toString());
    }

    /**
     * @param mustLock4Edit
     * @return true if current user holds the lock after execution of this function
     */
    public boolean lockOrUnlockIfNeeded(boolean mustLock4Edit) {
        if (document == null) {
            return false;
        }
        final DocLockService lockService = getDocLockService();
        final NodeRef docRef = document.getNodeRef();
        synchronized (document) { // to avoid extending lock after unlock(save/cancel)
            if (mustLock4Edit) {
                if (lockService.setLockIfFree(docRef) == LockStatus.LOCK_OWNER) {
                    return true;
                }
                log.debug("Lock can't be created");
                if (log.isDebugEnabled()) {
                    log.warn("failed to lock: document_validation_alreadyLocked");
                }
                MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "document_validation_alreadyLocked",
                        getUserService().getUserFullName((String) getNodeService().getProperty(docRef, ContentModel.PROP_LOCK_OWNER)));
                inEditMode = false; // don't allow going to editMode
                return false;
            }
            lockService.unlockIfOwner(docRef);
        }
        return false;
    }

    public DocumentType getDocumentType() {
        return getDocumentTypeService().getDocumentType(document.getType());
    }

    /**
     * Called when a new (not yet saved) document is set to be a reply or a follow up to some base document
     * and is filled with some properties of the base document.
     * 
     * @param nodeRef to the base document
     */
    public void updateFollowUpOrReplyProperties(NodeRef nodeRef) {
        setOwnerCurrentUser();
        setDocumentName(nodeRef);
        getDocumentService().setTransientProperties(document, getDocumentService().getAncestorNodesByDocument(nodeRef));
        { // make sure that not only properties get updated, but also visual components
            final NodeRef funRef = (NodeRef) document.getProperties().get(TransientProps.FUNCTION_NODEREF);
            final NodeRef seriesRef = (NodeRef) document.getProperties().get(TransientProps.SERIES_NODEREF);
            final NodeRef volumeRef = (NodeRef) document.getProperties().get(TransientProps.VOLUME_NODEREF);
            final String caseLabel = (String) document.getProperties().get(TransientProps.CASE_LABEL);
            updateFnSerVol(funRef, seriesRef, volumeRef, caseLabel, true);
        }
        final Map<String, Object> docProps = document.getProperties();
        updateAccessRestrictionProperties((NodeRef) docProps.get(TransientProps.SERIES_NODEREF));
    }

    private void setDocumentName(NodeRef nodeRef) {
        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentCommonModel.Props.DOC_NAME.toString(), getNodeService().getProperty(nodeRef, DocumentCommonModel.Props.DOC_NAME));
    }

    // START: snapshot logic
    public Snapshot createSnapshot() {
        return new Snapshot(this);
    }

    public void restoreSnapshot(Snapshot snapshot) {
        snapshot.restoreState(this);
    }

    public static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Node document;
        private final boolean inEditMode;
        private final boolean isDraft;
        private final DateFormat dateFormat;
        private final String documentTypeName;

        private Snapshot(MetadataBlockBean bean) {
            document = bean.document;
            inEditMode = bean.inEditMode;
            isDraft = bean.isDraft;
            dateFormat = bean.dateFormat;
            documentTypeName = bean.documentTypeName;
        }

        private void restoreState(MetadataBlockBean bean) {
            bean.document = document;
            bean.inEditMode = inEditMode;
            bean.isDraft = isDraft;
            bean.dateFormat = dateFormat;
            bean.documentTypeName = documentTypeName;
        }
    }

    // END: snapshot logic

    // START: getters / setters

    public GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    public DocLockService getDocLockService() {
        if (docLockService == null) {
            docLockService = (DocLockService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocLockService.BEAN_NAME);
        }
        return docLockService;
    }

    public String getDocumentTypeName() {
        return documentTypeName;
    }

    public Node getDocument() {
        return document;
    }

    public boolean isInEditMode() {
        return inEditMode;
    }

    /**
     * Returns true if required conditions are met for locking.
     * a) document is in edit mode
     * OR
     * b) current document is opened in send out dialog
     * 
     * @return true if we can lock, false otherwise.
     */
    public boolean isLockingAllowed() {
        boolean allowed = isInEditMode();
        DocumentSendOutDialog sendOut = null;
        if (document != null && (sendOut = BeanHelper.getDocumentSendOutDialog()) != null && sendOut.getModel() != null) {
            allowed |= document.getNodeRef().equals(sendOut.getModel().getNodeRef());
        }
        return allowed;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public boolean isShowPaymentAnnotation() {
        if (document == null || document.getProperties().get(DocumentSpecificModel.Props.PAYMENT_ANNOTATION) == null) {
            return false;
        }

        return true;
    }

    public boolean isShowStorageType() {
        if (document == null) {
            return false;
        }
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType()) //
                || DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType()) //
                || DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())
                || DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(document.getType())
                || DocumentSubtypeModel.Types.PERSONAL_VEHICLE_USAGE_COMPENSATION_MV.equals(document.getType())
                || DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV.equals(document.getType())) {
            return false; // value of StorageType should always be DIGITAL
        }
        return true;
    }

    /**
     * NB! Don't call this method from java code; this is meant ONLY for metadata-block.jsp binding.
     * For code use getPropertySheetInner() instead
     */
    public UIPropertySheet getPropertySheet() {
        propertySheetControlDocument = document;
        return propertySheet;
    }

    public UIPropertySheet getPropertySheetInner() {
        return propertySheet;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        if (propertySheetControlDocument != null && !propertySheetControlDocument.equals(document)) {
            propertySheet.getChildren().clear();
            propertySheetControlDocument = document;
        }
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

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    protected ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ParametersService.BEAN_NAME);
        }
        return parametersService;
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

    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    }

    protected WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    protected AddressbookService getAddressbookService() {
        if (addressbookService == null) {
            addressbookService = (AddressbookService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    AddressbookService.BEAN_NAME);
        }
        return addressbookService;
    }

    protected DvkService getDvkService() {
        if (dvkService == null) {
            dvkService = (DvkService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    DvkService.BEAN_NAME);
        }
        return dvkService;
    }

    protected ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }

    public void setUserListDialog(UserListDialog userListDialog) {
        this.userListDialog = userListDialog;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME);
        }
        return userService;
    }

    protected InMemoryChildNodeHelper getInMemoryChildNodeHelper() {
        if (inMemoryChildNodeHelper == null) {
            inMemoryChildNodeHelper = (InMemoryChildNodeHelper) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(InMemoryChildNodeHelper.BEAN_NAME);
        }
        return inMemoryChildNodeHelper;
    }

    // END: getters / setters

    @Override
    public void clearState() {
        // When user has locked the document and click on a menu link this is called and the lock is freed.
        reset();
    }
}