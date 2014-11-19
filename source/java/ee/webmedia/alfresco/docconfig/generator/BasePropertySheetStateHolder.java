package ee.webmedia.alfresco.docconfig.generator;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public abstract class BasePropertySheetStateHolder implements PropertySheetStateHolder {
    private static final long serialVersionUID = 1L;

    protected DialogDataProvider dialogDataProvider;

    @Override
    public void reset(DialogDataProvider dialogDataProvider) {
        this.dialogDataProvider = dialogDataProvider;
        reset(dialogDataProvider.isInEditMode());
    }

    protected void reset(@SuppressWarnings("unused") boolean inEditMode) {
        // Child classes can override
    }

}
