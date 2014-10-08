<<<<<<< HEAD
package ee.webmedia.alfresco.menu.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author Kaarel Jõgeva
 */
@XStreamAlias("menu")
public class Menu implements Serializable {

    @XStreamOmitField
    private static final long serialVersionUID = 0L;
    @XStreamImplicit
    private List<MenuItem> subItems;

    public void addMenuItems(MenuItem menuItem) {
        if (subItems == null) {
            subItems = new ArrayList<MenuItem>();
        }

        subItems.add(menuItem);
    }

    @Override
    public String toString() {
        return Integer.toString(subItems.size());
    }

    public List<MenuItem> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<MenuItem> children) {
        subItems = children;
    }

=======
package ee.webmedia.alfresco.menu.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("menu")
public class Menu implements Serializable {

    @XStreamOmitField
    private static final long serialVersionUID = 0L;
    @XStreamImplicit
    private List<MenuItem> subItems;

    public void addMenuItems(MenuItem menuItem) {
        if (subItems == null) {
            subItems = new ArrayList<MenuItem>();
        }

        subItems.add(menuItem);
    }

    @Override
    public String toString() {
        return Integer.toString(subItems.size());
    }

    public List<MenuItem> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<MenuItem> children) {
        subItems = children;
    }

>>>>>>> develop-5.1
}