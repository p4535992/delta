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
package org.alfresco.repo.search.impl.lucene;

import java.util.concurrent.ThreadPoolExecutor;

import org.alfresco.repo.domain.hibernate.BulkLoader;
import org.alfresco.repo.search.MLAnalysisMode;
import org.springframework.context.ConfigurableApplicationContext;

public interface LuceneConfig
{
    /**
     * Set the lock dir - just to make sure - this should no longer be used.
     * 
     * @param lockDirectory
     */
    public void setLockDirectory(String lockDirectory);

    /**
     * The path to the index location
     * 
     * @return
     */
    public String getIndexRootLocation();

    /**
     * The batch size in which to group flushes of the index.
     * 
     * @return
     */
    public int getIndexerBatchSize();

    /**
     * The maximum numbr of sub-queries the can be generated out of wild card expansion etc
     * 
     * @return
     */
    public int getQueryMaxClauses();

    /**
     * The default mode for analysing ML text during index.
     * 
     * @return
     */
    public MLAnalysisMode getDefaultMLIndexAnalysisMode();

    /**
     * The default mode for analysis of ML text during search.
     * 
     * @return
     */
    public MLAnalysisMode getDefaultMLSearchAnalysisMode();

    /**
     * Get the max field length that determine how many tokens are put into the index
     * 
     * @return
     */
    public int getIndexerMaxFieldLength();

    /**
     * Get the thread pool for index merging etc
     * 
     * @return
     */
    public ThreadPoolExecutor getThreadPoolExecutor();

    /**
     * Get preloader - may be null if preloading is not supported
     * 
     * @return
     */
    public BulkLoader getBulkLoader();

    /**
     * Use the nio memory mapping (work arounf for bugs with some JVMs)
     * @return
     */
    public boolean getUseNioMemoryMapping();
    
    /**
     * Max doc number that will merged in memory (and not on disk)
     * 
     * @return
     */
    public int getMaxDocsForInMemoryMerge();
    
    /**
     * Lucene writer config
     * @return
     */
    public int getWriterMinMergeDocs();
    
    /**
     * Lucene writer config
     * @return
     */
    public int getWriterMergeFactor();
    
    /**
     * Lucene writer config
     * @return
     */
    public int getWriterMaxMergeDocs();
    
    /**
     * Lucene merger config
     * @return
     */
    public int getMergerMinMergeDocs();
    
    /**
     * Lucene merger config
     * @return
     */
    public int getMergerMergeFactor();
    
    /**
     * Lucene merger config
     * @return
     */
    public int getMergerMaxMergeDocs();
    
    /**
     * Target overlays (will apply deletions and create indexes if over this limit)
     * @return
     */
    public int getMergerTargetOverlayCount();
    
    /**
     * The factor by which the target overlay count is multiplied to determine the allowable number of overlays before
     * blocking.
     * 
     * @return the factor by which the target overlay count is multiplied to determine the allowable number of overlays
     *         before blocking
     */
    public int getMergerTargetOverlaysBlockingFactor();
    
    /**
     * Target index count. Over this indexes will be merged together.
     * @return
     */
    public int getMergerTargetIndexCount();
    
    /**
     * Lucene term index interval
     * @return
     */
    public int getTermIndexInterval();
    
    /**
     * Is caching enabled for each index fragment?
     * @return
     */
    public boolean isCacheEnabled();
    
    /**
     * How many categories to cache (-ve => unbounded)
     * @return
     */
    public int getMaxIsCategoryCacheSize();
    
    /**
     * How many documents to cache (-ve => unbounded)
     * @return
     */
    public int getMaxDocumentCacheSize();
    
    /**
     * How many document ids to cache (-ve => unbounded)
     * @return
     */
    public int getMaxDocIdCacheSize();
    
    /**
     * How many paths to cache (-ve => unbounded)
     * @return
     */
    public int getMaxPathCacheSize();
    
    /**
     * How many types to cache (-ve => unbounded)
     * @return
     */
    public int getMaxTypeCacheSize();
    
    /**
     * How many parents to cache (-ve => unbounded)
     * @return
     */
    public int getMaxParentCacheSize();
   
    /**
     * How many link aspects to cache (-ve => unbounded)
     * @return
     */
    public int getMaxLinkAspectCacheSize();

    /**
     * If we are using the DateAnalyser then lucene sort is only to the date, as that is all that is in the index.
     * If this is true, a query that defines a sort on a datetime field will do a post sort in Java.
     * 
     * For the DateTimeAnalyser no post sort is done.
     * (The default config does do a post sort)
     * 
     * In the future, this behaviour may also be set per query on the SearchParameters object.
     * 
     * @return
     */
    public boolean getPostSortDateTime();

    /**
     * Gets the application context through which events can be broadcast
     * @return
     */
    public ConfigurableApplicationContext getApplicationContext();
}
