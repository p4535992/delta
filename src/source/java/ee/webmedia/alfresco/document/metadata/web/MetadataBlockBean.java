package ee.webmedia.alfresco.document.metadata.web;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.LockStatus;
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
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.propertysheet.relateddropdown.RelatedDropdown;
import ee.webmedia.alfresco.common.propertysheet.relateddropdown.RelatedDropdownGenerator;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.SessionContext;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocLockService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.TransientProps;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.ActionUtil;
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
    private transient SessionContext sessionContext;
    private transient DocLockService docLockService;
    private transient GeneralService generalService;
    private transient DocumentTemplateService documentTemplateService;
    private transient ParametersService parametersService;
    private transient UIPropertySheet propertySheet;

    private transient HtmlInputTextarea newCaseHtmlInput;

    private Node document;
    private boolean inEditMode;
    private DateFormat dateFormat;
    private String documentTypeName;
    private String selectedCaseRefNum;
    private String originalVolumeRef;
    private String originalLocationId;

    /** timeOut in seconds how often lock should be refreshed to avoid expiring */
    private Integer lockRefreshTimeout;

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

    public List<String> setVacationSubstitute(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);

        List<String> list = new ArrayList<String>(3);
        list.add(UserUtil.getPersonFullName1(personProps));
        list.add(null);
        list.add(null);
        return list;
    }
    
    public List<String>  setProcurementApplicantName(String userName) {
        Map<QName, Serializable> personProps = getPersonProps(userName);
        List<String> list = new ArrayList<String>(3);
        list.add(UserUtil.getPersonFullName1(personProps));
        list.add((String) personProps.get(ContentModel.PROP_JOBTITLE));
        String orgstructName = getOrganizationStructureService().getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
        list.add(orgstructName);
        return list;
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

            if (document.hasAspect(DocumentSpecificModel.Aspects.COMPLIENCE)) {
                Date complienceDate = (Date) props.get(DocumentSpecificModel.Props.COMPLIENCE_DATE);
                if (complienceDate != null) {
                    props.put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
                    getNodeService().setProperty(document.getNodeRef(), DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.FINISHED.getValueName());
                }
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
                String responsible = Utils.encode((String) props.get(DocumentSpecificModel.Props.RESPONSIBLE_NAME));
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
                    String msg = document.getType().equals(DocumentSubtypeModel.Types.VACATION_ORDER_SMIT) ? "document_leaveAnnualSmit" : "document_leaveAnnual";
                    sb.append(Utils.encode(MessageUtil.getMessage(context, msg, from, to, days)));
                }
                if (BooleanUtils.isTrue(leaveWithoutPay)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_DAYS));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    sb.append(Utils.encode(MessageUtil.getMessage(context, "document_leaveWithoutPay", from, to, days)));
                }
                if (BooleanUtils.isTrue(leaveChild)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_CHILD_DAYS));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    sb.append(Utils.encode(MessageUtil.getMessage(context, "document_leaveChild", from, to, days)));
                }
                if (BooleanUtils.isTrue(leaveStudy)) {
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_STUDY_DAYS));
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    sb.append(Utils.encode(MessageUtil.getMessage(context, "document_leaveStudy", from, to, days)));
                }
                if (StringUtils.isNotBlank(sb.toString())) {
                    props.put("{temp}vacationAddText", sb.toString());
                }

                Boolean leaveChange = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CHANGE);
                Boolean leaveCancel = (Boolean) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL);
                sb = new StringBuilder();
                if (BooleanUtils.isTrue(leaveChange)) {
                    sb.append(Utils.encode(MessageUtil.getMessage(context, "document_leaveChange1")));
                    sb.append("<br/>");
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATE));
                    sb.append(Utils.encode(MessageUtil.getMessage(context, "document_leaveChange2", from, to)));
                    sb.append("<br/>");
                    String newFrom = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE));
                    String newTo = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_NEW_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_NEW_DAYS));
                    sb.append(Utils.encode(MessageUtil.getMessage(context, "document_leaveChange3", newFrom, newTo, days)));
                }
                if (BooleanUtils.isTrue(leaveCancel)) {
                    if (StringUtils.isNotBlank(sb.toString())) {
                        sb.append("<br/>");
                    }
                    sb.append(Utils.encode(MessageUtil.getMessage(context, "document_leaveCancel1")));
                    sb.append("<br/>");
                    String from = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE));
                    String to = formatDateOrEmpty((Date) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE));
                    String days = formatIntegerOrEmpty((Integer) props.get(DocumentSpecificModel.Props.LEAVE_CANCEL_DAYS));
                    sb.append(Utils.encode(MessageUtil.getMessage(context, "document_leaveCancel2", from, to, days)));
                }
                if (StringUtils.isNotBlank(sb.toString())) {
                    props.put("{temp}vacationChangeText", sb.toString());
                }
                
                StringBuilder table = new StringBuilder("<table cellspacing='0' cellpadding='0' class='recipient padding'><thead><tr><th>");
                table.append(MessageUtil.getMessage(context, "document_vacationSubstitute2"));
                table.append("</th><th>").append(MessageUtil.getMessage(context, "from"));
                table.append("</th><th>").append(MessageUtil.getMessage(context, "to"));
                table.append("</th></tr></thead><tbody>");
                @SuppressWarnings("unchecked")
                List<String> names = (List<String>) props.get(DocumentSpecificModel.Props.SUBSTITUTE_NAME);
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
                        table.append("<tr><td>").append(Utils.encode(name)).append("</td><td>");
                        table.append(Utils.encode(begin)).append("</td><td>");
                        table.append(Utils.encode(end)).append("</td></tr>");
                    }
                    table.append("</tbody></table>");
                    props.put("{temp}vacationSubstitute", table.toString());
                }
            }

        } else {
            /** in Edit mode */

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
    public void updateAccessRestrictionProperties(Object submittedValue) {
        final Map<String, Object> docProps = document.getProperties();
        final String accessRestriction = (String) docProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
        if (StringUtils.isBlank(accessRestriction)) {
            String sVal = (String) submittedValue;
            if (StringUtils.isBlank(sVal)) {
                return;
            }
            // read serAccessRestriction-related values from series
            final Series series = getSeriesService().getSeriesByNodeRef(sVal);
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
        final QName applicantAssoc = getApplicantAssoc();
        final Node docNode = getParentNode(event);
        final String assocIndexParam = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_ASSOC_INDEX);
        final int assocIndex = Integer.parseInt(assocIndexParam);
        docNode.removeChildAssociations(applicantAssoc, assocIndex);
    }

    /**
     * Add applicant childNode to errand(abroad/domestic) document 
     * @param event
     */
    public void addApplicant(ActionEvent event) {
        final Node docNode = getParentNode(event);
        final WmNode newApplicant = getGeneralService().createNewUnSaved(getApplicantType(), null);
        log.debug("adding new Applicant to document.\n\tdocNodeRef=" + docNode.getNodeRefAsString() //
                + "\n\tnewApplicantRef=" + newApplicant.getNodeRefAsString());
        docNode.addChildAssociations(getApplicantAssoc(), newApplicant);
        addErrand(newApplicant);
    }

    public void removeErrand(ActionEvent event) {
        final Node applicantNode = getParentNode(event);
        final String assocIndexParam = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_ASSOC_INDEX);
        final int assocIndex = Integer.parseInt(assocIndexParam);
        log.debug("removing errand (assocIndex=" + assocIndex + ") from applicant. ApplicantNodeRef=" + applicantNode.getNodeRefAsString());
        applicantNode.removeChildAssociations(getErrandAssocType(applicantNode), assocIndex);
    }

    public void addErrand(ActionEvent event) {
        final Node applicantNode = getParentNode(event);
        addErrand(applicantNode);
    }

    private void addErrand(final Node applicantNode) {
        final QName errandType = getErrandType(applicantNode);
        if(errandType == null) {
            return;
        }
        final WmNode newErrand = getGeneralService().createNewUnSaved(errandType, null);
        log.debug("adding new errand to applicant.\n\tapplicantNodeRef=" + applicantNode.getNodeRefAsString() //
                + "\n\tnewApplicantRef=" + newErrand.getNodeRefAsString());
        applicantNode.addChildAssociations(getErrandAssocType(applicantNode), newErrand);
    }

    private Node getParentNode(ActionEvent event) {
        final String nodeKeyInSessionContext = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_PARENT_PROP_SHEET_NODE);
        final SessionContext sessionContext = getSessionContext();
        return (Node) sessionContext.get(nodeKeyInSessionContext);
    }
    
    private QName getApplicantType() {
        final QName applicantType;
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType())) {
            applicantType = DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType())) {
            applicantType = DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())) {
            applicantType = DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE;
        } else {
            throw new RuntimeException("Unimplemented adding applicant to document with type '" + document.getType() + "'");
        }
        return applicantType;
    }

    private QName getApplicantAssoc() {
        final QName applicantAssoc;
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType())) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType())) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())) {
            applicantAssoc = DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS;
        } else {
            throw new RuntimeException("Unimplemented adding applicant to document with type '" + document.getType() + "'");
        }
        return applicantAssoc;
    }
    
    private QName getErrandAssocType(final Node applicantNode) {
        final QName errandAssocType;
        if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_DOMESTIC;
        } else {
            throw new RuntimeException("Unimplemented adding errand to applicant with type '" + applicantNode.getType() + "'");
        }
        return errandAssocType;
    }
    
    private QName getErrandType(final Node applicantNode) {
        final QName errandType;
        if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())) {
            return null; // trainingApplication document has applicant block, but not errand
        } else {
            throw new RuntimeException("Unimplemented adding errand to applicant with type '" + applicantNode.getType() + "'");
        }
        return errandType;
    }

    /**
     * Query callback method executed by the component generated by {@link RelatedDropdown} or {@link RelatedDropdownGenerator}.
     * This method is part of the contract to the {@link RelatedDropdownGenerator}, it is up to the backing bean
     * to execute whatever query is appropriate and populate <code>selectComponent</code> with selection items.<br>
     * 
     * @param context - FacesContext for creating selection items
     * @param selectComponent - selectComponent that will be rendered(use <code>selectComponent.getChildren()</code> to add selection items)
     * @param submittedValue - value submitted by the previous {@link RelatedDropdown} (RelatedDropdown, that belongs to the same group and has order equal to
     *            order of given <code>selectComponent.order -1 </code>) in the same group.
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public void findAllFunctions(FacesContext context, HtmlSelectOneMenu selectComponent, Object submittedValue) {
        List<Function> functions = getFunctionsService().getAllFunctions(DocListUnitStatus.OPEN);
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();
        selectOptions.clear();

        /** Handle filled in data */
        Map<String, Object> props = document.getProperties();
        String nodeRef = (String) props.get(TransientProps.FUNCTION_NODEREF);
        Function filled = null;
        if (StringUtils.isNotBlank(nodeRef)) {
            filled = getFunctionsService().getFunctionByNodeRef(nodeRef);
            functions.add(0, filled);
        }

        for (Function function : functions) {
            // skip the duplicate (when filled is open),
            if (function != filled && function.getNodeRef().toString().equals(nodeRef)) {
                continue;
            }
            // don't check if filled is closed
            if (function != filled || DocListUnitStatus.OPEN.equals(function.getStatus())) {
                List<Series> allSeries = getSeriesService().getAllSeriesByFunction(function.getNodeRef(), DocListUnitStatus.OPEN, document.getType());
                if (allSeries.size() == 0) {
                    continue;
                }
            }
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(function.getMark() + " " + function.getTitle());
            selectItem.setItemValue(function.getNodeRef().toString());
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
     * @param submittedValue - value submitted by the previous {@link RelatedDropdown} (RelatedDropdown, that belongs to the same group and has order equal to
     *            order of given <code>selectComponent.order -1 </code>) in the same group.
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public void findAllSeries(FacesContext context, HtmlSelectOneMenu selectComponent, Object submittedValue) {
        String sVal = (String) submittedValue;
        if (StringUtils.isBlank(sVal)) {
            return;
        }
        final NodeRef functionNodeRef = new NodeRef(sVal);
        List<Series> allSeries = getSeriesService().getAllSeriesByFunction(functionNodeRef, DocListUnitStatus.OPEN, document.getType());
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();
        selectOptions.clear();

        /** Handle filled in data */
        Map<String, Object> props = document.getProperties();
        String nodeRef = (String) props.get(TransientProps.SERIES_NODEREF);
        Series filled = null;
        if (StringUtils.isNotBlank(nodeRef)) {
            filled = getSeriesService().getSeriesByNodeRef(nodeRef);
            allSeries.add(0, filled);
        }

        for (Series series : allSeries) {
            // skip the duplicate (when filled is open),
            if (series != filled && series.getNode().getNodeRefAsString().equals(nodeRef)) {
                continue;
            }
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
     * @param submittedValue - value submitted by the previous {@link RelatedDropdown} (RelatedDropdown, that belongs to the same group and has order equal to
     *            order of given <code>selectComponent.order -1 </code>) in the same group.
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public void findAllVolumes(FacesContext context, HtmlSelectOneMenu selectComponent, Object submittedValue) {
        String sVal = (String) submittedValue;
        if (StringUtils.isBlank(sVal)) {
            return;
        }
        final NodeRef seriesNodeRef = new NodeRef(sVal);
        List<Volume> volumes = getVolumeService().getAllValidVolumesBySeries(seriesNodeRef);
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();
        selectOptions.clear();

        /** Handle filled in data */
        Map<String, Object> props = document.getProperties();
        String nodeRef = (String) props.get(TransientProps.VOLUME_NODEREF);
        Volume filled = null;
        if (StringUtils.isNotBlank(nodeRef)) {
            filled = getVolumeService().getVolumeByNodeRef(nodeRef);
            volumes.add(0, filled);
        }

        for (Volume volume : volumes) {
            // skip the duplicate (when filled is open),
            if (volume != filled && volume.getNode().getNodeRefAsString().equals(nodeRef)) {
                continue;
            }
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(volume.getVolumeMark() + " " + volume.getTitle());
            selectItem.setItemValue(volume.getNode().getNodeRef().toString());
            selectOptions.add(selectItem);
        }
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
        newCaseHtmlInput = null;// this field might have been reinitialized by browser, when last action before creating new doc was saving under new case
        document = getDocumentService().getDocument(nodeRef);
        inEditMode = created;
        if (!created) {// only create lock for existing doc
            lockOrUnlockIfNeeded(inEditMode);
        }
        propertySheet = null;
        DocumentType documentType = getDocumentTypeService().getDocumentType(document.getType());
        documentTypeName = documentType != null ? documentType.getName() : null;
        afterModeChange();
        final Map<String, Object> props = document.getProperties();
        originalVolumeRef = (String) props.get(TransientProps.VOLUME_NODEREF);
        originalLocationId = getLocationId(props);
    }

    private String getLocationId(final Map<String, Object> props) {
        return (String) props.get(TransientProps.FUNCTION_NODEREF) + (String) props.get(TransientProps.SERIES_NODEREF) + (String) props.get(TransientProps.VOLUME_NODEREF);
    }

    public void reset() {
        inEditMode = false;
        lockOrUnlockIfNeeded(inEditMode);
        document = null;
        propertySheet = null;
        documentTypeName = null;
        selectedCaseRefNum = null;
        originalVolumeRef = null;
        originalLocationId = null;
        newCaseHtmlInput = null;
    }

    public void saveAndRegister() {
        save();
        registerDocument(null);
        // We need to refresh the propertySheetgrid
        clearPropertySheet();
        afterModeChange();
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

    public void save() {
        if (!inEditMode) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }
        if (validate()) {
            createOrSelectCase();
            try {
                document = getDocumentService().updateDocument(document);
                inEditMode = false;
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
            }
            lockOrUnlockIfNeeded(inEditMode);
            reloadTransientProperties();
            propertySheet.setMode(getMode());
            clearPropertySheet();
            afterModeChange();
            final Map<String, Object> props = document.getProperties();
            originalVolumeRef = (String) props.get(TransientProps.VOLUME_NODEREF); // user might not leave view, but return and want to save again
        }
    }

    public void clearPropertySheet() {
        propertySheet.getChildren().clear();
        propertySheet.getClientValidations().clear();
    }

    private boolean validate() throws ValidatorException {
        NodeRef functionRef = null;
        NodeRef seriesRef = null;
        NodeRef volumeRef = null;
        final Map<String, Object> props = document.getProperties();
        final String functionNodeRef = (String) props.get(TransientProps.FUNCTION_NODEREF);
        final String seriesNodeRef = (String) props.get(TransientProps.SERIES_NODEREF);
        final String volumeNodeRef = (String) props.get(TransientProps.VOLUME_NODEREF);
        try {
            functionRef = new NodeRef(functionNodeRef);
            seriesRef = new NodeRef(seriesNodeRef);
            volumeRef = new NodeRef(volumeNodeRef);
        } catch (Exception e) {
            // invalid nodeRef
        }
        if (volumeRef == null) {
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
        if (DocListUnitStatus.CLOSED.equals(getVolumeService().getVolumeByNodeRef(volumeRef).getStatus())) {
            messages.add("document_validationMsg_closed_volume");
        }
        if (messages.size() > 0) {
            for (String message : messages) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), message);
            }
            return false;
        }

        return validateCase(volumeRef);
    }

    private boolean validateCase(NodeRef volumeRef) {
        final Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);
        if (volume.isContainsCases()) {
            if(StringUtils.equals(originalLocationId, getLocationId(document.getProperties()))) {
                return true; // location has not changed
            }
            log.debug("originalLocationId="+originalLocationId);
            log.debug("new     LocationId="+getLocationId(document.getProperties()));
            NodeRef caseNodeRef = null;
            try {
                caseNodeRef = new NodeRef(selectedCaseRefNum);
                if (DocListUnitStatus.CLOSED.equals(getCaseService().getCaseByNoderef(caseNodeRef).getStatus())) {
                    MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_validationMsg_closed_case");
                    return false;
                }
            } catch (Exception e) {
                // invalid nodeRef
            }
            final String newCaseTitle = (String) newCaseHtmlInput.getValue();
            if (caseNodeRef == null && StringUtils.isBlank(newCaseTitle)) {
                // client-side validation should already prevent it, but just to make sure:
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_validationMsg_mandatory_case");
                return false;
            } else if (caseNodeRef != null && StringUtils.isNotBlank(newCaseTitle)) {
                throw new RuntimeException("specified selected name of new case '" + newCaseTitle + "' and selected existing case '" + caseNodeRef + "'");
            }
        }
        return true;
    }

    private void createOrSelectCase() {
        final Map<String, Object> props = document.getProperties();
        final String volumeRef = (String) props.get(TransientProps.VOLUME_NODEREF);
        final NodeRef volumeNodeRef = new NodeRef(volumeRef);
        final Volume volume = getVolumeService().getVolumeByNodeRef(volumeNodeRef);
        if (StringUtils.equals(volumeRef, originalVolumeRef)) {
            // no need to create case nor put value of original case to props(already set when initialized)
        } else if (volume.isContainsCases()) {
            final String newCaseTitle = (String) newCaseHtmlInput.getValue();
            newCaseHtmlInput.setValue(null); // reset value(otherwise might cause troubles later, when creating new doc right after this doc has been saved)
            final NodeRef caseNodeRef;
            if (StringUtils.isNotBlank(newCaseTitle)) {
                final Case newCase = getCaseService().createCase(volumeNodeRef);
                newCase.setTitle(newCaseTitle);
                newCase.setStatus(DocListUnitStatus.OPEN.getValueName());
                getCaseService().saveOrUpdate(newCase, false);
                caseNodeRef = newCase.getNode().getNodeRef();
            } else {
                caseNodeRef = new NodeRef(selectedCaseRefNum);
            }
            if (caseNodeRef == null) {
                throw new RuntimeException("failed to save document under case, caseNodeRef=null");
            }
            props.put(TransientProps.CASE_NODEREF, caseNodeRef.toString());
        } else {
            props.put(TransientProps.CASE_NODEREF, null);
        }
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

    /**
     * @return SelectItems of cases from selected volume for jsp
     */
    public List<SelectItem> getCasesOfSelectedVolume() {
        final Map<String, Object> props = document.getProperties();
        final String volumeNodeRef = (String) props.get(TransientProps.VOLUME_NODEREF);
        if (StringUtils.isNotBlank(volumeNodeRef)) {
            NodeRef volumeRef = null;
            try {
                volumeRef = new NodeRef(volumeNodeRef);
            } catch (AlfrescoRuntimeException e) {
                // invalid nodeRef
            }
            if (volumeRef != null) {
                final List<Case> casesOfVolume = getCaseService().getAllCasesByVolume(volumeRef);
                final List<SelectItem> selectItems = new ArrayList<SelectItem>(casesOfVolume.size());
                for (Case case1 : casesOfVolume) {
                    selectItems.add(new SelectItem(case1.getNode().getNodeRefAsString(), case1.getTitle()));
                }
                selectItems.add(0, new SelectItem("[defaultSelection]", ""));
                return selectItems;
            }
        }
        return Collections.emptyList();
    }

    public void setNewCaseHtmlInput(HtmlInputTextarea newCaseInput) {
        this.newCaseHtmlInput = newCaseInput;
    }

    public HtmlInputTextarea getNewCaseHtmlInput() {
        return newCaseHtmlInput;
    }

    /** @param event from jsp */
    public void caseOfVolumeSelected(ValueChangeEvent event) {
        selectedCaseRefNum = (String) event.getNewValue();
    }

    private void reloadTransientProperties() {
        if (document == null) {
            return;
        }
        DocumentParentNodesVO parentNodes = getDocumentService().getAncestorNodesByDocument(document.getNodeRef());
        getDocumentService().setTransientProperties(document, parentNodes);
    }

    /**
     * Web-client action
     * 
     * @param event
     */
    public void registerDocument(ActionEvent event) {
        try {
            document = getDocumentService().registerDocument(document);
            getDocumentTemplateService().updateGeneratedFilesOnRegistration(document);
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * AJAX:
     */
    public void volumeContainsCasesClientHandler() throws IOException {
        NodeRef volumeNodeRef = null;
        final Map<String, Object> props = document.getProperties();
        final String volumeRef = (String) props.get(TransientProps.VOLUME_NODEREF);
        Boolean selectedVolumeContainsCases = null;
        boolean volumeSelectionChanged = false;
        log.debug("originalVolumeRef=" + originalVolumeRef + "\nnew noderef      =" + volumeRef + "");
        if (!StringUtils.equals(volumeRef, originalVolumeRef)) {
            volumeSelectionChanged = true;
            if (volumeRef != null) {
                volumeNodeRef = new NodeRef(volumeRef);
                final Volume volume = getVolumeService().getVolumeByNodeRef(volumeNodeRef);
                selectedVolumeContainsCases = volume.isContainsCases();
            } else {
                selectedVolumeContainsCases = false;
            }
        }
        // Boolean.valueOf(selectedVolumeContainsCases).toString()
        FacesContext context = FacesContext.getCurrentInstance();
        ResponseWriter out = context.getResponseWriter();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>" //
                + "<volume-info volume-selection-changed='" + volumeSelectionChanged + "' contains-cases='" + selectedVolumeContainsCases + "' />";
        out.write(xml);
        log.debug("returning XML: " + xml);
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
     * @param nodeRef to the base document
     */
    public void updateFollowUpOrReplyProperties(NodeRef nodeRef) {
        setOwnerCurrentUser();
        setDocumentName(nodeRef);
        getDocumentService().setTransientProperties(document, getDocumentService().getAncestorNodesByDocument(nodeRef));
        updateAccessRestrictionProperties(document.getProperties().get(TransientProps.SERIES_NODEREF));
        //FIXME: miks originalVolumeRef üle kirjutatakse teise dokumendi volRef'iga?
        originalVolumeRef = (String) document.getProperties().get(TransientProps.VOLUME_NODEREF);
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

    public static class Snapshot implements Serializable{
        private static final long serialVersionUID = 1L;
        
        private Node document;
        private boolean inEditMode;
        private DateFormat dateFormat;
        private String documentTypeName;
        private String selectedCaseRefNum;
        private String originalVolumeRef;
        private String originalLocationId;

        private Snapshot(MetadataBlockBean bean) {
            this.document = bean.document;
            this.inEditMode = bean.inEditMode;
            this.dateFormat = bean.dateFormat;
            this.documentTypeName = bean.documentTypeName;
            this.selectedCaseRefNum = bean.selectedCaseRefNum;
            this.originalVolumeRef = bean.originalVolumeRef;
            this.originalLocationId = bean.originalLocationId;
        }

        private void restoreState(MetadataBlockBean bean) {
            bean.document = this.document;
            bean.inEditMode = this.inEditMode;
            bean.dateFormat = this.dateFormat;
            bean.documentTypeName = this.documentTypeName;
            bean.selectedCaseRefNum = this.selectedCaseRefNum;            
            bean.originalVolumeRef = this.originalVolumeRef;
            bean.originalLocationId = this.originalLocationId;
        }
    }
    // END: snapshot logic

    // START: getters / setters

    protected SessionContext getSessionContext() {
        if (sessionContext == null) {
            sessionContext = (SessionContext) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(SessionContext.BEAN_NAME);
        }
        return sessionContext;
    }

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

    // END: getters / setters
}
