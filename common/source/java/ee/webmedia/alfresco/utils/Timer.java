<<<<<<< HEAD
package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple helper for time logging.
 */
public class Timer {
    String name;
    long time = System.currentTimeMillis();
    List<Timer> subs;

    public Timer() {
        this("It");
    }

    public Timer(String name) {
        this.name = name;
    }

    public long time() {
        return System.currentTimeMillis() - time;
    }

    public Timer sub() {
        return sub("It");
    }

    public Timer sub(String name) {
        Timer sub = new Timer(name);
        if (subs == null) {
            subs = new ArrayList<Timer>();
        }
        subs.add(sub);
        return this;
    }

    @Override
    public String toString() {
        String s = String.format("%s (%d ms)", name, time());
        if (subs != null) {
            StringBuilder sb = new StringBuilder(s);
            for (Timer sub : subs) {
                sb.append(" : ").append(sub);
            }
            return sb.toString();
        }
        return s;
    }
}
=======
package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple helper for time logging.
 */
public class Timer {
    String name;
    long time = System.currentTimeMillis();
    List<Timer> subs;

    public Timer() {
        this("It");
    }

    public Timer(String name) {
        this.name = name;
    }

    public long time() {
        return System.currentTimeMillis() - time;
    }

    public Timer sub() {
        return sub("It");
    }

    public Timer sub(String name) {
        Timer sub = new Timer(name);
        if (subs == null) {
            subs = new ArrayList<Timer>();
        }
        subs.add(sub);
        return this;
    }

    @Override
    public String toString() {
        String s = String.format("%s (%d ms)", name, time());
        if (subs != null) {
            StringBuilder sb = new StringBuilder(s);
            for (Timer sub : subs) {
                sb.append(" : ").append(sub);
            }
            return sb.toString();
        }
        return s;
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
