package ee.webmedia.alfresco.utils;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Note - it is not jet thoroughly tested (at first created to double validate equality)
 * 
 * @author Ats Uiboupin
 */
public class CollectionComparator<E> extends AbstractCollection<E> implements Comparator<Collection<E>>, Comparable<Collection<E>> {
    Collection<E> wrappedCollection;

    public CollectionComparator(Collection<E> c) {
        this.wrappedCollection = Collections.unmodifiableCollection(c);
    }

    @Override
    public int compareTo(Collection<E> o) {
        return compare(wrappedCollection, o);
    }

    @Override
    public int compare(Collection<E> self, Collection<E> o) {
        // based on AbstractList.equals(Object)
        if (o == self) {
            return 0;
        }
        Iterator<E> e1 = self.iterator();
        Iterator<E> e2 = o.iterator();
        while (e1.hasNext() && e2.hasNext()) {
            E o1 = e1.next();
            Object o2 = e2.next();
            {
                if (o1 == null) {
                    if (o2 != null) {
                        return -1;
                    }
                } else {
                    boolean isComparable1 = o1 instanceof Comparable;
                    boolean isComparable2 = o2 instanceof Comparable;
                    if (isComparable1 && isComparable2) {
                        @SuppressWarnings("unchecked")
                        Comparable<E> comparable1 = (Comparable<E>) o1;
                        @SuppressWarnings("unchecked")
                        Comparable<E> comparable2 = (Comparable<E>) o2;
                        return comparable1.compareTo((E) comparable2);
                    }
                    if (isComparable1) {
                        return -1;
                    }
                    if (isComparable2) {
                        return 1;
                    }
                }
            }
        }
        boolean hasNext1 = e1.hasNext();
        boolean hasNext2 = e2.hasNext();
        if (!hasNext1 && !hasNext2) {
            return 0;
        }
        if (hasNext1) {
            return -1;
        }
        if (hasNext2) {
            return 1;
        }
        return 0;
    }

    @Override
    public Iterator<E> iterator() {
        return wrappedCollection.iterator();
    }

    @Override
    public int size() {
        return wrappedCollection.size();
    }

}
