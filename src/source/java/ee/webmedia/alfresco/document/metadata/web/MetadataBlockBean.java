package ee.webmedia.alfresco.document.metadata.web;

import static org.alfresco.web.ui.common.StringUtils.encode;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
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
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocLockService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.EventsLoggingHelper;
import ee.webmedia.alfresco.document.service.InMemoryChildNodeHelper;
import ee.webmedia.alfresco.document.service.DocumentService.TransientProps;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
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
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Alar Kvell
 */
public class MetadataBlockBean implements Serializable {
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
    private transient UIPropertySheet propertySheet;
    private transient InMemoryChildNodeHelper inMemoryChildNodeHelper;

    private Node document;
    private boolean inEditMode;
    private DateFormat dateFormat;
    private String documentTypeName;

    /** timeOut in seconds how often lock should be refreshed to avoid expiring */
    private Integer lockRefreshTimeout;
    private NodeRef nodeRef;

    public MetadataBlockBean() {
        String datePattern = Application.getMessage(FacesContext.getCurrentInstance(), "date_pattern");
        dateFormat = new SimpleDateFormat(datePattern);
    }

    public String getLeavingLetterContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"message\">");
        sb.append(getParametersService().getStringParameter(Parameters.LEAVING_LETTER_CONTENT));
        sb.append("</div>");
        return sb.toString();
    }

    public void setApplicantName(String userName, Node applicantNode) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> props = applicantNode.getProperties();
        props.put(DocumentSpecificModel.Props.APPLICANT_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        props.put(DocumentSpecificModel.Props.APPLICANT_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        props.put(DocumentSpecificModel.Props.APPLICANT_STRUCT_UNIT_NAME.toString(), orgstructName);
    }

    public void setErrandSubstituteName(String userName, Node applicantNode) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = applicantNode.getProperties();
        docProps.put(DocumentSpecificModel.Props.ERRAND_SUBSTITUTE_NAME.toString(), UserUtil.getPersonFullName1(personProps));
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

    public void setDeliverer(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        Map<String, Object> docProps = document.getProperties();
        docProps.put(DocumentSpecificModel.Props.DELIVERER_NAME.toString(), UserUtil.getPersonFullName1(personProps));
        docProps.put(DocumentSpecificModel.Props.DELIVERER_JOB_TITLE.toString(), personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        docProps.put(DocumentSpecificModel.Props.DELIVERER_STRUCT_UNIT.toString(), orgstructName);
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

        String name = "";
        Map<QName, Serializable> props = getNodeService().getProperties(nodeRef);
        if (AddressbookModel.Types.ORGANIZATION.equals(getNodeService().getType(nodeRef))) {
            name = (String) props.get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else {
            name = UserUtil.getPersonFullName((String) props.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) props
                    .get(AddressbookModel.Props.PERSON_LAST_NAME));
        }

        document.getProperties().put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME.toString(), name);
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

    protected void afterModeChange() {
        Map<String, Object> props = document.getProperties();
        if (!inEditMode) {

            String comment = (String) props.get(DocumentCommonModel.Props.COMMENT);
            if (comment != null) {
                comment = StringUtils.replace(encode(comment), "\n", "<br/>");
            }
            props.put("{temp}comment", comment);

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
                props.put("owner", joinStringAndStringWithParentheses(owner, ownerDetails));
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
                String senderEmail = (String) props.get(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL);
                props.put("senderNameEmail", joinStringAndStringWithComma(senderName, senderEmail));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.WHOM)) {
                String whomName = (String) props.get(DocumentSpecificModel.Props.WHOM_NAME);
                String whomJobTitle = (String) props.get(DocumentSpecificModel.Props.WHOM_JOB_TITLE);
                props.put("whomNameAndJobTitle", joinStringAndStringWithParentheses(whomName, whomJobTitle));
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

            if (document.hasAspect(DocumentSpecificModel.Aspects.MANAGEMENTS_ORDER_DETAILS)) {
                String responsible = encode((String) props.get(DocumentSpecificModel.Props.RESPONSIBLE_NAME));
                String responsibleStructUnit = (String) props.get(DocumentSpecificModel.Props.RESPONSIBLE_STRUCT_UNIT);
                props.put("responsible", joinStringAndStringWithParentheses(responsible, responsibleStructUnit));
            }

            if (document.hasAspect(DocumentSpecificModel.Aspects.VACATION_ORDER)) {
                Boolean leaveAnnual = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL);
                Boolean leaveWithoutPay = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY);
                Boolean leaveChild = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CHILD);
                Boolean leaveStudy = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_STUDY);
                StringBuilder sb = new StringBuilder();

                FacesContext context = FacesContext.getCurrentInstance();
                if (BooleanUtils.isTrue(leaveAnnual)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_ANNUAL_DAYS));
                    String msg = document.getType().equals(DocumentSubtypeModel.Types.VACATION_ORDER_SMIT) //
                            ? "document_leaveAnnualSmit" : "document_leaveAnnual";
                    sb.append(encode(MessageUtil.getMessage(context, msg, from, to, days)));
                }
                if (BooleanUtils.isTrue(leaveWithoutPay)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_DAYS));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveWithoutPay", from, to, days)));
                }
                if (BooleanUtils.isTrue(leaveChild)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_DAYS));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveChild", from, to, days)));
                }
                if (BooleanUtils.isTrue(leaveStudy)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_DAYS));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveStudy", from, to, days)));
                }
                if (StringUtils.isNotBlank(sb.toString())) {
                    props.put("{temp}vacationAddText", sb.toString());
                }

                Boolean leaveChange = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CHANGE);
                Boolean leaveCancel = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL);
                sb = new StringBuilder();
                if (BooleanUtils.isTrue(leaveChange)) {
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveChange1")));
                    sb.append("<br/>");
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATE));
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveChange2", from, to)));
                    sb.append("<br/>");
                    String newFrom = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE));
                    String newTo = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_NEW_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_NEW_DAYS));
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveChange3", newFrom, newTo, days)));
                }
                if (BooleanUtils.isTrue(leaveCancel)) {
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveCancel1")));
                    sb.append("<br/>");
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_DAYS));
                    sb.append(encode(MessageUtil.getMessage(context, "document_leaveCancel2", from, to, days)));
                }
                if (StringUtils.isNotBlank(sb.toString())) {
                    props.put("{temp}vacationChangeText", sb.toString());
                }

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

        // empty default selection
        selectItems.add(new SelectItem("", ""));

        for (DocumentTemplate tmpl : docTemplates) {
            selectItems.add(new SelectItem(tmpl.getName(), FilenameUtils.removeExtension(tmpl.getName())));
        }
        WebUtil.sort(selectItems);
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
            UIPropertySheet ps = getPropertySheet();
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

    public boolean isShowCase() {
        return document.getProperties().get(TransientProps.CASE_NODEREF) != null;
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

    protected String generateEmailLink(String email) {
        if (StringUtils.isBlank(email)) {
            return email;
        }
        return "<a href=\"mailto:" + encode(email) + "\">" + encode(email) + "</a>";
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
        if (StringUtils.isNotBlank(value1)) {
            result += value1;
        }
        if (StringUtils.isNotBlank(value2)) {
            if (StringUtils.isNotBlank(result)) {
                result += ", ";
            }
            result += value2;
        }
        return result;
    }

    protected String joinStringAndStringWithParentheses(String value1, String value2) {
        String result = "";
        if (StringUtils.isNotBlank(value1)) {
            result += value1;
        }
        if (StringUtils.isNotBlank(value2)) {
            if (StringUtils.isNotBlank(result)) {
                result += " ";
            }
            result += "(" + value2 + ")";
        }
        return result;
    }

    public void init(NodeRef nodeRef, boolean created) {
        this.nodeRef = nodeRef;
        this.inEditMode = created;
        propertySheet = null;
        reloadDoc();
        DocumentType documentType = getDocumentTypeService().getDocumentType(document.getType());
        documentTypeName = documentType != null ? documentType.getName() : null;
    }

    public void reloadDoc() {
        document = getDocumentService().getDocument(nodeRef);
        if (!inEditMode) {// only create lock for existing doc
            lockOrUnlockIfNeeded(inEditMode);
        }
        afterModeChange();
    }

    public void reset() {
        inEditMode = false;
        lockOrUnlockIfNeeded(inEditMode);
        document = null;
        propertySheet = null;
        documentTypeName = null;
    }

    public void saveAndRegister(boolean isDraft) {
        if (save(isDraft)) {
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

    public void editDocument(Node doc) {
        document = doc;
        inEditMode = true;
        lockOrUnlockIfNeeded(inEditMode);
        propertySheet.setMode(getMode());
        clearPropertySheet();
        DocumentType documentType = getDocumentTypeService().getDocumentType(document.getType());
        documentTypeName = documentType != null ? documentType.getName() : null;
        afterModeChange();
    }

    public void viewDocument(Node doc) {
        document = doc;
        inEditMode = false;
        propertySheet.setMode(getMode());
        clearPropertySheet();
        DocumentType documentType = getDocumentTypeService().getDocumentType(document.getType());
        documentTypeName = documentType != null ? documentType.getName() : null;
        afterModeChange();
    }

    public boolean save(boolean isDraft) {
        log.debug("save: docNodeRef=" + document.getNodeRefAsString());
        if (!inEditMode) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }
        if (validate()) {
            try {
                log.debug("save: doc NodeRef=" + document.getNodeRefAsString());
                document.getProperties().put(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT, isDraft);
                document = getDocumentService().updateDocument(document);
                inEditMode = false;
            } catch (UnableToPerformException e) {
                if (log.isDebugEnabled()) {
                    log.warn("failed to save: " + e.getMessage());
                }
                MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
                return false;
            } finally {
                lockOrUnlockIfNeeded(inEditMode);
                reloadTransientProperties();
            }
            propertySheet.setMode(getMode());
            clearPropertySheet();
            afterModeChange();
            MessageUtil.addInfoMessage("save_success");
            return true;
        }
        return false;
    }

    public void clearPropertySheet() {
        propertySheet.getChildren().clear();
        propertySheet.getClientValidations().clear();
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
        if (DocListUnitStatus.CLOSED.equals(getFunctionsService().getFunctionByNodeRef(functionRef).getStatus())) {
            messages.add("document_validationMsg_closed_function");
        }
        if (DocListUnitStatus.CLOSED.equals(getSeriesService().getSeriesByNodeRef(seriesRef).getStatus())) {
            messages.add("document_validationMsg_closed_series");
        }
        Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);
        if (DocListUnitStatus.CLOSED.equals(volume.getStatus())) {
            messages.add("document_validationMsg_closed_volume");
        }

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
        if (volume.isContainsCases() && StringUtils.isNotBlank(caseLabel)) {
            List<Case> allCases = getCaseService().getAllCasesByVolume(volumeRef);
            NodeRef caseRef = null;
            for (Case tmpCase : allCases) {
                if (StringUtils.equalsIgnoreCase(caseLabel, tmpCase.getTitle())) {
                    caseRef = tmpCase.getNode().getNodeRef();
                    if (tmpCase.isClosed()) {
                        if (log.isDebugEnabled()) {
                            log.warn("validation failed: document_validationMsg_closed_case");
                        }
                        messages.add("document_validationMsg_closed_case");
                    }
                    break;
                }
            }
            props.put(TransientProps.CASE_NODEREF, caseRef);
        }
        props.put(TransientProps.CASE_LABEL_EDITABLE, caseLabel);

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

    public void cancel() {
        if (!inEditMode) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }
        document = getDocumentService().getDocument(document.getNodeRef());
        inEditMode = false;
        lockOrUnlockIfNeeded(inEditMode);
        propertySheet.setMode(getMode());
        clearPropertySheet();
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
        DocumentParentNodesVO parentNodes = getDocumentService().getAncestorNodesByDocument(document.getNodeRef());
        getDocumentService().setTransientProperties(document, parentNodes);
    }

    /** Web-client action */
    public void registerDocument(@SuppressWarnings("unused") ActionEvent event) {
        try {
            document = getDocumentService().registerDocument(document);
            nodeRef = document.getNodeRef(); // reloadDoc uses NodeRef
            getDocumentTemplateService().updateGeneratedFilesOnRegistration(document.getNodeRef());
            ((MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME)).processTaskItems();
            MessageUtil.addInfoMessage("document_registerDoc_success");
        } catch (UnableToPerformException e) {
            if (log.isDebugEnabled()) {
                log.warn("failed to register: " + e.getMessage());
            }
            MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
        } catch (NodeLockedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_registerDoc_error_docLocked");
        }
        reloadDoc();
    }

    public void setCaseAssignmentNeeded(@SuppressWarnings("unused") boolean showModal) {
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
                if (inEditMode) {
                    lockSuccessfullyRefreshed = lockOrUnlockIfNeeded(inEditMode);
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
    private boolean lockOrUnlockIfNeeded(boolean mustLock4Edit) {
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
                MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "document_validation_alreadyLocked");
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
            updateFnSerVol(funRef, seriesRef, volumeRef, caseLabel, false);
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

        private Node document;
        private boolean inEditMode;
        private DateFormat dateFormat;
        private String documentTypeName;

        private Snapshot(MetadataBlockBean bean) {
            this.document = bean.document;
            this.inEditMode = bean.inEditMode;
            this.dateFormat = bean.dateFormat;
            this.documentTypeName = bean.documentTypeName;
        }

        private void restoreState(MetadataBlockBean bean) {
            bean.document = this.document;
            bean.inEditMode = this.inEditMode;
            bean.dateFormat = this.dateFormat;
            bean.documentTypeName = this.documentTypeName;
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

    public boolean isShowStorageType() {
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType()) //
                || DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType()) //
                || DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())
                || DocumentSubtypeModel.Types.TENDERING_APPLICATION.equals(document.getType())) {
            return false; // value of StorageType should always be DIGITAL
        }
        return true;
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

    protected InMemoryChildNodeHelper getInMemoryChildNodeHelper() {
        if (inMemoryChildNodeHelper == null) {
            inMemoryChildNodeHelper = (InMemoryChildNodeHelper) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(InMemoryChildNodeHelper.BEAN_NAME);
        }
        return inMemoryChildNodeHelper;
    }

    // END: getters / setters
}
