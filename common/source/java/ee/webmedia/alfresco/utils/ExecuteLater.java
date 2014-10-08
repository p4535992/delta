<<<<<<< HEAD
package ee.webmedia.alfresco.utils;

import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;

/**
 * Use {@link ComponentUtil#executeLater(PhaseId, UIComponent, org.apache.commons.collections.Closure)} for convenience.
 * Simple subclass of ActionEvent that can be used to execute some code (in {@link #execute()} method) later (specified by phaseId).
 * 
 * @author Ats Uiboupin
 */
abstract class ExecuteLater extends ActionEvent {
    private static final long serialVersionUID = 1L;
    boolean notExecuted = true;

    ExecuteLater(PhaseId phaseId, UIComponent uiComponent) {
        super(uiComponent);
        setPhaseId(phaseId);
        uiComponent.queueEvent(this);
    }

    @Override
    public void processListener(FacesListener faceslistener) {
        notExecuted = false;
        execute();
    }

    public abstract void execute();

    @Override
    public boolean isAppropriateListener(FacesListener faceslistener) {
        return notExecuted;
    }
=======
package ee.webmedia.alfresco.utils;

import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;

/**
 * Use {@link ComponentUtil#executeLater(PhaseId, UIComponent, org.apache.commons.collections.Closure)} for convenience.
 * Simple subclass of ActionEvent that can be used to execute some code (in {@link #execute()} method) later (specified by phaseId).
 */
abstract class ExecuteLater extends ActionEvent {
    private static final long serialVersionUID = 1L;
    boolean notExecuted = true;

    ExecuteLater(PhaseId phaseId, UIComponent uiComponent) {
        super(uiComponent);
        setPhaseId(phaseId);
        uiComponent.queueEvent(this);
    }

    @Override
    public void processListener(FacesListener faceslistener) {
        notExecuted = false;
        execute();
    }

    public abstract void execute();

    @Override
    public boolean isAppropriateListener(FacesListener faceslistener) {
        return notExecuted;
    }
>>>>>>> develop-5.1
}