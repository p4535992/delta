<<<<<<< HEAD
package ee.webmedia.alfresco.menu.service;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Riina Tens
 */
public enum ShortcutMenuItemOutcome {

    /** Case file outcome "dialog:caseFileDialog" is produced by BaseSnapshotCapableDialog.createSnapshot */
    CASE_FILE("", "#{CaseFileDialog.openFromDocumentList}", Arrays.asList(VolumeModel.Props.MARK, VolumeModel.Props.TITLE)),
    VOLUME("dialog:caseDocListDialog", "#{CaseDocumentListDialog.showAllFromShortcut}", Arrays.asList(VolumeModel.Props.MARK, VolumeModel.Props.TITLE));

    ShortcutMenuItemOutcome(String outcome, String action, List<QName> titlePropQNames) {
        this.action = action;
        this.outcome = outcome;
        this.titlePropQNames = titlePropQNames;
    }

    private String outcome;
    private String action;
    private List<QName> titlePropQNames;

    public String getOutcome() {
        return outcome;
    }

    public String getAction() {
        return action;
    }

    public List<QName> getTitlePropQNames() {
        return titlePropQNames;
    }

}
=======
package ee.webmedia.alfresco.menu.service;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.volume.model.VolumeModel;

public enum ShortcutMenuItemOutcome {

    /** Case file outcome "dialog:caseFileDialog" is produced by BaseSnapshotCapableDialog.createSnapshot */
    CASE_FILE("", "#{CaseFileDialog.openFromDocumentList}", Arrays.asList(VolumeModel.Props.MARK, VolumeModel.Props.TITLE)),
    VOLUME("dialog:caseDocListDialog", "#{CaseDocumentListDialog.showAllFromShortcut}", Arrays.asList(VolumeModel.Props.MARK, VolumeModel.Props.TITLE));

    ShortcutMenuItemOutcome(String outcome, String action, List<QName> titlePropQNames) {
        this.action = action;
        this.outcome = outcome;
        this.titlePropQNames = titlePropQNames;
    }

    private String outcome;
    private String action;
    private List<QName> titlePropQNames;

    public String getOutcome() {
        return outcome;
    }

    public String getAction() {
        return action;
    }

    public List<QName> getTitlePropQNames() {
        return titlePropQNames;
    }

}
>>>>>>> develop-5.1
