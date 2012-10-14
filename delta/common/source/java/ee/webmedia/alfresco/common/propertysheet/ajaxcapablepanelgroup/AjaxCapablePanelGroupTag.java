package ee.webmedia.alfresco.common.propertysheet.ajaxcapablepanelgroup;

import org.apache.myfaces.taglib.html.HtmlPanelGroupTag;

/**
 * @author Riina Tens
 */
public class AjaxCapablePanelGroupTag extends HtmlPanelGroupTag {

    @Override
    public String getComponentType() {
        return AjaxCapablePanelGroup.AJAX_CAPABLE_PANEL_GROUP_COMPONENT_TYPE;
    }

}
