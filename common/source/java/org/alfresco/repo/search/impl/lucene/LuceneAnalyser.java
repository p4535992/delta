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

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.impl.lucene.analysis.AlfrescoStandardAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.LongAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.MLAnalayser;
import org.alfresco.repo.search.impl.lucene.analysis.PathAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.VerbatimAnalyser;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.search.lucene.SimplePhraseAnalyzer;

/**
 * Analyse properties according to the property definition. The default is to use the standard tokeniser. The tokeniser
 * should not have been called when indexing properties that require no tokenisation. (tokenise should be set to false
 * when adding the field to the document)
 * 
 * @author andyh
 */

public class LuceneAnalyser extends Analyzer
{
    private static Log s_logger = LogFactory.getLog(LuceneAnalyser.class);

    // Dictinary service to look up analyser classes by data type and locale.
    private DictionaryService dictionaryService;

    // If all else fails a fall back analyser
    private final Analyzer defaultAnalyser;

    // Cached analysers for non ML data types.
    private final Map<String, Analyzer> analysers = new HashMap<String, Analyzer>();

    private MLAnalysisMode mlAlaysisMode;
    private DocumentConfigService documentConfigService;

    /**
     * Constructs with a default standard analyser
     * 
     * @param defaultAnalyzer
     *            Any fields not specifically defined to use a different analyzer will use the one provided here.
     */
    public LuceneAnalyser(DictionaryService dictionaryService, MLAnalysisMode mlAlaysisMode)
    {
        this(new AlfrescoStandardAnalyser());
        this.dictionaryService = dictionaryService;
        this.mlAlaysisMode = mlAlaysisMode;
    }

    /**
     * Constructs with default analyzer.
     * 
     * @param defaultAnalyzer
     *            Any fields not specifically defined to use a different analyzer will use the one provided here.
     */
    public LuceneAnalyser(Analyzer defaultAnalyser)
    {
        this.defaultAnalyser = defaultAnalyser;
    }

    public TokenStream tokenStream(String fieldName, Reader reader, AnalysisMode analysisMode)
    {
        // Treat multilingual as a special case.
        // If multilingual then we need to find the correct tokeniser.
        // This is done dynamically by reading a language code at the start of the reader.
        if (fieldName.startsWith("@"))
        {
            QName propertyQName = QName.createQName(fieldName.substring(1));
            PropertyDefinition propertyDef = getPropertyDefinition(propertyQName);
            if (propertyDef != null)
            {
                if (propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT))
                {
                    MLAnalayser analyser = new MLAnalayser(dictionaryService, mlAlaysisMode);
                    return analyser.tokenStream(fieldName, reader);
                }
            }
        }

        Analyzer analyser = analysers.get(fieldName);
        if (analyser == null)
        {
            analyser = findAnalyser(fieldName, analysisMode);
        }
        return analyser.tokenStream(fieldName, reader);
    }

    // FIXME TODO XXX - This is a temporary workaround for LIVE. Should be fixed properly! See also LuceneQuaryParser for same fix. CL 188070
    private PropertyDefinition getPropertyDefinition(QName qname) {
        return getDocumentConfigService().getStaticOrDynamicPropertyDefinition(qname);
    }

    private DocumentConfigService getDocumentConfigService() {
        if (documentConfigService == null) {
            documentConfigService = BeanHelper.getDocumentConfigService();
        }
        return documentConfigService;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader)
    {
        return tokenStream(fieldName, reader, AnalysisMode.DEFAULT);
    }

    /**
     * Pick the analyser from the field name
     * 
     * @param fieldName
     * @return
     */
    private Analyzer findAnalyser(String fieldName, AnalysisMode analysisMode)
    {
        Analyzer analyser;
        if (fieldName.equals("PATH"))
        {
            analyser = new PathAnalyser();
        }
        else if (fieldName.equals("QNAME"))
        {
            analyser = new PathAnalyser();
        }
        else if (fieldName.equals("TYPE"))
        {
            throw new UnsupportedOperationException("TYPE must not be tokenised");
        }
        else if (fieldName.equals("ASPECT"))
        {
            throw new UnsupportedOperationException("ASPECT must not be tokenised");
        }
        else if (fieldName.equals("ANCESTOR"))
        {
            analyser = new WhitespaceAnalyzer();
        }
        else if (fieldName.equals("VALUES"))
        {
            analyser = new MLAnalayser(dictionaryService, MLAnalysisMode.ALL_ONLY);
        }
        else if (fieldName.startsWith("@"))
        {
            if (fieldName.endsWith(".mimetype"))
            {
                analyser = new VerbatimAnalyser();
            }
            else if (fieldName.endsWith(".size"))
            {
                analyser = new LongAnalyser();
            }
            else if (fieldName.endsWith(".locale"))
            {
                analyser = new VerbatimAnalyser(true);
            }
            else
            {
                QName propertyQName = QName.createQName(fieldName.substring(1));

                DynamicPropertyDefinition def = null;
                if (DocumentDynamicModel.URI.equals(propertyQName.getNamespaceURI())) {
                    def = documentConfigService.getPropertyDefinitionById(propertyQName.getLocalName());
                }

                // Temporary fix for person and user uids
                if (propertyQName.equals(ContentModel.PROP_USER_USERNAME)
                        || propertyQName.equals(ContentModel.PROP_USERNAME) || propertyQName.equals(ContentModel.PROP_AUTHORITY_NAME))
                {
                    analyser = new VerbatimAnalyser(true);
                }

                else if (propertyQName.equals(DocumentSpecificModel.Props.TRANSMITTAL_MODE) || propertyQName.equals(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE)
                        || (def != null && FieldType.STRUCT_UNIT == def.getFieldType())) {
                    analyser = new SimplePhraseAnalyzer(MLAnalysisMode.ALL_ONLY);
                }
                else
                {
                    PropertyDefinition propertyDef = getPropertyDefinition(propertyQName);
                    IndexTokenisationMode tokenise = IndexTokenisationMode.TRUE;
                    if (propertyDef != null)
                    {
                        DataTypeDefinition dataType = propertyDef.getDataType();
                        tokenise = propertyDef.getIndexTokenisationMode();
                        if (tokenise == null)
                        {
                            tokenise = IndexTokenisationMode.TRUE;
                        }
                        switch (tokenise)
                        {
                        case TRUE:
                            if (dataType.getName().equals(DataTypeDefinition.CONTENT))
                            {
                                analyser = new MLAnalayser(dictionaryService, MLAnalysisMode.ALL_ONLY);
                            }
                            else if (dataType.getName().equals(DataTypeDefinition.TEXT))
                            {
                                analyser = new MLAnalayser(dictionaryService, MLAnalysisMode.ALL_ONLY);
                            }
                            else
                            {
                                analyser = loadAnalyzer(dataType);
                            }
                            break;
                        case BOTH:
                            switch (analysisMode)
                            {
                            case DEFAULT:
                            case TOKENISE:
                                if (dataType.getName().equals(DataTypeDefinition.CONTENT))
                                {
                                    analyser = new MLAnalayser(dictionaryService, MLAnalysisMode.ALL_ONLY);
                                }
                                else if (dataType.getName().equals(DataTypeDefinition.TEXT))
                                {
                                    analyser = new MLAnalayser(dictionaryService, MLAnalysisMode.ALL_ONLY);
                                }
                                else
                                {
                                    analyser = loadAnalyzer(dataType);
                                }
                                break;
                            case IDENTIFIER:
                                analyser = new VerbatimAnalyser();
                                break;
                            default:
                                throw new UnsupportedOperationException("TYPE must not be tokenised");
                            }

                            break;
                        case FALSE:
                            analyser = new VerbatimAnalyser();
                            break;
                        default:
                            throw new UnsupportedOperationException("TYPE must not be tokenised");
                        }
                    }
                    else
                    {
                        switch (analysisMode)
                        {
                        case IDENTIFIER:
                            analyser = new VerbatimAnalyser();
                            break;
                        case DEFAULT:
                        case TOKENISE:
                            DataTypeDefinition dataType = dictionaryService.getDataType(DataTypeDefinition.TEXT);
                            analyser = loadAnalyzer(dataType);
                            break;
                        default:
                            throw new UnsupportedOperationException();
                        }

                    }
                }
            }
        }
        else
        {
            analyser = defaultAnalyser;
        }
        analysers.put(fieldName, analyser);
        return analyser;
    }

    /**
     * Find an instantiate an analyser. The shuld all be thread sade as Analyser.tokenStream should be re-entrant.
     * 
     * @param dataType
     * @return
     */
    private Analyzer loadAnalyzer(DataTypeDefinition dataType)
    {
        String analyserClassName = dataType.getAnalyserClassName().trim();
        try
        {
            Class<?> clazz = Class.forName(analyserClassName);
            Analyzer analyser = (Analyzer) clazz.newInstance();
            if (s_logger.isDebugEnabled())
            {
                s_logger.debug("Loaded " + analyserClassName + " for type " + dataType.getName());
            }
            return analyser;
        } catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Unable to load analyser for property of type " + dataType.getName() + " using " + analyserClassName);
        } catch (InstantiationException e)
        {
            throw new RuntimeException("Unable to load analyser for property of type " + dataType.getName() + " using " + analyserClassName);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Unable to load analyser for property of type " + dataType.getName() + " using " + analyserClassName);
        }
    }

    /**
     * For multilingual fields we separate the tokens for each instance to break phrase queries spanning different
     * languages etc.
     */
    @Override
    public int getPositionIncrementGap(String fieldName)
    {
        if (fieldName.startsWith("@") && !fieldName.endsWith(".mimetype"))
        {
            QName propertyQName = QName.createQName(fieldName.substring(1));
            PropertyDefinition propertyDef = dictionaryService.getProperty(propertyQName);
            if (propertyDef != null)
            {
                if (propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT))
                {
                    return 1000;
                }
            }
        }
        return super.getPositionIncrementGap(fieldName);
    }

}
