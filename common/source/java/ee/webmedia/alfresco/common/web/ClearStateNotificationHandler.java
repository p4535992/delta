package ee.webmedia.alfresco.common.web;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles registering and notifying components (dialogs),
 * that must clear their state at different situations/events in application life-cycle.
 * For ex. when menu item is clicked and all the dialogs state must be cleared.
 *
 * @author Romet Aidla
 */
public class ClearStateNotificationHandler implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "ClearStateNotificationHandler";
    private Set<ClearStateListener> clearStateListeners = new HashSet<ClearStateListener>();

    /**
     * Adds listener for receiving clear state notification.
     * Same listener can be added several times, but it will receive only one notification.
     *
     * @param listener Listener that will be notified when state must be cleared.
     */
    public void addClearStateListener(ClearStateListener listener) {
        clearStateListeners.add(listener);
    }

    /**
     * Notifies all listeners that their state must be cleared.
     */
    public void notifyClearStateListeners() {
        for (ClearStateListener listener : clearStateListeners) {
            listener.clearState();
        }
    }

    /**
     * Marker interface, component that wants to receive clear state notifications, must implement it.
     */
    public interface ClearStateListener extends Serializable {
        /**
         * Called when component state must be cleared.
         */
        void clearState();
    }
}
