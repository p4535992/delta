package ee.webmedia.alfresco.monitoring.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Keit Tehvan
 */
public class Status {
    private Application application;
    private List<ServiceStatus> services;

    public void setServices(List<ServiceStatus> services) {
        this.services = services;
    }

    public List<ServiceStatus> getServices() {
        if (services == null) {
            services = new ArrayList<ServiceStatus>();
        }
        return services;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Application getApplication() {
        return application;
    }

}
