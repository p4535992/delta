package ee.webmedia.alfresco.common.richlist;

import java.io.Serializable;
import java.util.Map;

public interface PageLoadCallback<Key, Value> extends Serializable {

    void doWithPageItems(Map<Key, Value> loadedRows);

}
