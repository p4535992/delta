package ee.webmedia.alfresco.volume.web;

import java.util.Calendar;
import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.DialogManager;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.volume.model.Volume;

/**
 * Generates usual check box, but makes it readonly if necessary: {@link SendToDestructionGenerator#isReadOnly()}.
 * 
 * @author Romet Aidla
 */
public class SendToDestructionGenerator extends BaseComponentGenerator {
    private DialogManager dialogManager;

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_SELECT_BOOLEAN);
        FacesHelper.setupComponentId(context, component, id);
        return component;
    }

    @Override
    public UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item) {
        UIComponent component = super.createComponent(context, propertySheet, item);
        if (isReadOnly()) {
            ComponentUtil.setDisabledAttributeRecursively(component);
        }
        return component;
    }

    private boolean isReadOnly() {
        VolumeDetailsDialog dialog = ((VolumeDetailsDialog) dialogManager.getBean());
        Volume volume = dialog.getCurrentVolume();
        boolean isDestroyed = DocListUnitStatus.DESTROYED.getValueName().equals(volume.getStatus());
        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        boolean isDispositionDateInFuture = volume.getDispositionDate() != null && volume.getDispositionDate().after(today);
        return isDestroyed || isDispositionDateInFuture;
    }

    public void setDialogManager(DialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }
}
