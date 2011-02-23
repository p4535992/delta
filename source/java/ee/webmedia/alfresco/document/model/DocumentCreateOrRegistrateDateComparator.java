package ee.webmedia.alfresco.document.model;

import java.util.Comparator;

public class DocumentCreateOrRegistrateDateComparator implements Comparator<Document> {
    
    private static DocumentCreateOrRegistrateDateComparator comparator = new DocumentCreateOrRegistrateDateComparator();

    @Override
    public int compare(Document doc1, Document doc2) {
        if(doc1.getRegDateTime() != null && doc2.getRegDateTime() == null){
            return 1;
        }
        else if(doc1.getRegDateTime() == null && doc2.getRegDateTime() != null){
            return -1;
        }
        else if(doc1.getRegDateTime() != null && doc2.getRegDateTime() != null){
            return doc2.getRegDateTime().compareTo(doc1.getRegDateTime());
        }
        if(doc1.getCreated() != null && doc2.getCreated() != null){
            return doc2.getCreated().compareTo(doc1.getCreated());
        }
        //these cases should't actually happen
        else if(doc1.getCreated() != null && doc2.getCreated() == null){
            return -1;
        }
        else if(doc1.getCreated() == null && doc2.getCreated() != null){
            return 1;
        }
        return 0;
    }

    public static DocumentCreateOrRegistrateDateComparator getComparator() {
        return comparator;
    }

}
