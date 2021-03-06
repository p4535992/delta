package ee.webmedia.alfresco.utils;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Comparator that compares two collections based on comparing elements at the same index. <br>
 * If <code>elementComparator</code> is given at construction time, then elementComparator is used, otherwise when element implements {@link Comparable}, then it will be used. <br>
 * Note - it is not jet thoroughly tested (at first created to double validate equality)
 */
public class CollectionComparator<E> extends AbstractCollection<E> implements Comparator<Collection<E>>, Comparable<Collection<E>> {
    private Collection<E> wrappedCollection;
    private Comparator<E> elementComparator;

    public CollectionComparator(Collection<E> c) {
        this(c, null);
    }

    public CollectionComparator(Collection<E> c, Comparator<E> elementComparator) {
        this.wrappedCollection = Collections.unmodifiableCollection(c);
        this.elementComparator = elementComparator;
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
            E o2 = e2.next();
            if (elementComparator != null) {
                int res = elementComparator.compare(o1, o2);
                if (res != 0) {
                    return res;
                }
            }
            {
                if (o1 == null) {
                    if (o2 != null) {
                        return -1;
                    }
                } else {
                    boolean isComparable1 = o1 instanceof Comparable;
                    boolean isComparable2 = o2 instanceof Comparable;
                    if (isComparable1 == isComparable2) {
                        if (isComparable1) {
                            @SuppressWarnings("unchecked")
                            Comparable<E> comparable1 = (Comparable<E>) o1;
                            @SuppressWarnings("unchecked")
                            Comparable<E> comparable2 = (Comparable<E>) o2;
                            return comparable1.compareTo((E) comparable2);
                        }
                        // neither comparable - this probably should have been constructed with custom elementComparator
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
