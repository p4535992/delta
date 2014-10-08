<<<<<<< HEAD
package ee.webmedia.alfresco.volume.model;

import java.util.Date;
import java.util.Map;

/**
 * @author Keit Tehvan
 */
public interface VolumeOrCaseFile extends Comparable<VolumeOrCaseFile> {
    String getVolumeMark();

    String getTitle();

    Date getValidFrom();

    boolean isDynamic();

    Map<String, Object> getConvertedPropsMap();

    String getHierarchicalKeywords();

    String getFunctionLabel();

    String getSeriesLabel();

    Map<String, Object> getUnitStrucPropsConvertedMap();

    String getType();

    Map<String, Object> getProperties();

}
=======
package ee.webmedia.alfresco.volume.model;

import java.util.Date;
import java.util.Map;

public interface VolumeOrCaseFile extends Comparable<VolumeOrCaseFile> {
    String getVolumeMark();

    String getTitle();

    Date getValidFrom();

    boolean isDynamic();

    Map<String, Object> getConvertedPropsMap();

    String getHierarchicalKeywords();

    String getFunctionLabel();

    String getSeriesLabel();

    Map<String, Object> getUnitStrucPropsConvertedMap();

    String getType();

    Map<String, Object> getProperties();

}
>>>>>>> develop-5.1
