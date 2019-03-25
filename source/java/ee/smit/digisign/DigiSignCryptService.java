package ee.smit.digisign;

public interface DigiSignCryptService {
    String BEAN_NAME = "DigiSignCryptService";

    String getUri();

    String getAppname();

    String getApppass();

    boolean getActive();

}
