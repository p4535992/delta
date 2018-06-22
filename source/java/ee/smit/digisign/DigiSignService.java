package ee.smit.digisign;

public interface DigiSignService {
    String BEAN_NAME = "DigiSignService";

    String getUri();

    String getAppname();

    String getApppass();

    boolean getDigiSignServiceActive();

    int getMaxThreads();

    int getBatchSize();
    }
