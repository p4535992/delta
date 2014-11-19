package ee.webmedia.alfresco.help.model;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;

/**
 * Wrapper object for HelpText node.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
