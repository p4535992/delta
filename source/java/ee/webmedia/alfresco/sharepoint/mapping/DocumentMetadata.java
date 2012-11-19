package ee.webmedia.alfresco.sharepoint.mapping;

import static org.apache.commons.lang.StringUtils.trimToNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.sharepoint.DocumentImporter.DocumentHistory;
import ee.webmedia.alfresco.sharepoint.DocumentImporter.VolumeCase;
import ee.webmedia.alfresco.sharepoint.ImportSettings;
import ee.webmedia.alfresco.sharepoint.ImportUtil;
import ee.webmedia.alfresco.sharepoint.ImportValidationException;

public abstract class DocumentMetadata {

    protected String documentType;

    protected String direction;

    protected String subtype;

    protected String originalLocation;

    protected String originalName;

    protected String tempName;

    protected Boolean makePublic;

    protected String creator;

    protected Date created;

    protected String modifier;

    protected Date modified;

    protected List<ImportFile> files = new ArrayList<ImportFile>(1);

    protected List<String> assocs;

    protected List<DocumentHistory> history;

    public static DocumentMetadata create(Element docRoot, File dirFiles, GeneralMappingData generalSettings, ImportSettings settings) {
        if (settings.isSharepointOrigin()) {
            return new SharepointMetadata(docRoot, generalSettings, dirFiles);
        } else if (settings.isAmphoraOrigin()) {
            return new AmphoraMetadata(docRoot, dirFiles);
        } else if (settings.isRiigikohusOrigin()) {
            return new RiigikohusMetadata(docRoot, generalSettings, dirFiles);
        } else {
            throw new RuntimeException("Import origin not supported: " + settings.getStructAndDocsOrigin());
        }
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDirection() {
        return direction;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getOriginalLocation() {
        return originalLocation;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Boolean isMakePublic() {
        return makePublic;
    }

    public Date getCreated() {
        return created;
    }

    public String getCreator() {
        return creator;
    }

    public Date getModified() {
        return modified;
    }

    public String getModifier() {
        return modifier;
    }

    public List<ImportFile> getFiles() {
        return files;
    }

    public List<String> getAssocs() {
        return assocs;
    }

    public abstract QName getAssocType();

    public abstract boolean isDocumentAssocSource();

    public abstract NodeRef resolveVolumeRef(Map<String, Set<VolumeCase>> volumeCases) throws ImportValidationException;

    public List<DocumentHistory> getHistory() {
        return history;
    }

    public String getVolumeURL() {
        return StringUtils.substringBeforeLast(originalLocation, "/");
    }

    private static class SharepointMetadata extends DocumentMetadata {

        public SharepointMetadata(Element docRoot, GeneralMappingData generalSettings, File dirFiles) {
            documentType = trimToNull(docRoot.elementTextTrim(generalSettings.getDocumentTypeElement()));
            direction = trimToNull(docRoot.elementTextTrim(generalSettings.getDirectionElement()));
            subtype = trimToNull(docRoot.elementTextTrim(generalSettings.getSubtypeElement()));

            originalLocation = trimToNull(docRoot.elementTextTrim("originalLocation"));
            originalName = trimToNull(docRoot.elementTextTrim("originalName"));
            tempName = trimToNull(docRoot.elementTextTrim("tempName"));
            makePublic = Boolean.parseBoolean(docRoot.elementTextTrim("makePublic"));
            created = ImportUtil.getDateTime(docRoot.elementTextTrim("Created"));
            creator = StringUtils.substringAfter(docRoot.elementTextTrim("Created_x0020_By"), "\\");
            modified = ImportUtil.getDateTime(docRoot.elementTextTrim("Modified"));
            modifier = StringUtils.substringAfter(docRoot.elementTextTrim("Modified_x0020_By"), "\\");

            if (tempName != null) {
                files.add(new ImportFile(originalName, new File(dirFiles, tempName), created, creator, modified, modifier));
            }
        }

        @Override
        public NodeRef resolveVolumeRef(Map<String, Set<VolumeCase>> volumeCases) throws ImportValidationException {
            String volumeURL = StringUtils.substringBeforeLast(originalLocation, "/");
            Set<VolumeCase> volumes = volumeCases.get(volumeURL);

            if (volumes == null || volumes.size() != 1) {
                throw new ImportValidationException("Could not find volume/case for document, searched based on URL " + volumeURL, volumes == null || volumes.isEmpty());
            }

            return volumes.iterator().next().getNodeRef();
        }

        @Override
        public QName getAssocType() {
            return null;
        }

        @Override
        public boolean isDocumentAssocSource() {
            return false;
        }
    }

    private static class AmphoraMetadata extends DocumentMetadata {

        public AmphoraMetadata(Element docRoot, File dirFiles) {
            List<Element> children = docRoot.elements();
            Element form = children.get(0).element("Form");

            String tmpSubtype = null;
            Element formIdElem = form.element("Document_form_id");
            if (formIdElem != null) {
                tmpSubtype = trimToNull(formIdElem.elementTextTrim("et"));
            }

            String isPublicTxt = form.elementTextTrim("Is_public");

            documentType = trimToNull(form.attributeValue("et"));
            subtype = tmpSubtype;
            originalLocation = trimToNull(form.element("Parent_id").attributeValue("value"));
            makePublic = ("True".equals(isPublicTxt) || "False".equals(isPublicTxt)) ? "True".equals(isPublicTxt) : null;
            created = ImportUtil.getDateTime(form.elementTextTrim("Create_Time"));
            modified = ImportUtil.getDateTime(form.elementTextTrim("Edit_Time"));

            List<DocumentHistory> tmpHistory = new ArrayList<DocumentHistory>();

            if (form.element("HistoryList") != null) {
                List<Element> historyEntries = form.element("HistoryList").elements("History");
                for (Element entry : historyEntries) {
                    tmpHistory.add(new DocumentHistory(
                            ImportUtil.getDateTimeShort(entry.elementTextTrim("Create_time")),
                            trimToNull(entry.elementTextTrim("Person_name")),
                            trimToNull(entry.elementTextTrim("History_type"))));
                }
                Collections.sort(tmpHistory);
            }

            Element relationList = form.element("RelationList");
            if (relationList != null) {
                Set<String> tmpAssocs = new LinkedHashSet<String>();

                List<Element> relations = relationList.elements("Relation");
                for (Element relation : relations) {
                    tmpAssocs.add(relation.elementTextTrim("RelatedObject_ID"));
                }

                tmpAssocs.remove(null);
                assocs = tmpAssocs.isEmpty() ? null : new ArrayList<String>(tmpAssocs);
            }

            creator = !tmpHistory.isEmpty() ? tmpHistory.get(0).getCreator() : null;
            modifier = !tmpHistory.isEmpty() ? tmpHistory.get(tmpHistory.size() - 1).getCreator() : null;
            history = tmpHistory;

            Element fileElem = form.element("Original_file");
            if (fileElem == null) {
                fileElem = form.element("File");
            }

            if (fileElem != null) {
                File file = new File(dirFiles, fileElem.attributeValue("guid_name"));
                files.add(new ImportFile(fileElem.attributeValue("filename"), file, created, creator, modified, modifier));
            }
        }

        @Override
        public NodeRef resolveVolumeRef(Map<String, Set<VolumeCase>> volumeCases) throws ImportValidationException {
            Set<VolumeCase> volumes = volumeCases.get(originalLocation);
            NodeRef volumeRef = null;
            int count = 0;

            if (volumes != null) {
                for (VolumeCase volumeCase : volumes) {
                    if (volumeCase.isInVolumePeriod(created)) {
                        volumeRef = volumeCase.getNodeRef();
                        count++;
                    }
                }
            }

            if (count != 1) {
                throw new ImportValidationException("Could not find volume/case for document, searched based on Parent_id value " + originalLocation + " and Create_Time "
                        + ImportUtil.formatDateTime(created), count == 0);
            }

            return volumeRef;
        }

        @Override
        public QName getAssocType() {
            return DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
        }

        @Override
        public boolean isDocumentAssocSource() {
            return true;
        }
    }

    private static class RiigikohusMetadata extends DocumentMetadata {

        private final String function;

        private final String series;

        private final String volume;

        public RiigikohusMetadata(Element docRoot, GeneralMappingData generalMapping, File dirFiles) {
            documentType = trimToNull(docRoot.elementTextTrim(generalMapping.getDocumentTypeElement()));
            function = trimToNull(docRoot.elementTextTrim("funktsioon"));
            series = trimToNull(docRoot.elementTextTrim("sari"));
            volume = trimToNull(docRoot.elementTextTrim("toimik"));

            List<Element> failid = docRoot.elements("failid");
            if (!failid.isEmpty()) {
                failid = failid.get(0).elements("failid_ROW");
                for (Element fail : failid) {
                    files.add(new ImportFile(fail.elementTextTrim("pealkiri"), new File(dirFiles, fail.elementTextTrim("abipealkiri"))));
                }
            }

            Element relationList = docRoot.element("seosed");
            if (relationList != null) {
                Set<String> tmpAssocs = new LinkedHashSet<String>();

                List<Element> relations = relationList.elements("seotud_dok_nr");
                for (Element relation : relations) {
                    tmpAssocs.add(relation.getTextTrim());
                }

                tmpAssocs.remove(null);
                assocs = tmpAssocs.isEmpty() ? null : new ArrayList<String>(tmpAssocs);
            }
        }

        @Override
        public NodeRef resolveVolumeRef(Map<String, Set<VolumeCase>> volumeCases) throws ImportValidationException {
            NodeRef volumeRef = null;
            int count = 0;

            for (Set<VolumeCase> volumeCasesEntry : volumeCases.values()) {
                for (VolumeCase volumeCase : volumeCasesEntry) {
                    if (volumeCase.isLocation(function, series, volume)) {
                        volumeRef = volumeCase.getNodeRef();
                        count++;
                    }
                }
            }

            if (count > 1) {
                throw new ImportValidationException("Found several volumes/cases for document, searched based on function mark " + function + ", series identifier " + series
                        + " and created date/year " + volume);
            } else if (count == 0) {
                throw new ImportValidationException("Could not find volume/case for document, searched based on function mark " + function + ", series identifier " + series
                        + " and created date/year " + volume, true);
            }

            return volumeRef;
        }

        @Override
        public QName getAssocType() {
            if (!"outgoingLetter".equals(documentType) && !"incomingLetter".equals(documentType)) {
                return null;
            }
            return DocumentCommonModel.Assocs.DOCUMENT_REPLY;
        }

        @Override
        public boolean isDocumentAssocSource() {
            return "outgoingLetter".equals(documentType);
        }
    }

}
