package ee.webmedia.alfresco.common.propertysheet.search;

import java.io.IOException;

import javax.faces.context.ResponseWriter;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class UserSearchRenderer extends SearchRenderer {
    public static final String USER_SEARCH_RENDERER_TYPE = UserSearchRenderer.class.getCanonicalName();

    @Override
    protected void renderExtraInfo(Search search, ResponseWriter out) throws IOException {
        UserSearchViewModeRenderer.renderExtraInfo(search, out);
    }
}
