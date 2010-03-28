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
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class SignatureBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(SignatureBlockBean.class);

    public static final String BEAN_NAME = "SignatureBlockBean";

    private transient SignatureService signatureService;

    private NodeRef file;
    private List<DataItem> dataItems;
    private List<SignatureItem> signatureItems;

    protected SignatureService getSignatureService() {
        if (signatureService == null) {
            signatureService = (SignatureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    SignatureService.BEAN_NAME);
        }
        return signatureService;
    }

    public void init(NodeRef file) {
        this.file = file;
        load();
    }

    public void restored() {
        load();
    }

    private void load() {
        if (!getSignatureService().isDigiDocContainer(file)) {
            return;
        }

        dataItems = null;
        signatureItems = null;
        try {
            SignatureItemsAndDataItems values = getSignatureService().getDataItemsAndSignatureItems(file, false);
            signatureItems = values.getSignatureItems();
            dataItems = values.getDataItems();
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
        return dataItems == null ? Collections.<DataItem>emptyList() : dataItems;
    }

    /**
     * Used in JSP pages.
     */
    public List<SignatureItem> getSignatureItems() {
        return signatureItems == null ? Collections.<SignatureItem>emptyList() : signatureItems;
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
        log.warn(e.getMessage(), e);
        String additionalInfo = "";
        if (e.getCause() != null && StringUtils.isNotEmpty(e.getCause().getMessage())) {
            additionalInfo = ": " + e.getCause().getMessage();
        }
        Utils.addErrorMessage(MessageUtil.getMessage("ddoc_signature_failed") + additionalInfo);
    }

}
