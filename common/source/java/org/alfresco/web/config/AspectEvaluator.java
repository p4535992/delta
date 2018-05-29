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
package org.alfresco.web.config;

import java.util.Set;

import org.alfresco.config.evaluator.Evaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

/**
 * Evaluator that determines whether a given object has a particular aspect applied
 * 
 * @author gavinc
 */
public final class AspectEvaluator implements Evaluator {
    /**
     * Determines whether the given aspect is applied to the given object
     * 
     * @see org.alfresco.config.evaluator.Evaluator#applies(java.lang.Object, java.lang.String)
     */
    @Override
    public boolean applies(Object obj, String condition) {
        boolean result = false;

        if (obj instanceof Node) {
            Set aspects = ((Node) obj).getAspects();
            if (aspects != null) {
                String[] aspectConditions = condition.split(",");
                for (String aspectCondition : aspectConditions) {
                    result = aspects.contains(Repository.resolveToQName(aspectCondition));
                    if (result == true) {
                        break;
                    }
                }
            }
        }

        return result;
    }
}