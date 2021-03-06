/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.collections.comparators.BooleanComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Sort
 * 
 * Base sorting helper supports locale specific case sensitive, case in-sensitive and
 * numeric data sorting.
 * 
 * @author Kevin Roast
 * <br>----------------------
 */
public abstract class Sort
{
   // ------------------------------------------------------------------------------
   // Construction
   
   /**
    * Constructor
    * 
    * @param data             a the List of String[] data to sort
    * @param column           the column getter method to use on the row to sort
    * @param bForward         true for a forward sort, false for a reverse sort
    * @param mode             sort mode to use (see IDataContainer constants)
    */
   public Sort(List data, String column, boolean bForward, String mode)
   {
      this.data = data;
      this.column = column;
      this.bForward = bForward;
      this.sortMode = mode;
      
      if (this.data.size() != 0)
      {
         // setup the Collator for our Locale
         Collator collator;
         
         // set the strength according to the sort mode
         if (mode.equals(IDataContainer.SORT_CASESENSITIVE))
         {
             throw new RuntimeException("Case-sensitive ordering probably shouldn't be used!");
         }
         else
         {
             collator = AppConstants.getNewCollatorInstance();
         }
         
         this.keys = buildCollationKeys(collator);
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // Abstract Methods
   
   /**
    * Runs the Sort routine on the current dataset
    */
   public abstract void sort();
   
   
   // ------------------------------------------------------------------------------
   // Helper methods
   
   /**
    * Build a list of collation keys for comparing locale sensitive strings or build
    * the appropriate objects for comparison for other standard data types.
    * 
    * @param collator      the Collator object to use to build String keys
    */
   @SuppressWarnings("unchecked")
   protected List buildCollationKeys(Collator collator)
   {
      List data = this.data;
      int iSize = data.size();
      List keys = new ArrayList(iSize);
      
      try
      {
         // create the Bean getter method invoker to retrieve the value for a colunm
         String methodName = getGetterMethodName(this.column);
         Class returnType = null;
         // getter method for first item, used to determine types
         Method getter = null;
         // getter methods for different item classes
         Map<Class, Method> getters = new HashMap<Class, Method>();
         String propMethodName = null;
         // property getter methods for different item classes
         Map<Class, Method> propertiesGetters = new HashMap<Class, Method>();
         // properties getter method for first item, used to determine types
         Method propertiesGetter = null;
         // there will always be at least one item to sort if we get to this method
         Object bean = this.data.get(0);
         Class<? extends Object> beanClass = bean.getClass();
         try
         {
            try {
                getter = beanClass.getMethod(methodName, (Class [])null);
            } catch (NoSuchMethodException e) { // Check for boolean naming convention
                getter = beanClass.getMethod(methodName.replaceFirst("get", "is"), (Class[]) null);
            }
            getters.put(beanClass, getter);
            returnType = getter.getReturnType();
         }
         catch (NoSuchMethodException nsmerr)
         {
             String methodSep = ";";
             if(column.contains(methodSep)){
                 int methodEndIndex = column.indexOf(methodSep);
                 String propertiesGetterMethod = column.substring(0, methodEndIndex);
                 column = column.substring(methodEndIndex + 1);
                 propMethodName = getGetterMethodName(propertiesGetterMethod);
                 try {
                     propertiesGetter = beanClass.getMethod(propMethodName, (Class [])null);
                     propertiesGetters.put(beanClass, propertiesGetter);
                 } catch (NoSuchMethodException error){
                     // no action
                 }
             }
            // no bean getter method found - try Map implementation
            bean = invokeGetter(propertiesGetter, bean);
            if (bean instanceof Map)
            {
               if (column != null && column.contains(":")) {
                   column = QName.createQName(column, BeanHelper.getNamespaceService()).toString();
               }
               Object obj = ((Map)bean).get(this.column);
               
               if(obj == null) { // Right now only first element is tested but that might be null causing the sorting to fall back to comparing strings values of objects
                   for(int i = 1; i < this.data.size() && obj == null; i++) {
                       bean = invokeGetter(propertiesGetter, this.data.get(i));
                       if(bean instanceof Map) {
                           obj = ((Map)bean).get(this.column);
                       }
                   }
               }
               
               if (obj != null)
               {
                  returnType = obj.getClass();
               }
               else
               {
                  if (s_logger.isInfoEnabled())
                  {
                     s_logger.info("Unable to get return type class for RichList column: " + column +
                           ". Suggest set java type directly in sort component tag.");
                  }
                  returnType = Object.class;
               }
            }
            else
            {
               throw new IllegalStateException("Unable to find bean getter or Map impl for column name: " + this.column);
            }
         }
         
         // create appropriate comparator instance based on data type
         // using the strategy pattern so  sub-classes of Sort simply invoke the
         // compare() method on the comparator interface - no type info required
         boolean bknownType = true;
         if (returnType.equals(String.class))
         {
            if (strongStringCompare == true)
            {
               this.comparator = new StringComparator();
            }
            else
            {
               this.comparator = new SimpleStringComparator();
            }
         }
         else if (returnType.equals(Date.class))
         {
            this.comparator = new DateComparator();
         }
         else if (returnType.equals(boolean.class) || returnType.equals(Boolean.class))
         {
            this.comparator = BooleanComparator.getFalseFirstComparator();
         }
         else if (returnType.equals(int.class) || returnType.equals(Integer.class))
         {
            this.comparator = new IntegerComparator();
         }
         else if (returnType.equals(long.class) || returnType.equals(Long.class))
         {
            this.comparator = new LongComparator();
         }
         else if (returnType.equals(float.class) || returnType.equals(Float.class))
         {
            this.comparator = new FloatComparator();
         }
         else if (returnType.equals(Timestamp.class))
         {
            this.comparator = new TimestampComparator();
         }
         else
         {
             if(ClassUtils.getAllInterfaces(returnType).contains(Comparable.class)){
                 this.comparator = new SimpleComparableComparator();
             }else{
                 s_logger.warn("Unsupported sort data type: " + returnType + " defaulting to .toString()");
                 this.comparator = new SimpleComparator();
                 bknownType = false;
             }
         }
         
         // create a collation key for each required column item in the dataset
         for (int iIndex=0; iIndex<iSize; iIndex++)
         {
            Object obj;
            Object dataItem = data.get(iIndex);
            Class<? extends Object> dataItemClass = dataItem.getClass();
            if (getter != null)
            {
                Method itemGetter = getItemMethod(methodName, getters, dataItemClass);
               // if we have a bean getter method impl use that
               try
               {
                  getter.setAccessible(true);
               }
               catch (SecurityException se)
               {
               }
               obj = itemGetter.invoke(data.get(iIndex), (Object [])null);
            }
            else
            {
               if (propertiesGetter != null){
                   Method propGetter = getItemMethod(propMethodName, propertiesGetters, dataItemClass);
                   dataItem = propGetter.invoke(dataItem); 
               } else if (dataItem instanceof Node) {
                   Node node = (Node) dataItem;
                   dataItem = node.getProperties();
               }
               // else we must have a bean Map impl
               obj = ((Map) dataItem).get(column);
            }
            
            if (obj instanceof String)
            {
               String str = (String)obj;
               if (strongStringCompare == true)
               {
                  if (str.indexOf(' ') != -1)
                  {
                     // quote white space characters or they will be ignored by the Collator!
                     int iLength = str.length();
                     StringBuilder s = new StringBuilder(iLength + 4);
                     char c;
                     for (int i=0; i<iLength; i++)
                     {
                        c = str.charAt(i);
                        if (c != ' ')
                        {
                           s.append(c);
                        }
                        else
                        {
                           s.append('\'').append(c).append('\'');
                        }
                     }
                     str = s.toString();
                  }
                  keys.add(collator.getCollationKey(str));
               }
               else
               {
                  keys.add(str);
               }
            }
            else if (bknownType == true)
            {
               // the appropriate wrapper object will be create by the reflection
               // system to wrap primative types e.g. int and boolean.
               // therefore the correct type will be ready for use by the comparator
               keys.add(obj);
            }
            else
            {
               if (obj != null)
               {
                  keys.add(obj.toString());
               }
               else
               {
                  keys.add(null);
               }
            }
         }
      }
      catch (Exception err)
      {
         throw new RuntimeException(err);
      }
      
      return keys;
   }


private Object invokeGetter(Method propertiesGetter, Object bean) throws IllegalAccessException, InvocationTargetException {
    if (propertiesGetter != null){
        bean = propertiesGetter.invoke(bean);
    } else if (bean instanceof Node) {
        bean = ((Node) bean).getProperties();
    }
    return bean;
}


private Method getItemMethod(String propMethodName, Map<Class, Method> propertiesGetters, Class<? extends Object> dataItemClass) {
    Method propGetter = propertiesGetters.get(dataItemClass);
       if (propGetter == null) {
           try {
               propGetter = dataItemClass.getMethod(propMethodName, (Class[]) null);
               propertiesGetters.put(dataItemClass, propGetter);
           } catch (NoSuchMethodException error) {
               throw new RuntimeException("No method " + propMethodName + " for class " + dataItemClass);
           }
       }
    return propGetter;
}
   
   /**
    * Given the array and two indices, swap the two items in the
    * array.
    */
   @SuppressWarnings("unchecked")
   protected void swap(final List v, final int a, final int b)
   {
      Object temp = v.get(a);
      v.set(a, v.get(b));
      v.set(b, temp);
   }
   
   /**
    * Return the comparator to be used during column value comparison
    * 
    * @return Comparator for the appropriate column data type
    */
   protected Comparator getComparator()
   {
      return this.comparator;
   }
   
   /**
    * Return the name of the Bean getter method for the specified getter name
    * 
    * @param name of the field to build getter method name for e.g. "value"
    * 
    * @return the name of the Bean getter method for the field name e.g. "getValue"
    */
   protected static String getGetterMethodName(String name)
   {
      return "get" + name.substring(0, 1).toUpperCase() +
             name.substring(1, name.length());
   }
   
   
   // ------------------------------------------------------------------------------
   // Inner classes for data type comparison
   
   private static class SimpleComparator implements Comparator
   {
      /**
       * @see org.alfresco.web.data.IDataComparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return (obj1.toString()).compareTo(obj2.toString());
      }
   }
   
   private static class SimpleComparableComparator implements Comparator
   {      
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return ((Comparable)obj1).compareTo(obj2);
      }
   }
   
   private static class SimpleStringComparator implements Comparator
   {
      /**
       * @see org.alfresco.web.data.IDataComparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return ((String)obj1).compareToIgnoreCase((String)obj2);
      }
   }
   
   private static class StringComparator implements Comparator
   {
      /**
       * @see org.alfresco.web.data.IDataComparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return ((CollationKey)obj1).compareTo((CollationKey)obj2);
      }
   }
   
   private static class IntegerComparator implements Comparator
   {
      /**
       * @see org.alfresco.web.data.IDataComparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return ((Integer)obj1).compareTo((Integer)obj2);
      }
   }
   
   private static class FloatComparator implements Comparator
   {
      /**
       * @see org.alfresco.web.data.IDataComparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return ((Float)obj1).compareTo((Float)obj2);
      }
   }
   
   private static class LongComparator implements Comparator
   {
      /**
       * @see org.alfresco.web.data.IDataComparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return ((Long)obj1).compareTo((Long)obj2);
      }
   }
   
   private static class DateComparator implements Comparator
   {
      /**
       * @see org.alfresco.web.data.IDataComparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return ((Date)obj1).compareTo((Date)obj2);
      }
   }
   
   private static class TimestampComparator implements Comparator
   {
      /**
       * @see org.alfresco.web.data.IDataComparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final Object obj1, final Object obj2)
      {
         if (obj1 == null && obj2 == null) return 0;
         if (obj1 == null) return -1;
         if (obj2 == null) return 1;
         return ((Timestamp)obj1).compareTo((Timestamp)obj2);
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // Private Data
   
   /** list of Object[] data to sort */
   protected List data;
   
   /** column name to sort against */
   protected String column;
   
   /** sort direction */
   protected boolean bForward;
   
   /** sort mode (see IDataContainer constants) */
   protected String sortMode;
   
   /** locale sensitive collator */
   protected Collator collator;
   
   /** collation keys for comparisons */
   protected List keys = null;
   
   /** the comparator instance to use for comparing values when sorting */
   private Comparator comparator = null;
   
   // TODO: make this configurable
   /** config value whether to use strong collation Key string comparisons */
   private boolean strongStringCompare = true;
   
   private static Log    s_logger = LogFactory.getLog(IDataContainer.class);
   
} // end class Sort
