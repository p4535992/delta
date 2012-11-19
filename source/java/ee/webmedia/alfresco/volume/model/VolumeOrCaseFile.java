package ee.webmedia.alfresco.volume.model;

import java.util.Map;

/**
 * @author Keit Tehvan
 */
public interface VolumeOrCaseFile extends Comparable<VolumeOrCaseFile> {
    String getVolumeMark();

    String getTitle();

    boolean isDynamic();

    Map<String, Object> getConvertedPropsMap();

    String getHierarchicalKeywords();

    String getFunctionLabel();

    String getSeriesLabel();

    Map<String, Object> getUnitStrucPropsConvertedMap();

    String getType();

}
