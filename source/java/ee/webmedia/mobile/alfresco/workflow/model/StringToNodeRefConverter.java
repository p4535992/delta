package ee.webmedia.mobile.alfresco.workflow.model;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.convert.converter.Converter;

public class StringToNodeRefConverter implements Converter<String, NodeRef> {

    @Override
    public NodeRef convert(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        return new NodeRef(source);

    }
}