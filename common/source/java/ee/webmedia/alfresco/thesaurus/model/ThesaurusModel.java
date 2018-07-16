package ee.webmedia.alfresco.thesaurus.model;

import org.alfresco.service.namespace.QName;

public interface ThesaurusModel {
    String URI = "http://alfresco.webmedia.ee/model/thesaurus/1.0";
    String NAMESPACE_PREFFIX = "the:";

    interface Repo {
        final static String THESAURI_PARENT = "/";
        final static String THESAURI_SPACE = THESAURI_PARENT + NAMESPACE_PREFFIX + Types.THESAURI_ROOT.getLocalName();
    }

    interface Types {
        QName THESAURI_ROOT = QName.createQName(URI, "thesauri");
        QName THESAURUS = QName.createQName(URI, "thesaurus");
        QName HIERARCHICAL_KEYWORD = QName.createQName(URI, "hierarchicalKeyword");
    }

    interface Prop {
        QName NAME = QName.createQName(URI, "name");
        QName DESCRIPTION = QName.createQName(URI, "description");

        QName KEYWORD_LEVEL_1 = QName.createQName(URI, "keywordLevel1");
        QName KEYWORD_LEVEL_2 = QName.createQName(URI, "keywordLevel2");
    }

    interface Assoc {
        QName THESAURUS = Types.THESAURUS;
        QName HIERARCHICAL_KEYWORD = Types.HIERARCHICAL_KEYWORD;
    }

}
