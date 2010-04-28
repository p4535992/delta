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
package org.alfresco.web.ui.common.component.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import javax.transaction.UserTransaction;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ViewsConfigElement;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.ui.common.renderer.data.IRichListRenderer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Kevin Roast
 */
public class UIRichList extends UIComponentBase implements IDataContainer,Serializable,NamingContainer
{
   // ------------------------------------------------------------------------------
   // Construction
   
   private static final long serialVersionUID = 4302199745018058173L;

   /**
    * Default constructor
    */
   public UIRichList()
   {
      setRendererType("org.alfresco.faces.RichListRenderer");
      
      // get the list of views from the client configuration
      ViewsConfigElement viewsConfig = (ViewsConfigElement)Application.getConfigService(
            FacesContext.getCurrentInstance()).getConfig("Views").
            getConfigElement(ViewsConfigElement.CONFIG_ELEMENT_ID);
      List<String> views = viewsConfig.getViews();
      
      // instantiate each renderer and add to the list
      for (String view : views)
      {
         try
         {
            Class clazz = Class.forName(view);
            IRichListRenderer renderer = (IRichListRenderer)clazz.newInstance();
            viewRenderers.put(renderer.getViewModeID(), renderer);
            
            if (logger.isDebugEnabled())
               logger.debug("Added view '" + renderer.getViewModeID() + "' to UIRichList");
         }
         catch (Exception e)
         {
            if (logger.isWarnEnabled())
            {
               logger.warn("Failed to create renderer: " + view, e);
            }
         }
      }
   }


   // ------------------------------------------------------------------------------
   // Component implementation
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.Data";
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.currentPage = ((Integer)values[1]).intValue();
      this.sortColumn = (String)values[2];
      this.sortDescending = ((Boolean)values[3]).booleanValue();
      this.value = values[4];                      // not serializable!
      this.dataModel = (IGridDataModel)values[5];  // not serializable!
      this.viewMode = (String)values[6];
      this.pageSize = ((Integer)values[7]).intValue();
      this.initialSortColumn = (String)values[8];
      this.initialSortDescending = ((Boolean)values[9]).booleanValue();
      this.refreshOnBind = ((Boolean)values[10]).booleanValue();
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[] {
            // standard component attributes are saved by the super class
            super.saveState(context),
            Integer.valueOf(this.currentPage),
            this.sortColumn,
            (this.sortDescending ? Boolean.TRUE : Boolean.FALSE),
            this.value,
            this.dataModel,
            this.viewMode,
            Integer.valueOf(this.pageSize),
            this.initialSortColumn,
            (this.initialSortDescending ? Boolean.TRUE : Boolean.FALSE),
            this.refreshOnBind};
      
      return (values);
   }
   
   /**
    * Get the value (for this component the value is an object used as the DataModel)
    *
    * @return the value
    */
   public Object getValue()
   {
      if (this.value == null)
      {
         ValueBinding vb = getValueBinding("value");
         if (vb != null)
         {
            this.value = vb.getValue(getFacesContext());
         }
      }
      return this.value;
   }

   /**
    * Set the value (for this component the value is an object used as the DataModel)
    *
    * @param value     the value
    */
   public void setValue(Object value)
   {
      this.dataModel = null;
      this.value = value;

      // When the data model is cleared it is also necessary to
      // clear the saved row state, as there is an implicit 1:1
      // relation between objects in the _rowStates and the
      // corresponding DataModel element.
      _rowStates.clear();
   }
   
   /**
    * Clear the current sorting settings back to the defaults
    */
   public void clearSort()
   {
      this.sortColumn = null;
      this.sortDescending = true;
      this.initialSortColumn = null;
      this.initialSortDescending = false;
   }
   
   /**
    * Get the view mode for this Rich List
    * 
    * @return view mode as a String
    */
   public String getViewMode()
   {
      ValueBinding vb = getValueBinding("viewMode");
      if (vb != null)
      {
         this.viewMode = (String)vb.getValue(getFacesContext());
      }
      
      return this.viewMode;
   }
   
   /**
    * Set the current view mode for this Rich List
    * 
    * @param viewMode      the view mode as a String
    */
   public void setViewMode(String viewMode)
   {
      this.viewMode = viewMode;
   }
   
   /**
    * Get the refreshOnBind flag.
    *
    * @return the refreshOnBind
    */
   public boolean getRefreshOnBind()
   {
      ValueBinding vb = getValueBinding("refreshOnBind");
      if (vb != null)
      {
         this.refreshOnBind = (Boolean)vb.getValue(getFacesContext());
      }
      return this.refreshOnBind;
   }

   /**
    * Set the refreshOnBind flag. True to force the list to retrieve bound data on bind().
    *
    * @param refreshOnBind     the refreshOnBind
    */
   public void setRefreshOnBind(boolean refreshOnBind)
   {
      this.refreshOnBind = refreshOnBind;
   }
   
   /**
    * Return the UI Component to be used as the "no items available" message
    * 
    * @return UIComponent
    */
   public UIComponent getEmptyMessage()
   {
      return getFacet("empty");
   }
   
   
   // ------------------------------------------------------------------------------
   // IDataContainer implementation 
   
   /**
    * Return the currently sorted column if any
    * 
    * @return current sorted column if any
    */
   public String getCurrentSortColumn()
   {
      return this.sortColumn;
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#isCurrentSortDescending()
    */
   public boolean isCurrentSortDescending()
   {
      return this.sortDescending;
   }
   
   /**
    * @return Returns the initialSortColumn.
    */
   public String getInitialSortColumn()
   {
      return this.initialSortColumn;
   }

   /**
    * @param initialSortColumn The initialSortColumn to set.
    */
   public void setInitialSortColumn(String initialSortColumn)
   {
      this.initialSortColumn = initialSortColumn;
   }

   /**
    * @return Returns the initialSortDescending.
    */
   public boolean isInitialSortDescending()
   {
      return this.initialSortDescending;
   }

   /**
    * @param initialSortDescending The initialSortDescending to set.
    */
   public void setInitialSortDescending(boolean initialSortDescending)
   {
      this.initialSortDescending = initialSortDescending;
   }
   
   /**
    * Returns the current page size used for this list, or -1 for no paging.
    */
   public int getPageSize()
   {
      ValueBinding vb = getValueBinding("pageSize");
      if (vb != null)
      {
         int pageSize = ((Integer)vb.getValue(getFacesContext())).intValue();
         if (pageSize != this.pageSize)
         {
            // force a reset of the current page - else the bind may show a page that isn't there
            setPageSize(pageSize);
         }
      }
      
      return this.pageSize;
   }
   
   /**
    * Sets the current page size used for the list.
    * 
    * @param val
    */
   public void setPageSize(int val)
   {
      if (val >= -1)
      {
         this.pageSize = val;
         setCurrentPage(0);
      }
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#getPageCount()
    */
   public int getPageCount()
   {
      return this.pageCount;
   }
   
   /**
    * Return the current page the list is displaying
    * 
    * @return current page zero based index
    */
   public int getCurrentPage()
   {
      return this.currentPage;
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#setCurrentPage(int)
    */
   public void setCurrentPage(int index)
   {
      this.currentPage = index;
      this.sortOrPageChanged = true;
   }

   /**
    * Returns true if a row of data is available
    * 
    * @return true if data is available, false otherwise
    */
   public boolean isDataAvailable()
   {
      if (this.rowIndex < this.maxRowIndex)
      {
          return true;
      }
      // Reset rowIndex, so that footer components wouldn't have last row index inside their id
      setRowIndex(-1);
      return false;
   }
   
   /**
    * Returns the next row of data from the data model
    * 
    * @return next row of data as a Bean object
    */
   public Object nextRow()
   {
      // increment row count and get next row 
      setRowIndex(rowIndex + 1);
      return getDataModel().getRow(this.rowIndex);
   }

   /**
    * Sort the dataset using the specified sort parameters
    * 
    * @param column        Column to sort
    * @param descending    True for descending sort, false for ascending
    * @param mode          Sort mode to use (see IDataContainer constants)
    */
   public void sort(String column, boolean descending, String mode)
   {
      this.sortColumn = column;
      this.sortDescending = descending;
      this.sortOrPageChanged = true;
      
      // delegate to the data model to sort its contents
      // place in a UserTransaction as we may need to perform a LOT of node calls to complete
      UserTransaction tx = null;
      try
      {
         if (getDataModel().size() > 16)
         {
            FacesContext context = FacesContext.getCurrentInstance();
            tx = Repository.getUserTransaction(context, true);
            tx.begin();
         }
         
         getDataModel().sort(column, descending, mode);
         
         // commit the transaction
         if (tx != null)
         {
            tx.commit();
         }
      }
      catch (Throwable err)
      {
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // UIRichList implementation
   
   /**
    * Method called to bind the RichList component state to the data model value
    */
   public void bind()
   {
       bind(false);
   }

   public void bind(boolean ignoreRefreshOnBind)
   {
      if (!ignoreRefreshOnBind && getRefreshOnBind() == true)
      {
         this.value = null;
         this.dataModel = null;

         // When the data model is cleared it is also necessary to
         // clear the saved row state, as there is an implicit 1:1
         // relation between objects in the _rowStates and the
         // corresponding DataModel element.
         _rowStates.clear();
      }
      int rowCount = getDataModel().size();
      this.absoluteRwCount = rowCount;
      // if a page size is specified, then we use that
      int pageSize = getPageSize();
      if (pageSize != -1 && pageSize != 0)
      {
         // calc total number of pages available
         this.pageCount = (rowCount / this.pageSize) + 1;
         
         if (rowCount % pageSize == 0 && this.pageCount != 1)
         {
            this.pageCount--;
         }
         
         // set currentPage as lastPage if input digit in UIDataPager > pageCount
         if (this.currentPage >= this.pageCount)
         {
            this.currentPage = this.pageCount - 1;
         }
         
         // calc start row index based on current page index
         this.rowIndex = (this.currentPage * pageSize) - 1;
         
         // calc the maximum row index that can be returned
         this.maxRowIndex = this.rowIndex + pageSize;
         if (this.maxRowIndex >= rowCount)
         {
            this.maxRowIndex = rowCount - 1;
         }
      }
      // else we are not paged so show all data from start
      else
      {
         this.rowIndex = -1;
         this.pageCount = 1;
         this.maxRowIndex = (rowCount - 1);
      }
      if (logger.isDebugEnabled())
         logger.debug("Bound datasource: PageSize: " + pageSize + "; CurrentPage: " + this.currentPage + "; RowIndex: " + this.rowIndex + "; MaxRowIndex: " + this.maxRowIndex + "; RowCount: " + rowCount);
   }
   
   /**
    * @return A new IRichListRenderer implementation for the current view mode
    */
   public IRichListRenderer getViewRenderer()
   {
      // get type from current view mode, then create an instance of the renderer
      IRichListRenderer renderer = null;
      if (getViewMode() != null)
      {
         renderer = (IRichListRenderer)viewRenderers.get(getViewMode());
      }
      return renderer;
   }
   
   /**
    * Return the data model wrapper
    * 
    * @return IGridDataModel 
    */
   public IGridDataModel getDataModel()
   {
      if (this.dataModel == null)
      {
         // build the appropriate data-model wrapper object
         Object val = getValue();
         if (val instanceof List)
         {
            this.dataModel = new GridListDataModel((List)val);
         }
         else if ( (java.lang.Object[].class).isAssignableFrom(val.getClass()) )
         {
            this.dataModel = new GridArrayDataModel((Object[])val);
         }
         else
         {
            throw new IllegalStateException("UIRichList 'value' attribute binding should specify data model of a supported type!"); 
         }
         
         // sort first time on initially sorted column if set
         if (this.sortColumn == null)
         {
            String initialSortColumn = getInitialSortColumn();
            if (initialSortColumn != null && initialSortColumn.length() != 0)
            {
               boolean descending = isInitialSortDescending();
               
               // TODO: add support for retrieving correct column sort mode here
               this.sortColumn = initialSortColumn;
               this.sortDescending = descending;
            }
         }
         if (this.sortColumn != null)
         {
            // delegate to the data model to sort its contents
            this.dataModel.sort(this.sortColumn, this.sortDescending, IDataContainer.SORT_CASEINSENSITIVE);
         }
         
         // reset current page
         if (this.sortOrPageChanged == false)
         {
            this.currentPage = 0;
         }
         this.sortOrPageChanged = false;
      }
      
      return this.dataModel;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   /** map of available IRichListRenderer instances */
   private final Map<String, IRichListRenderer> viewRenderers = new HashMap<String, IRichListRenderer>(4, 1.0f);
   
   // component state
   private int currentPage = 0;
   private String sortColumn = null;
   private boolean sortDescending = true;
   private Object value = null;
   private IGridDataModel dataModel = null;
   private String viewMode = null;
   private int pageSize = -1;
   private String initialSortColumn = null;
   private boolean initialSortDescending = false;
   private boolean refreshOnBind = false;
   
   // transient component state that exists during a single page refresh only
   private int rowIndex = -1;
   private int maxRowIndex = -1;
   private int pageCount = 1;
   private boolean sortOrPageChanged = false;
   
   private static Log logger = LogFactory.getLog(IDataContainer.class);

    // ------------------------------------------------------------------------------
    // Alar Kvell: Support EditableValueHolder components (for example UIInput) inside UIRichList
    // Implementation copied from MyFaces class javax.faces.component.UIData class,
    // only few places changed, there are appropriate comments about this

    private static final int PROCESS_DECODES = 1;
    private static final int PROCESS_VALIDATORS = 2;
    private static final int PROCESS_UPDATES = 3;

    // Holds for each row the states of the child components of this UIData.
    // Note that only "partial" component state is saved: the component fields
    // that are expected to vary between rows.
    private Map _rowStates = new HashMap();
    private Object _initialDescendantComponentState = null;

    public int getRowIndex()
    {
        return rowIndex;
    }

    /**
     * Set the current row index that methods like getRowData use.
     * <p>
     * Param rowIndex can be -1, meaning "no row".
     * <p>
     * @param rowIndex
     */
    public void setRowIndex(int rowIndex)
    {
        if (rowIndex < -1)
        {
            throw new IllegalArgumentException("rowIndex is less than -1");
        }

        if (this.rowIndex == rowIndex)
        {
            return;
        }

        FacesContext facesContext = getFacesContext();

// is this fix correct? uirichlist has paging, so we may have initial rowIndex greater than -1 
//        if (this.rowIndex == -1)
//        {
            if (_initialDescendantComponentState == null)
            {
                // Create a template that can be used to initialise any row
                // that we haven't visited before, ie a "saved state" that can
                // be pushed to the "restoreState" method of all the child
                // components to set them up to represent a clean row.
                _initialDescendantComponentState = saveDescendantComponentStates(
                        getColumns(getChildren()).iterator(), false);
            }
//        }
        else
        {
            // We are currently positioned on some row, and are about to
            // move off it, so save the (partial) state of the components
            // representing the current row. Later if this row is revisited
            // then we can restore this state.
            _rowStates.put(getClientId(facesContext),
                    saveDescendantComponentStates(getColumns(getChildren()).iterator(),
                            false));
        }

        this.rowIndex = rowIndex;

        // Alar Kvell: data model and "var" are UIRichList specific
/*
        DataModel dataModel = getDataModel();
        dataModel.setRowIndex(rowIndex);

        String var = getVar();
 */
        String var = (String) getAttributes().get("var");
        if (rowIndex == -1)
        {
            if (var != null)
            {
                facesContext.getExternalContext().getRequestMap().remove(var);
            }
        }
        else
        {
            if (var != null)
            {
                // Alar Kvell: isRowAvailable method is not available on UIRichList data model
/*
                if (isRowAvailable())
                {
                    Object rowData = dataModel.getRowData();
*/
                    Object rowData = getDataModel().getRow(rowIndex);
                    facesContext.getExternalContext().getRequestMap().put(var,
                            rowData);
/*
                }
                else
                {
                    facesContext.getExternalContext().getRequestMap().remove(
                            var);
                }
*/
            }
        }

        if (rowIndex == -1)
        {
            // reset components to initial state
            restoreDescendantComponentStates(getColumns(getChildren()).iterator(),
                    _initialDescendantComponentState, false);
        }
        else
        {
            Object rowState = _rowStates.get(getClientId(facesContext));
            if (rowState == null)
            {
                // We haven't been positioned on this row before, so just
                // configure the child components of this component with
                // the standard "initial" state
                restoreDescendantComponentStates(getColumns(getChildren()).iterator(),
                        _initialDescendantComponentState, false);
            }
            else
            {
                // We have been positioned on this row before, so configure
                // the child components of this component with the (partial)
                // state that was previously saved. Fields not in the
                // partial saved state are left with their original values.
                restoreDescendantComponentStates(getColumns(getChildren()).iterator(),
                        rowState, false);
            }
        }
    }

    private List<UIColumn> getColumns(List<UIComponent> children) {
        List<UIColumn> columns = new ArrayList<UIColumn>(children.size());
        for (UIComponent child : children)
        {
            if (child instanceof UIColumn)
            {
                columns.add((UIColumn) child);
            }
        }
        return columns;
    }

    /**
     * Overwrite the state of the child components of this component
     * with data previously saved by method saveDescendantComponentStates.
     * <p>
     * The saved state info only covers those fields that are expected to
     * vary between rows of a table. Other fields are not modified.
     */
    private void restoreDescendantComponentStates(Iterator childIterator,
            Object state, boolean restoreChildFacets)
    {
        Iterator descendantStateIterator = null;
        while (childIterator.hasNext())
        {
            if (descendantStateIterator == null && state != null)
            {
                descendantStateIterator = ((Collection) state).iterator();
            }
            UIComponent component = (UIComponent) childIterator.next();

            // reset the client id (see spec 3.1.6)
            component.setId(component.getId());
            if(!component.isTransient())
            {
                Object childState = null;
                Object descendantState = null;
                if (descendantStateIterator != null
                        && descendantStateIterator.hasNext())
                {
                    Object[] object = (Object[]) descendantStateIterator.next();
                    childState = object[0];
                    descendantState = object[1];
                }
                if (component instanceof EditableValueHolder)
                {
                    ((EditableValueHolderState) childState)
                            .restoreState((EditableValueHolder) component);
                }
                Iterator childsIterator;
                if (restoreChildFacets)
                {
                    childsIterator = component.getFacetsAndChildren();
                }
                else
                {
                    childsIterator = component.getChildren().iterator();
                }
                restoreDescendantComponentStates(childsIterator, descendantState,
                        true);
            }
        }
    }

    /**
     * Walk the tree of child components of this UIData, saving the parts of
     * their state that can vary between rows.
     * <p>
     * This is very similar to the process that occurs for normal components
     * when the view is serialized. Transient components are skipped (no
     * state is saved for them).
     * <p>
     * If there are no children then null is returned. If there are one or
     * more children, and all children are transient then an empty collection
     * is returned; this will happen whenever a table contains only read-only
     * components.
     * <p>
     * Otherwise a collection is returned which contains an object for every
     * non-transient child component; that object may itself contain a collection
     * of the state of that child's child components.
     */
    private Object saveDescendantComponentStates(Iterator childIterator,
            boolean saveChildFacets)
    {
        Collection childStates = null;
        while (childIterator.hasNext())
        {
            if (childStates == null)
            {
                childStates = new ArrayList();
            }
            UIComponent child = (UIComponent) childIterator.next();
            if(!child.isTransient())
            {
                // Add an entry to the collection, being an array of two
                // elements. The first element is the state of the children
                // of this component; the second is the state of the current
                // child itself.

                Iterator childsIterator;
                if (saveChildFacets)
                {
                    childsIterator = child.getFacetsAndChildren();
                }
                else
                {
                    childsIterator = child.getChildren().iterator();
                }
                Object descendantState = saveDescendantComponentStates(
                        childsIterator, true);
                Object state = null;
                if (child instanceof EditableValueHolder)
                {
                    state = new EditableValueHolderState(
                            (EditableValueHolder) child);
                }
                childStates.add(new Object[] { state, descendantState });
            }
        }
        return childStates;
    }

    public String getClientId(FacesContext context)
    {
        String clientId = super.getClientId(context);
        int rowIndex = getRowIndex();
        if (rowIndex == -1)
        {
            return clientId;
        }
        return clientId + NamingContainer.SEPARATOR_CHAR + rowIndex;
    }

    /**
     * Modify events queued for any child components so that the
     * UIData state will be correctly configured before the event's
     * listeners are executed.
     * <p>
     * Child components or their renderers may register events against
     * those child components. When the listener for that event is
     * eventually invoked, it may expect the uidata's rowData and
     * rowIndex to be referring to the same object that caused the
     * event to fire.
     * <p>
     * The original queueEvent call against the child component has been
     * forwarded up the chain of ancestors in the standard way, making
     * it possible here to wrap the event in a new event whose source
     * is <i>this</i> component, not the original one. When the event
     * finally is executed, this component's broadcast method is invoked,
     * which ensures that the UIData is set to be at the correct row
     * before executing the original event.
     */
    public void queueEvent(FacesEvent event)
    {
        super.queueEvent(new FacesEventWrapper(event, getRowIndex(), this));
    }

    /**
     * Ensure that before the event's listeners are invoked this UIData
     * component's "current row" is set to the row associated with the event.
     * <p>
     * See queueEvent for more details. 
     */
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        if (event instanceof FacesEventWrapper)
        {
            FacesEvent originalEvent = ((FacesEventWrapper) event)
                    .getWrappedFacesEvent();
            int eventRowIndex = ((FacesEventWrapper) event).getRowIndex();
            int currentRowIndex = getRowIndex();
            // Romet: UIGenericPicker.PickerEvent is broadcasted also when row is deleted,
            // so we would get index out of bounds exception without this check.
            // Probably there should be better way to handle this.
            if (getDataModel().size() > eventRowIndex) {
                setRowIndex(eventRowIndex);
            }
            try
            {
              originalEvent.getComponent().broadcast(originalEvent);
            }
            finally
            {
              setRowIndex(currentRowIndex);
            }
        }
        else
        {
            super.broadcast(event);
        }
    }

    /**
     * Perform necessary actions when rendering of this component starts,
     * before delegating to the inherited implementation which calls the
     * associated renderer's encodeBegin method.
     */
    public void encodeBegin(FacesContext context) throws IOException
    {
        _initialDescendantComponentState = null;
       // Alar Kvell: UIData clears data model before each clean rendering
       // UIRichList clears data model only on setValue calls or when refreshOnBind = true
/*       
        if (_isValidChilds && !hasErrorMessages(context))
        {
            // Clear the data model so that when rendering code calls
            // getDataModel a fresh model is fetched from the backing
            // bean via the value-binding.
            _dataModelMap.clear();
            
            // When the data model is cleared it is also necessary to
            // clear the saved row state, as there is an implicit 1:1
            // relation between objects in the _rowStates and the
            // corresponding DataModel element.
            _rowStates.clear();
        }
*/
        super.encodeBegin(context);
    }

    /**
     * @see javax.faces.component.UIComponentBase#encodeEnd(javax.faces.context.FacesContext)
     */
    public void encodeEnd(FacesContext context) throws IOException
    {
        setRowIndex(-1);
        super.encodeEnd(context);
    }

    public void processDecodes(FacesContext context)
    {
        if (context == null)
            throw new NullPointerException("context");
        if (!isRendered())
            return;
        setRowIndex(-1);
        processFacets(context, PROCESS_DECODES);
        processColumnFacets(context, PROCESS_DECODES);
        processColumnChildren(context, PROCESS_DECODES);
        processNonColumns(context, PROCESS_DECODES);
        setRowIndex(-1);
        try
        {
            decode(context);
        }
        catch (RuntimeException e)
        {
            context.renderResponse();
            throw e;
        }
    }

    public void processValidators(FacesContext context)
    {
        if (context == null)
            throw new NullPointerException("context");
        if (!isRendered())
            return;
        setRowIndex(-1);
        processFacets(context, PROCESS_VALIDATORS);
        processColumnFacets(context, PROCESS_VALIDATORS);
        processColumnChildren(context, PROCESS_VALIDATORS);
        processNonColumns(context, PROCESS_VALIDATORS);
        setRowIndex(-1);

       // Alar Kvell: We don't need to clear data model before each clean rendering
       // As mentioned in encodeBegin
/*
        // check if an validation error forces the render response for our data
        if (context.getRenderResponse())
        {
            _isValidChilds = false;
        }
*/
    }

    public void processUpdates(FacesContext context)
    {
        if (context == null)
            throw new NullPointerException("context");
        if (!isRendered())
            return;
        setRowIndex(-1);
        processFacets(context, PROCESS_UPDATES);
        processColumnFacets(context, PROCESS_UPDATES);
        processColumnChildren(context, PROCESS_UPDATES);
        processNonColumns(context, PROCESS_UPDATES);
        setRowIndex(-1);

       // Alar Kvell: We don't need to clear data model before each clean rendering
       // As mentioned in encodeBegin
/*
        if (context.getRenderResponse())
        {
            _isValidChilds = false;
        }
*/
    }

    private void processFacets(FacesContext context, int processAction)
    {
        for (Iterator it = getFacets().values().iterator(); it.hasNext();)
        {
            UIComponent facet = (UIComponent) it.next();
            process(context, facet, processAction);
        }
    }

    /**
     * Invoke the specified phase on all facets of all UIColumn children
     * of this component. Note that no methods are called on the UIColumn
     * child objects themselves.
     * 
     * @param context is the current faces context.
     * @param processAction specifies a JSF phase: decode, validate or update.
     */
    private void processColumnFacets(FacesContext context, int processAction)
    {
        for (Iterator childIter = getChildren().iterator(); childIter.hasNext();)
        {
            UIComponent child = (UIComponent) childIter.next();
            if (child instanceof UIColumn)
            {
                if (!child.isRendered())
                {
                    //Column is not visible
                    continue;
                }
                for (Iterator facetsIter = child.getFacets().values()
                        .iterator(); facetsIter.hasNext();)
                {
                    UIComponent facet = (UIComponent) facetsIter.next();
                    process(context, facet, processAction);
                }
            }
        }
    }

    /**
     * Invoke the specified phase on all non-facet children of all UIColumn
     * children of this component. Note that no methods are called on the
     * UIColumn child objects themselves.
     * 
     * @param context is the current faces context.
     * @param processAction specifies a JSF phase: decode, validate or update.
     */
    private void processColumnChildren(FacesContext context, int processAction)
    {
/*
        int first = getFirst();
        int rows = getRows();
        int last;
        if (rows == 0)
        {
            last = getRowCount();
        }
        else
        {
            last = first + rows;
        }
        for (int rowIndex = first; last==-1 || rowIndex < last; rowIndex++)
        {
            setRowIndex(rowIndex);

            //scrolled past the last row
            if (!isRowAvailable())
                break;
*/
        // Alar Kvell: Iterating over all the rows is performed UIRichList specific
        // RichListRenderer.encodeChildren performs it the same way
        bind(true);
        while (isDataAvailable())
        {
            nextRow();

            for (Iterator it = getChildren().iterator(); it.hasNext();)
            {
                UIComponent child = (UIComponent) it.next();
                if (child instanceof UIColumn)
                {
                    if (!child.isRendered())
                    {
                        //Column is not visible
                        continue;
                    }
                    for (Iterator columnChildIter = child.getChildren()
                            .iterator(); columnChildIter.hasNext();)
                    {
                        UIComponent columnChild = (UIComponent) columnChildIter
                                .next();
                        process(context, columnChild, processAction);
                    }
                }
            }
        }
    }

    private void processNonColumns(FacesContext context, int processAction)
    {
        for (Iterator it = getChildren().iterator(); it.hasNext();)
        {
            UIComponent child = (UIComponent) it.next();
            if (!(child instanceof UIColumn))
            {
                if (!child.isRendered())
                {
                    //Column is not visible
                    continue;
                }
                process(context, child, processAction);
            }
        }
    }

    private void process(FacesContext context, UIComponent component,
            int processAction)
    {
        switch (processAction)
        {
        case PROCESS_DECODES:
            component.processDecodes(context);
            break;
        case PROCESS_VALIDATORS:
            component.processValidators(context);
            break;
        case PROCESS_UPDATES:
            component.processUpdates(context);
            break;
        }
    }

    private static class FacesEventWrapper extends FacesEvent
    {
        private static final long serialVersionUID = 6648047974065628773L;
        private FacesEvent _wrappedFacesEvent;
        private int _rowIndex;

        // Alar Kvell: third constructor argument is UIRichList
        public FacesEventWrapper(FacesEvent facesEvent, int rowIndex,
                UIRichList redirectComponent)
        {
            super(redirectComponent);
            _wrappedFacesEvent = facesEvent;
            _rowIndex = rowIndex;
        }

        public PhaseId getPhaseId()
        {
            return _wrappedFacesEvent.getPhaseId();
        }

        public void setPhaseId(PhaseId phaseId)
        {
            _wrappedFacesEvent.setPhaseId(phaseId);
        }

        public void queue()
        {
            _wrappedFacesEvent.queue();
        }

        public String toString()
        {
            return _wrappedFacesEvent.toString();
        }

        public boolean isAppropriateListener(FacesListener faceslistener)
        {
            return _wrappedFacesEvent.isAppropriateListener(faceslistener);
        }

        public void processListener(FacesListener faceslistener)
        {
            _wrappedFacesEvent.processListener(faceslistener);
        }

        public FacesEvent getWrappedFacesEvent()
        {
            return _wrappedFacesEvent;
        }

        public int getRowIndex()
        {
            return _rowIndex;
        }
    }

    private class EditableValueHolderState implements Serializable
    {
        private static final long serialVersionUID = 1L;
        
        private final Object _value;
        private final boolean _localValueSet;
        private final boolean _valid;
        private final Object _submittedValue;

        public EditableValueHolderState(EditableValueHolder evh)
        {
            _value = evh.getLocalValue();
            _localValueSet = evh.isLocalValueSet();
            _valid = evh.isValid();
            _submittedValue = evh.getSubmittedValue();
        }

        public void restoreState(EditableValueHolder evh)
        {
            evh.setValue(_value);
            evh.setLocalValueSet(_localValueSet);
            evh.setValid(_valid);
            evh.setSubmittedValue(_submittedValue);
        }
    }

    // additional methods for exporting all data rows(not just visible rows) that are used by RichListDataReader
    private int absoluteRwCount;
    public boolean isAbsoluteDataAvailable() {
        if (this.rowIndex + 1 < this.absoluteRwCount) {
            return true;
        }
        // Reset rowIndex, so that footer components wouldn't have last row index inside their id
        setRowIndex(-1);
        return false;
    }

    public void increment() {
        if (rowIndex + 1 == getDataModel().size()) {
            logger.warn("max rowindex '" + rowIndex + "' was already set");
            return;
        }
        // increment row count and get next row
        setRowIndex(rowIndex + 1);
    }

}
