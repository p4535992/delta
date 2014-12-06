package ee.webmedia.alfresco.common.web;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;

public class WeakReferenceSerializable implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private WeakReference wr;

    public WeakReferenceSerializable(Object referent) {
        wr = new WeakReference(referent);
    }

    public Object get() {
        return wr.get();
    }

    /**
     * Write only content of WeakReference. WeakReference itself is not seriazable.
     *
     * @param out
     * @throws java.io.IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(wr.get());
    }

    /**
     * Read saved content of WeakReference and construct new WeakReference.
     *
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        wr = new WeakReference(in.readObject());
    }
}