package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClearStateNotificationHandler;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.ClearStateNotificationHandler.ClearStateListener;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Base class for dialogs that must support multiple instances of the same dialog opened after each other and restored when closing dialog opened after that dialog.
 * S - implementation of {@link Snapshot} that contains data that must be saved for later use to reconstruct given state.<br>
 * D - type of object that is used to reset dialog.<br>
 * <b>Definition: Snapshot</b> - represents a state of a dialog at given moment. It can be used to reconstruct the dialog with the same state.<br>
 * 
 * @author Ats Uiboupin
 */
public abstract class BaseSnapshotCapableDialog<S extends BaseSnapshotCapableDialog.Snapshot, D extends Object> extends BaseDialogBean
        implements ClearStateListener {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(BaseSnapshotCapableDialog.class);

    // =========================================================================
    /**
     * Implementation should contain data that can be used to fully reconstruct given state.<br>
     * 
     * @author Ats Uiboupin
     */
    public static interface Snapshot extends Serializable {
        // subclasses should hold the state of each "Snapshot" in the fields of the subclass
        String getOpenDialogNavigationOutcome();
    }

    private final Deque<S> snapshots = new ArrayDeque<S>();

    protected S getCurrentSnapshot() {
        return snapshots.peek();
    }

    protected S createSnapshot(S newSnapshot) {
        snapshots.push(newSnapshot);
        WebUtil.navigateTo(newSnapshot.getOpenDialogNavigationOutcome());
        return newSnapshot;
    }

    @Override
    public void clearState() {
        snapshots.clear();
    }

    private String closeDialogSnapshot() {
        snapshots.pop();
        return super.cancel();
    }

    // =========================================================================

    @Override
    public void init(Map<String, String> params) {
        LOG.info("init");
        getClearStateNotificationHandler().addClearStateListener(this);
        super.init(params);
    }

    @Override
    public void restored() {
        LOG.info("restored");
        resetOrInit(getDataProvider());
        // Siin ei ole plaanis midagi teha; kui mingi teine dialoog suletakse ja seetõttu pöördutakse tagasi varemavatud dok.dialoogile,
        // siis nimelt ei tee õiguste ega kustutamise kontrolli
        // Õiguste kontroll on ainult dialoogile sisenemisel
        // Ja eksisteerimise kontroll on igasuguste erinevate tegevuste juures, sest suvalisel hetkel võib niikuinii keegi teine dokumendi kustutada
    }

    @Override
    public String cancel() {
        return cancel(true);
    }

    protected String cancel(boolean doClose) {
        if (doClose) {
            return closeDialogSnapshot();
        }
        return super.cancel();
    }

    abstract protected void resetOrInit(D provider);

    abstract protected D getDataProvider();

    // =========================================================================

}
