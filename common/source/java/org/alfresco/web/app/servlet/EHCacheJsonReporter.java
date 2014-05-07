package org.alfresco.web.app.servlet;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.alfresco.repo.cache.InternalEhCacheManagerFactoryBean;

import flexjson.JSONSerializer;

public class EHCacheJsonReporter extends BaseServlet {

    private static final long serialVersionUID = -3384383658370024824L;
    private CacheManager cacheManager;

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException
    {
        if (cacheManager == null)
        {
            cacheManager = InternalEhCacheManagerFactoryBean.getInstance();
        }

        String[] cacheNames;
        if (req.getParameter("caches") != null) {
            cacheNames = req.getParameter("caches").split(",");
        } else {
            cacheNames = cacheManager.getCacheNames();
        }

        Date creationTime = null;
        if (req.getParameter("creationTime") != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm");
            try {
                creationTime = sdf.parse(req.getParameter("creationTime"));
            } catch (ParseException e) {
                res.getWriter().print("dateformat error, try 12-12-2012 12:12");
                return;
            }
        }

        JSONSerializer serializer = new JSONSerializer();
        res.getWriter().print("[");

        for (Iterator cache_i = Arrays.asList(cacheNames).iterator(); cache_i.hasNext();)
        {
            String cacheName = (String) cache_i.next();
            Cache cache = cacheManager.getCache(cacheName);
            res.getWriter().print("{ \"cacheName\": ");
            serializer.serialize(cacheName, res.getWriter());
            if (cache == null) {
                continue;
            }
            res.getWriter().print(", \"storeSize\":");
            serializer.serialize(cache.getMemoryStoreSize(), res.getWriter());

            res.getWriter().print(", \"cacheContent\": [");

            String serialized = "";
            for (Iterator i = cache.getKeys().iterator(); i.hasNext();) {
                Element e = cache.get(i.next());
                if (creationTime == null || e.getCreationTime() >= creationTime.getTime()) {
                    serialized = "";
                    try {
                        serialized = serializer.serialize(e);
                        res.getWriter().print(serialized);
                    } catch (Exception ex) {
                        res.getWriter().print("{}");
                    }
                    if (i.hasNext()) {
                        res.getWriter().print(",");
                    }
                }
            }
            res.getWriter().print("]}\n");
            if (cache_i.hasNext()) {
                res.getWriter().print(",");
            }

        }
        res.getWriter().print("]");

    }
}
