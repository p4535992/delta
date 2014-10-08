package ee.webmedia.alfresco.docconfig.generator;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
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
