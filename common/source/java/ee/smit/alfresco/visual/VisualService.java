package ee.smit.alfresco.visual;

import java.util.List;

public interface VisualService {
    String BEAN_NAME = "visualService";

    public List<String> getUsernamesList();

    public Boolean isVisualUserName();

}
