package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation of a tree data structure.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class TreeNode<T> {

    private final T data;
    private final List<TreeNode<T>> children = new ArrayList<TreeNode<T>>();

    public TreeNode(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

}
