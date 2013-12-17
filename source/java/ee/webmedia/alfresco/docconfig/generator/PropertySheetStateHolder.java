package ee.webmedia.alfresco.docconfig.generator;

import java.io.Serializable;

/**
 * @author Alar Kvell
 */
public interface PropertySheetStateHolder extends Serializable {

    void reset(DialogDataProvider dialogDataProvider);

}
