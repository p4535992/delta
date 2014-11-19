<<<<<<< HEAD
package ee.webmedia.alfresco.document.web;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Provides functionality for modal that adds documents to users favorites
 * 
 * @author Vladimir Drozdik
 */
public class FavoritesModalComponent extends UICommand {

    public static final String ADD_TO_FAVORITES_MODAL_ID = "addToFavorites";

    public FavoritesModalComponent() {
        setRendererType(null);
    }

    @Override
    public void decode(FacesContext context) {
        @SuppressWarnings("unchecked")
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String actionValue = requestMap.get(getClientId(context));
        if (StringUtils.isNotBlank(actionValue)) {
            if (StringUtils.equals("SAVE", actionValue)) {
                String favDirName = requestMap.get(FacesHelper.makeLegalId(getSuggesterId(context)));
                AddToFavoritesEvent event = new AddToFavoritesEvent(this, favDirName);
                queueEvent(event);
            } else {
                throw new RuntimeException("Unknown action: " + actionValue);
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (isRendered() == false) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();

        // modal popup code
        ComponentUtil.writeModalHeader(out, ADD_TO_FAVORITES_MODAL_ID, MessageUtil.getMessage("document_favorites_add"), null);

        // popup content
        out.write("<table><tbody>");
        out.write("<tr><td>" + MessageUtil.getMessage("document_addFavoriteDirectory") + ":</td></tr>");
        out.write("<tr><td>");

        SuggesterGenerator generator = new SuggesterGenerator();
        Map<String, String> attributes = generator.getCustomAttributes();
        attributes.put(SuggesterGenerator.ComponentAttributeNames.SUGGESTER_VALUES, "#{DocumentDialog.getFavoriteDirectoryNames}");
        UIComponent suggest = generator.generate(context, getSuggesterId(context));
        ComponentUtil.putAttribute(suggest, "styleClass", ComponentUtil.getAttribute(suggest, "styleClass") + " expand19-200");
        Utils.encodeRecursive(context, suggest);
        out.write("</td></tr>");
        out.write("<tr><td>");
        out.write("<input id='" + getSuggesterId(context) + "_btn' type='submit' value='" + MessageUtil.getMessage("save")
                + "' onclick=\"" + Utils.generateFormSubmit(context, this, getClientId(context), "SAVE") + "\" />");
        out.write("</td></tr>");
        out.write("</tbody></table>");

        ComponentUtil.writeModalFooter(out);
    }

    private String getSuggesterId(FacesContext context) {
        return getClientId(context) + "_favDirName";
    }

    public static class AddToFavoritesEvent extends ActionEvent {

        private static final long serialVersionUID = 1L;
        private final String favoriteDirectoryName;

        public AddToFavoritesEvent(UIComponent component, String favoriteDirectoryName) {
            super(component);
            this.favoriteDirectoryName = favoriteDirectoryName;
        }

        public String getFavoriteDirectoryName() {
            return favoriteDirectoryName;
        }

    }

}
=======
package ee.webmedia.alfresco.document.web;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Provides functionality for modal that adds documents to users favorites
 */
public class FavoritesModalComponent extends UICommand {

    public static final String ADD_TO_FAVORITES_MODAL_ID = "addToFavorites";

    public FavoritesModalComponent() {
        setRendererType(null);
    }

    @Override
    public void decode(FacesContext context) {
        @SuppressWarnings("unchecked")
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String actionValue = requestMap.get(getClientId(context));
        if (StringUtils.isNotBlank(actionValue)) {
            if (StringUtils.equals("SAVE", actionValue)) {
                String favDirName = requestMap.get(FacesHelper.makeLegalId(getSuggesterId(context)));
                AddToFavoritesEvent event = new AddToFavoritesEvent(this, favDirName);
                queueEvent(event);
            } else {
                throw new RuntimeException("Unknown action: " + actionValue);
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (isRendered() == false) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();

        // modal popup code
        ComponentUtil.writeModalHeader(out, ADD_TO_FAVORITES_MODAL_ID, MessageUtil.getMessage("document_favorites_add"), null);

        // popup content
        out.write("<table><tbody>");
        out.write("<tr><td>" + MessageUtil.getMessage("document_addFavoriteDirectory") + ":</td></tr>");
        out.write("<tr><td>");

        SuggesterGenerator generator = new SuggesterGenerator();
        Map<String, String> attributes = generator.getCustomAttributes();
        attributes.put(SuggesterGenerator.ComponentAttributeNames.SUGGESTER_VALUES, "#{DocumentDialog.getFavoriteDirectoryNames}");
        UIComponent suggest = generator.generate(context, getSuggesterId(context));
        ComponentUtil.putAttribute(suggest, "styleClass", ComponentUtil.getAttribute(suggest, "styleClass") + " expand19-200");
        Utils.encodeRecursive(context, suggest);
        out.write("</td></tr>");
        out.write("<tr><td>");
        out.write("<input id='" + getSuggesterId(context) + "_btn' type='submit' value='" + MessageUtil.getMessage("save")
                + "' onclick=\"" + Utils.generateFormSubmit(context, this, getClientId(context), "SAVE") + "\" />");
        out.write("</td></tr>");
        out.write("</tbody></table>");

        ComponentUtil.writeModalFooter(out);
    }

    private String getSuggesterId(FacesContext context) {
        return getClientId(context) + "_favDirName";
    }

    public static class AddToFavoritesEvent extends ActionEvent {

        private static final long serialVersionUID = 1L;
        private final String favoriteDirectoryName;

        public AddToFavoritesEvent(UIComponent component, String favoriteDirectoryName) {
            super(component);
            this.favoriteDirectoryName = favoriteDirectoryName;
        }

        public String getFavoriteDirectoryName() {
            return favoriteDirectoryName;
        }

    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
