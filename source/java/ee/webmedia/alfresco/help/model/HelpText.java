package ee.webmedia.alfresco.help.model;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;

/**
 * Wrapper object for HelpText node.
 */
public class HelpText extends NodeBaseVO {

    private static final long serialVersionUID = 1L;

    public HelpText(WmNode node) {
        this.node = node;
    }

    public String getType() {
        return getProp(HelpTextModel.Props.TYPE);
    }

    public String getCode() {
        return getProp(HelpTextModel.Props.CODE);
    }

    public String getName() {
        return getProp(HelpTextModel.Props.NAME);
    }
}
