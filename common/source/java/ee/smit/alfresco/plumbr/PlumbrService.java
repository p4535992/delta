package ee.smit.alfresco.plumbr;

public interface PlumbrService {
    String BEAN_NAME = "plumbrService";


    Boolean isPlumbrActive();

    String getPlumbrScriptSrc();

    String getPlumbrAccountId();

    String getPlumbrAppName();

    String getPlumbrServerUrl();


}
