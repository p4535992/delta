package ee.smit.adit;

public interface AditAdapterService {
    String BEAN_NAME = "AditAdapterService";

    String getUri();

    boolean isAditAdapterActive();

    String getRegCode();
}
