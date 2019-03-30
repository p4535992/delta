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
package org.alfresco.repo.search;

import org.alfresco.repo.search.impl.lucene.LuceneIndexerAndSearcher;
import org.alfresco.repo.service.StoreRedirectorProxyFactory;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.AbstractLifecycleBean;
import org.springframework.context.ApplicationEvent;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Component API for indexing. Delegates to the real index retrieved from the {@link #indexerAndSearcherFactory} Transactional support is free.
 * 
 * @see Indexer
 * @author andyh
 */
public class IndexerComponent extends AbstractLifecycleBean implements Indexer
{
    private StoreRedirectorProxyFactory<IndexerAndSearcher> storeRedirectorProxyFactory;
    private IndexerAndSearcher indexerAndSearcherFactory;

    public void setStoreRedirectorProxyFactory(StoreRedirectorProxyFactory<IndexerAndSearcher> storeRedirectorProxyFactory)
    {
        this.storeRedirectorProxyFactory = storeRedirectorProxyFactory;
    }

    @Override
    protected void onBootstrap(ApplicationEvent event)
    {

    }

    @Override
    protected void onShutdown(ApplicationEvent event)
    {
    }

    public void setIndexerAndSearcherFactory(IndexerAndSearcher indexerAndSearcherFactory)
    {
        this.indexerAndSearcherFactory = indexerAndSearcherFactory;
    }

    @Override
    public void createNode(ChildAssociationRef relationshipRef)
    {
        Indexer indexer = getIndexerAndSearcherFactory().getIndexer(
                relationshipRef.getChildRef().getStoreRef());
        indexer.createNode(relationshipRef);
    }

    @Override
    public void updateNode(NodeRef nodeRef)
    {
        Indexer indexer = getIndexerAndSearcherFactory().getIndexer(nodeRef.getStoreRef());
        indexer.updateNode(nodeRef);
    }

    @Override
    public void deleteNode(ChildAssociationRef relationshipRef)
    {
        Indexer indexer = getIndexerAndSearcherFactory().getIndexer(
                relationshipRef.getChildRef().getStoreRef());
        indexer.deleteNode(relationshipRef);
    }

    @Override
    public void createChildRelationship(ChildAssociationRef relationshipRef)
    {
        Indexer indexer = getIndexerAndSearcherFactory().getIndexer(
                relationshipRef.getChildRef().getStoreRef());
        indexer.createChildRelationship(relationshipRef);
    }

    @Override
    public void updateChildRelationship(ChildAssociationRef relationshipBeforeRef, ChildAssociationRef relationshipAfterRef)
    {
        Indexer indexer = getIndexerAndSearcherFactory().getIndexer(
                relationshipBeforeRef.getChildRef().getStoreRef());
        indexer.updateChildRelationship(relationshipBeforeRef, relationshipAfterRef);
    }

    @Override
    public void deleteChildRelationship(ChildAssociationRef relationshipRef)
    {
        Indexer indexer = getIndexerAndSearcherFactory().getIndexer(
                relationshipRef.getChildRef().getStoreRef());
        indexer.deleteChildRelationship(relationshipRef);
    }

    private IndexerAndSearcher getIndexerAndSearcherFactory() {
        if (indexerAndSearcherFactory == null) {
            indexerAndSearcherFactory = BeanHelper.getSpringBean(LuceneIndexerAndSearcher.class, "indexerAndSearcherFactory");
        }
        return indexerAndSearcherFactory;
    }

}
