package ee.smit.tera;

import org.alfresco.service.cmr.dictionary.InvalidAspectException;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.util.Pair;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Andmebaasist otse failide küsimiseks SQL
 *
 * SELECT
 public.alf_content_url.content_url, SUBSTRING(public.alf_content_url.content_url, '[0-9]{4}\/[0-9]+\/[0-9]+')
 FROM
 public.alf_content_data
 INNER JOIN public.alf_content_url ON (public.alf_content_data.content_url_id = public.alf_content_url.id)
 WHERE
 public.alf_content_data.id IN
 (SELECT public.alf_node_properties.long_value FROM public.alf_node_properties INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id) WHERE public.alf_node_properties.node_id IN
 (SELECT public.alf_node_properties.node_id FROM
 public.alf_node_properties
 INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id)
 WHERE
 LOWER(public.alf_node_properties.string_value) LIKE '%.ddoc' AND
 public.alf_qname.local_name = 'name') AND public.alf_qname.local_name = 'content')

 Päringud failide tüüpide koguarvu andmete küsimiseks:
 SELECT count(*)
 FROM
 public.alf_node_properties
 INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id)
 WHERE
 LOWER(public.alf_node_properties.string_value) LIKE '%.ddoc' AND
 public.alf_qname.local_name = 'name';

 SELECT count(*)
 FROM
 public.alf_node_properties
 INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id)
 WHERE
 LOWER(public.alf_node_properties.string_value) LIKE '%.bdoc' AND
 public.alf_qname.local_name = 'name';

 SELECT count(*)
 FROM
 public.alf_node_properties
 INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id)
 WHERE
 LOWER(public.alf_node_properties.string_value) LIKE '%.asice' AND
 public.alf_qname.local_name = 'name';

 */
public class Tera {

    NodeService nodeService;

    public void getListOfDDOCFiles(){
        SearchParameters sp = new SearchParameters();
        //sp.addStore();
    }

}
