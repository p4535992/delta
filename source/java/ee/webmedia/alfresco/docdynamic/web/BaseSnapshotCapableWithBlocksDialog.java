package ee.webmedia.alfresco.docdynamic.web;

import java.util.HashMap;
import java.util.Map;

import ee.webmedia.alfresco.common.listener.RefreshEventListener;

/**
 * Base class for dialogs that must support multiple instances of the same dialog opened after each other<br>
 * and contains blocks that can be (re)initialized with the same object that the dialog is (re)initialized
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public abstract class BaseSnapshotCapableWithBlocksDialog<S extends BaseSnapshotCapableDialog.Snapshot, B extends DialogBlockBean<D>, D extends Object>
        extends BaseSnapshotCapableDialog<S, D> implements RefreshEventListener {
    private static final long serialVersionUID = 1L;

    @Override
    protected void resetOrInit(D provider) {
        for (B block : getBlocks().values()) {
            block.resetOrInit(provider);
        }
    }

    private Map<Class<? extends B>, B> blocks;

    protected Map<Class<? extends B>, B> getBlocks() {
        if (blocks == null) {
            blocks = new HashMap<Class<? extends B>, B>();
        }
        return blocks;
    }

    @Override
    public void refresh() {
        if (getCurrentSnapshot() == null) {
            return;
        }
        for (B block : getBlocks().values()) {
            if (block instanceof RefreshEventListener) {
                ((RefreshEventListener) block).refresh();
            }
        }
    }

}
