package ee.webmedia.alfresco.parameters.service;

import java.util.List;

import org.alfresco.util.BaseAlfrescoSpringTest;

import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.Parameters;

public class ParametersServiceImplTest extends BaseAlfrescoSpringTest {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParametersServiceImplTest.class);

    private ParametersService parametersService;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
    }

    public void testGetParameter() throws Exception {
        final Long value = parametersService.getLongParameter(Parameters.DVK_MAX_RECEIVE_DOCUMENTS_NR);
        assertEquals(Long.valueOf(50), value);
    }

    public void testGetAllParameters() throws Exception {
        final List<Parameter<?>> allParameters = parametersService.getAllParameters();
        assertTrue(allParameters.size() > 0);
        for (Parameter<?> parameter : allParameters) {
            log.debug("parameter: " + parameter);
        }
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

}
