package ee.webmedia.alfresco.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.webmedia.alfresco.monitoring.model.ServiceStatus;

public class MonitoringUtil {

    private static Map<MonitoredService, ServiceStatus> statusHolder;
    static {
        statusHolder = new HashMap<MonitoredService, ServiceStatus>();
        for (MonitoredService service : MonitoredService.values()) {
            statusHolder.put(service, new ServiceStatus(service.name()));
        }
    }

    public static void logSuccess(MonitoredService service) {
        statusHolder.get(service).increaseSuccessCount();
    }

    public static void logError(MonitoredService service, Throwable e) {
        logError(service, e.toString());
    }

    public static void logError(MonitoredService service, String string) {
        statusHolder.get(service).increaseErrorCount(string);
    }

    public static List<ServiceStatus> getStatusList() {
        return new ArrayList<ServiceStatus>(statusHolder.values());
    }

}
