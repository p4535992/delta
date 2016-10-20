package ee.webmedia.alfresco.signature.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.content.DocumentDetailsDialog;
import org.alfresco.web.ui.common.Utils;
import org.apache.log4j.Logger;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.DigiDoc4JSignatureService;

public class SignatureDocumentDetailsDialog extends DocumentDetailsDialog {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "SignatureDocumentDetailsDialog";

    private static Logger log = Logger.getLogger(SignatureDocumentDetailsDialog.class);

    private transient DigiDoc4JSignatureService digiDoc4JSignatureService;

    private boolean isDdocValid = true;
    private List<DataItem> dataItems;
    private List<SignatureItem> signatures;

    public void setDigiDoc4JSignatureService(DigiDoc4JSignatureService digiDoc4JSignatureService) {
        this.digiDoc4JSignatureService = digiDoc4JSignatureService;
    }

    protected DigiDoc4JSignatureService getDigiDoc4JSignatureService() {
        if (digiDoc4JSignatureService == null) {
        	digiDoc4JSignatureService = (DigiDoc4JSignatureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    DigiDoc4JSignatureService.BEAN_NAME);
        }
        return digiDoc4JSignatureService;
    }

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        loadDigiDoc();
    }

    @Override
    public void restored() {
        super.restored();
        loadDigiDoc();
    }

    private void loadDigiDoc() {
        resetData();
        // null when document was deleted
        if (browseBean.getDocument() == null) {
            return;
        }
        if (!getDigiDoc4JSignatureService().isBDocContainer(browseBean.getDocument().getNodeRef())) {
            return;
        }

        try {
            getDataFilesAndSignatures();
        } catch (SignatureException e) {
            log.debug(e.getMessage(), e);
            Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), "ddoc_container_fail"));
            return;
        }
        isDdocValid = true;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        resetData();
        return super.finishImpl(context, outcome);
    }

    @Override
    public String cancel() {
        resetData();
        return super.cancel();
    }

    private void resetData() {
        isDdocValid = false;
        dataItems = null;
        signatures = null;
    }

    public boolean isDdocValid() {
        return isDdocValid;
    }

    public List<DataItem> getDataFiles() {
        return dataItems;
    }

    public List<SignatureItem> getSignatures() {
        return signatures;
    }

    /**
     * Used in JSP pages.
     */
    public boolean isContainerDataFiles() {
        return isDdocValid();
    }

    /**
     * Used in JSP pages.
     */
    public boolean isContainerSignatures() {
        return isDdocValid();
    }

    private void getDataFilesAndSignatures() throws SignatureException {
        SignatureItemsAndDataItems values = getDigiDoc4JSignatureService().getDataItemsAndSignatureItems(browseBean.getDocument().getNodeRef(), false);
        signatures = values.getSignatureItems();
        dataItems = values.getDataItems();
    }

}
