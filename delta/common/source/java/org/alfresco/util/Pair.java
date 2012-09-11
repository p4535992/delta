/**
 * 
 */
package org.alfresco.util;

import java.io.Serializable;

import org.apache.commons.lang.ObjectUtils;

/**
 * Utility class for containing two things that aren't like each other
 * @author britt
 */
public final class Pair<F, S> implements Serializable
{
    private static final long serialVersionUID = -7406248421185630612L;
    
    /**
     * The first member of the pair.
     */
    private F fFirst;
    
    /**
     * The second member of the pair.
     */
    private S fSecond;
    
    /**
     * Make a new one.
     * 
     * @param first The first member.
     * @param second The second member.
     */
    public Pair(F first, S second)
    {
        fFirst = first;
        fSecond = second;
    }

    public static <F, S> Pair<F, S> newInstance(F first, S second)
    {
        return new Pair<F, S>(first, second);
    }

    /**
     * Get the first member of the tuple.
     * @return The first member.
     */
    public F getFirst()
    {
        return fFirst;
    }
    
    /**
     * Get the second member of the tuple.
     * @return The second member.
     */
    public S getSecond()
    {
        return fSecond;
    }
    
    public void setFirst(F first)
    {
        fFirst = first;
    }
    
    public void setSecond(S second)
    {
        fSecond = second;
    }
    
    /**
     * Override of equals.
     * @param other The thing to compare to.
     * @return equality.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof Pair))
        {
            return false;
        }
        Pair o = (Pair)other;
        return ObjectUtils.equals(fFirst, o.getFirst()) && ObjectUtils.equals(fSecond, o.getSecond());
    }
    
    /**
     * Override of hashCode.
     */
    @Override
    public int hashCode()
    {
        return ObjectUtils.hashCode(fFirst) + ObjectUtils.hashCode(fSecond);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "(" + fFirst + ", " + fSecond + ")";
    }
}
