package ee.webmedia.alfresco.common.service;

import java.util.List;

public interface PropsConditionCallback {

    void addAdditionalConditions(StringBuilder sqlQuery, List<Object> arguments);

}
