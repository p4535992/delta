package ee.webmedia.alfresco.signature.web;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.DigiDoc4JSignatureService;
import ee.webmedia.alfresco.utils.MessageUtil;

public class SignatureBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(SignatureBlockBean.class);

    public static final String BEAN_NAME = "SignatureBlockBean";

    private transient DigiDoc4JSignatureService digiDoc4JSignatureService;

    private NodeRef file;
    private List<DataItem> dataItems;
    private List<SignatureItem> signatureItems;

    protected DigiDoc4JSignatureService getDigiDoc4JSignatureService() {
        log.debug("getDigiDoc4JSignatureService():...");
        if (digiDoc4JSignatureService == null) {
        	digiDoc4JSignatureService = (DigiDoc4JSignatureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    DigiDoc4JSignatureService.BEAN_NAME);
        }
        return digiDoc4JSignatureService;
    }

    public void init(NodeRef file) {
        reset();
        this.file = file;
        load();
    }

    public void restored() {
        load();
    }

    private void load() {
        log.debug("load()...");
        dataItems = null;
        signatureItems = null;
        if (!getDigiDoc4JSignatureService().isBDocContainer(file)) {
            return;
        }

        try {
            log.debug("GET SignatureItemsAndDataItems...");
            SignatureItemsAndDataItems values = getDigiDoc4JSignatureService().getDataItemsAndSignatureItems(file, false);
            signatureItems = values.getSignatureItems();
            log.debug("SignatureITEMS: " + (signatureItems!= null ? signatureItems.size() : "NULL"));
            dataItems = values.getDataItems();
            log.debug("dataITEMS: " + (dataItems!= null ? dataItems.size() : "NULL"));
        } catch (SignatureException e) {
            log.debug(e.getMessage(), e);
            Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), "ddoc_container_fail"));
            return;
        }
    }

    public void reset() {
        file = null;
        dataItems = null;
        signatureItems = null;
    }

    /**
     * Used in JSP pages.
     */
    public List<DataItem> getDataItems() {
        return dataItems == null ? Collections.<DataItem> emptyList() : dataItems;
    }

    /**
     * Used in JSP pages.
     */
    public List<SignatureItem> getSignatureItems() {
        return signatureItems == null ? Collections.<SignatureItem> emptyList() : signatureItems;
    }

    /**
     * Used in JSP pages.
     */
    public boolean isDataItemsRendered() {
        return dataItems != null && !dataItems.isEmpty();
    }

    /**
     * Used in JSP pages.
     */
    public boolean isSignatureItemsRendered() {
        return signatureItems != null && !signatureItems.isEmpty();
    }

    public static void addSignatureError(Exception e) {
        log.warn("addSignatureError(): ERROR MESSAGE: " + e.getMessage(), e);
        String additionalInfo = "";
        if (e.getCause() != null && StringUtils.isNotEmpty(e.getCause().getMessage())) {
            additionalInfo = ": " + e.getCause().getMessage();
        }
        Utils.addErrorMessage(MessageUtil.getMessage("ddoc_signature_failed") + additionalInfo);
    }

}
