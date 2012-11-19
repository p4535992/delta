package ee.webmedia.alfresco.monitoring;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.app.servlet.BaseServlet;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.ISO8601DateConverter;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.monitoring.model.Application;
import ee.webmedia.alfresco.monitoring.model.ServiceStatus;
import ee.webmedia.alfresco.monitoring.model.Status;

/**
 * @author Keit Tehvan
 */
public class MonitoringServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/xml;charset=UTF-8");
        setNoCacheHeaders(response);

        Status status = new Status();
        Application application = new Application();
        application.setVersion(BeanHelper.getApplicationService().getProjectVersion());
        application.setServer(request.getServerName() + ":" + request.getServerPort());
        application.setDateTime(new Date());
        status.setApplication(application);

        status.setServices(MonitoringUtil.getStatusList());

        XStream xStream = new XStream();
        xStream.alias("service", ServiceStatus.class);
        xStream.alias("status", Status.class);
        xStream.registerConverter(new ISO8601DateConverter());
        response.getWriter().print(xStream.toXML(status));

    }

}
