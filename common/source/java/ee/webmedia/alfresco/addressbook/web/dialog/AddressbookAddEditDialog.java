package ee.webmedia.alfresco.addressbook.web.dialog;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSignatureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSkLdapService;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.service.AddressbookEntry;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class AddressbookAddEditDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "AddressbookAddEditDialog";
    public static final String PERSON_CODE_EXISTS_ERROR = "addressbook_save_person_error_codeExists";
    public static final String PERSON_NAME_EXISTS_ERROR = "addressbook_save_person_error_nameExists";
    public static final String ORG_CODE_EXISTS_ERROR = "addressbook_save_organization_error_codeExists";
    public static final String ORG_NAME_EXISTS_ERROR = "addressbook_save_organization_error_nameExists";

    protected Node entry;
    protected NodeRef parentOrg = null;
    private boolean skipReset = false;
    private List<AddressbookEntry> orgCertificates = Collections.emptyList();
    private List<AddressbookEntry> orgCertificatesToRemove = Collections.emptyList();

    // ------------------------------------------------------------------------------
    // Wizard implementation

    public void setOrgCertificates(List<AddressbookEntry> orgCertificates) {
		this.orgCertificates = orgCertificates;
	}
    
    public List<AddressbookEntry> getOrgCertificates() {
		return orgCertificates;
	}

	public void setupAdd(ActionEvent event) {
        QName type = QName.createQName(ActionUtil.getParam(event, "type"), getNamespaceService());
        entry = getAddressbookService().getEmptyNode(type);
        if (ActionUtil.hasParam(event, "parentOrg")) {
            parentOrg = new NodeRef(ActionUtil.getParam(event, "parentOrg"));
        } else {
            parentOrg = null;
        }
        orgCertificates = Collections.emptyList();
        orgCertificatesToRemove = Collections.emptyList();
    }

    public void setupEdit(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        entry = getAddressbookService().getNode(nodeRef);
        if (isOrganizationType()) {
        	loadOrgCertificates();
        }
        orgCertificatesToRemove = new ArrayList<AddressbookEntry>();
    }
    
    private void loadOrgCertificates() {
        BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable {

                setOrgCertificates(getAddressbookService().listOrganizationCertificates(entry.getNodeRef()));

                return null;
            }
        });
    }
    
    public void updateOrgCertificates(ActionEvent event) {
    	if (entry == null) {
    		return;
    	}
    	
    	String orgCode = (String)entry.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE.toString());
    	String orgName = (String)entry.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME.toString());
    	List<SkLdapCertificate> skLdapCerts = null;
    	if (StringUtils.isNotBlank(orgCode)) {
    		skLdapCerts = getSkLdapService().getCertificates(orgCode);
    	} else if (StringUtils.isNotBlank(orgName)) {
    		skLdapCerts = getSkLdapService().getCertificatesByName(orgName);
    	}
    	
    	if (skLdapCerts != null && !skLdapCerts.isEmpty()) {
    		// update orgCertificates
    		for (SkLdapCertificate skLdapCert: skLdapCerts) {
    			X509Certificate cert = getSignatureService().getCertificateForEncryption(skLdapCert);
    			if (cert != null) {
    				String cnName = skLdapCert.getCn();
    				Date validTo = cert.getNotAfter();
    				if (!isAlreadyAddedCert(cnName, validTo)) {
    					String base64Cert = Base64.encodeBase64String(skLdapCert.getUserCertificate());
    					Node orgCertNode = getAddressbookService().getEmptyNode(AddressbookModel.Types.ORGCERTIFICATE);
    					Map<String, Object> properties = orgCertNode.getProperties();
    					properties.put(AddressbookModel.Props.ORG_CERT_NAME.toString(), cnName);
    					properties.put(AddressbookModel.Props.ORG_CERT_VALID_TO.toString(), validTo);
    					properties.put(AddressbookModel.Props.ORG_CERT_CONTENT.toString(), base64Cert);
    					orgCertificates.add(new AddressbookEntry(orgCertNode, entry.getNodeRef()));
    				}
    			}
    		}
    	} else {
    		MessageUtil.addInfoMessage("addressbook_org_certs_empty_error");
    	}
    }
    
    private boolean isAlreadyAddedCert(String cnName, Date validTo) {
    	for (AddressbookEntry orgCert: orgCertificates) {
    		if (cnName.equals(orgCert.getCertName()) && validTo.equals(orgCert.getCertValidTo())) {
    			return true;
    		}
		}
    	return false;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        if (getEntry() == null) {
            Utils.addErrorMessage("addressbook_data_not_found");
            skipReset = true;
            return null;
        }
        if (!validate()) {
            skipReset = true;
            return null;
        }
        checkUserInput();
        return saveData(context, outcome);
    }

    private void checkUserInput() {
        Map<String, Object> properties = getEntry().getProperties();
        // Remove Whitespace from orgCode
        String personId = (String) properties.get(AddressbookModel.Props.PERSON_ID.toString());
        if (personId != null) {
            properties.put(AddressbookModel.Props.PERSON_ID.toString(), StringUtils.deleteWhitespace(personId));
        }
        String orgCode = (String) properties.get(AddressbookModel.Props.ORGANIZATION_CODE.toString());
        if (orgCode != null) {
            properties.put(AddressbookModel.Props.ORGANIZATION_CODE.toString(), StringUtils.deleteWhitespace(orgCode));
        }
        // ... and email
        String email = (String) properties.get(AddressbookModel.Props.EMAIL.toString());
        if (email != null) {
            properties.put(AddressbookModel.Props.EMAIL.toString(), StringUtils.deleteWhitespace(email));
        }
    }

    private boolean validate() {
        Node contact = getEntry();
        boolean isValid = true;
        if (Boolean.TRUE.equals(contact.getProperties().get(AddressbookModel.Props.TASK_CAPABLE))) {
            if (StringUtils.isBlank((String) contact.getProperties().get(AddressbookModel.Props.EMAIL))) {
                MessageUtil.addInfoMessage("addressbook_contact_email_empty_error");
                isValid = false;
            }
        }
        String website = (String) contact.getProperties().get(AddressbookModel.Props.WEBSITE);
        if (StringUtils.isNotBlank(website) && WebUtil.isNotValidUrl(website)) {
            MessageUtil.addInfoMessage("addressbook_contact_invalid_webiste");
            isValid = false;
        }
        return isValid;
    }

    @Override
    public String cancel() {
        entry = null;
        skipReset = false;
        orgCertificates = Collections.emptyList();
        orgCertificatesToRemove = Collections.emptyList();
        return super.cancel();
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        if (!skipReset) {
            entry = null;
        }
        return outcome;
    }

    protected String saveData(FacesContext context, String outcome) {
        List<Pair<String, String>> duplicateMessages = getAddressbookService().checkIfContactExists(getEntry());
        boolean allowSave = true;
        String confirmMessage = null;
        for (Pair<String, String> message : duplicateMessages) {
            String messageKey = message.getFirst();
            if (PERSON_CODE_EXISTS_ERROR.equals(messageKey)
                    || ORG_CODE_EXISTS_ERROR.equals(messageKey)) {
                MessageUtil.addErrorMessage(context, messageKey, message.getSecond());
                allowSave = false;
            } else {
                confirmMessage = MessageUtil.getMessage(messageKey, message.getSecond());
                if (confirmMessage != null) {
                    break;
                }
            }
        }
        if (!allowSave) {
            isFinished = false;
            skipReset = true;
            outcome = null;
        } else if (confirmMessage != null) {
            ConfirmAddDuplicateDialog confirmBean = (ConfirmAddDuplicateDialog) FacesHelper.getManagedBean( //
                    context, ConfirmAddDuplicateDialog.BEAN_NAME);
            confirmBean.setConfirmMessage(confirmMessage);
            isFinished = false;
            skipReset = true;
            outcome = "dialog:confirmAddDuplicate";
        } else {
        	NodeRef entryNodeRef = persistEntry();
        	persistOrgCertificates(entryNodeRef);
            MessageUtil.addInfoMessage("save_success");
            
        }
        return outcome;
    }

    public NodeRef persistEntry() {
        NodeRef entryNodeRef = getAddressbookService().addOrUpdateNode(getEntry(), parentOrg);
        skipReset = false;
        return entryNodeRef;
    }
    
    public void persistOrgCertificates(NodeRef orgNodeRef) {
    	for (AddressbookEntry orgCert: orgCertificates) {
    		getAddressbookService().addOrUpdateNode(orgCert.getNode(), orgNodeRef);
    	}
    	for (AddressbookEntry orgCert: orgCertificatesToRemove) {
    		getAddressbookService().deleteNode(orgCert.getNodeRef());
    	}
    }
    
    public String getConfirmMessage() {
        return "addressbook_save_organization_error_nameExists";
    }
    
    public void removeOrgCert(ActionEvent event) {
      
        String orgCertRef = ActionUtil.getParam(event, "nodeRef");
        int indexToRemove = -1;
        for (int i = 0; i < orgCertificates.size(); i++) {
        	AddressbookEntry orgCertEntry = orgCertificates.get(i);
        	if (orgCertEntry.getNodeRef().toString().equals(orgCertRef)) {
        		indexToRemove = i;
        		if (getNodeService().exists(orgCertEntry.getNodeRef())) {
        			orgCertificatesToRemove.add(orgCertEntry);
        		}
        	}
        }
        orgCertificates.remove(indexToRemove);
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public String getContainerTitle() {
        String messageId = "addressbook_" + ((entry instanceof TransientNode) ? "add" : "edit") + "_" + entry.getType().getLocalName();
        return MessageUtil.getMessage(messageId);
    }

    public String getPanelTitle() {
        String messageId = "addressbook_" + ((entry instanceof TransientNode) ? "new" : "edit") + "_entry";
        return MessageUtil.getMessage(messageId);
    }

    public boolean getNotAllowedEditTaskCapable() {
        if (entry == null || entry instanceof TransientNode) {
            return false;
        }
        return getAddressbookService().isTaskCapableGroupMember(entry.getNodeRef());
    }

    public boolean isNotDvkCapable() {
        if (entry == null) {
            return true;
        }
        NodeRef nodeRef = entry.getNodeRef();
        if (nodeRef == null || !nodeExists(nodeRef)) {
            return true;
        }
        return !Boolean.TRUE.equals(getNodeService().getProperty(nodeRef, AddressbookModel.Props.DVK_CAPABLE));
    }
    
    public boolean isAllowUpdateCertificates() {
        
        return isOrganizationType();
    }
    
    public boolean isOrganizationType() {
    	return entry != null?Types.ORGANIZATION.equals(entry.getType()):false;
    }

    // ------------------------------------------------------------------------------
    // Bean Getters and Setters

    public Node getEntry() {
        return entry;
    }
    

}
