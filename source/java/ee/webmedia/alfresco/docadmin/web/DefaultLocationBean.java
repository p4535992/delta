package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.DialogBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.RepoUtil;

public class DefaultLocationBean implements DialogBlockBean<DocTypeDetailsDialog>, DialogDataProvider {

    private static final long serialVersionUID = 1L;

    private Node locationPropHolder;
    private transient UIPropertySheet propertySheet;
    private DocumentConfig config;
    private PropertySheetConfigElement locationPropSheetConfig;

    @Override
    public void resetOrInit(DocTypeDetailsDialog docTypeDetailsDialog) {
        if (docTypeDetailsDialog != null) {
            locationPropHolder = docTypeDetailsDialog.getDocType().getNode();
            if (propertySheet != null) {
                propertySheet.getChildren().clear();
                propertySheet.setNode(locationPropHolder);
            }
            getPropertySheetStateBean().reset(getConfig().getStateHolders(), this);
        } else {
            reset();
        }
    }

    private void reset() {
        locationPropHolder = null;
        locationPropSheetConfig = null;
        getPropertySheetStateBean().reset(null, null);
    }

    // START: getters/setters

    public boolean isPanelExpanded() {
        return locationPropHolder != null && locationPropHolder.getProperties().get(DocumentCommonModel.Props.FUNCTION) != null;
    }

    public boolean isPanelRendered() {
        return locationPropHolder != null && RepoUtil.isSaved(locationPropHolder);
    }

    @Override
    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public Node getLocationPropHolder() {
        return locationPropHolder;
    }

    public void setLocationPropHolder(Node locationPropHolder) {
        this.locationPropHolder = locationPropHolder;
    }

    public PropertySheetConfigElement getLocationPropSheetConfig() {
        if (locationPropSheetConfig == null) {
            locationPropSheetConfig = getConfig().getPropertySheetConfigElement();
        }
        return locationPropSheetConfig;
    }

    public void setLocationPropSheetConfig(PropertySheetConfigElement locationPropSheetConfig) {
        this.locationPropSheetConfig = locationPropSheetConfig;
    }

    public DocumentConfig getConfig() {
        if (config == null) {
            config = getDocumentConfigService().getDocLocationConfig();
        }
        return config;
    }

    @Override
    public DocumentDynamic getDocument() {
        // Not supported
        return null;
    }

    @Override
    public CaseFile getCaseFile() {
        // Not supported
        return null;
    }

    @Override
    public Node getNode() {
        return locationPropHolder;
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    public void switchMode(boolean inEditMode) {
        // Not supported
    }

    // END: getters/setters
}