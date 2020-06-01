/*
 * Copyright 2009-2013 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jolokia.request.notification;

import java.util.*;

import javax.management.*;

import org.jolokia.util.EscapeUtil;

/**
 * Command for adding a notification listener for a client with optional
 * filter and handback.
 *
 * @author roland
 * @since 19.03.13
 */
public class AddCommand extends ClientCommand {

    // MBean on which to register a notification listener
    private final ObjectName objectName;

    // List of filter on notification types which are ORed together
    private List<String> filter;

    // An arbitrary handback returned for every notification received
    private Object handback;

    /**
     * Add for GET requests, which mus have the path part '/client/mbean'.
     * Optionally an '/filter1,filter2/handback' part can be provided.
     * (an handback works only with filters given)
     *
     * @param pStack path stack from where to extract the information
     * @throws MalformedObjectNameException if the given mbean name is not a valid {@link ObjectName}
     */
    AddCommand(Stack<String> pStack) throws MalformedObjectNameException {
        super(CommandType.ADD, pStack);
        if (pStack.isEmpty()) {
            throw new IllegalArgumentException("No MBean name given for " + CommandType.ADD);
        }
        objectName = new ObjectName(pStack.pop());
        if (!pStack.isEmpty()) {
            filter = EscapeUtil.split(pStack.pop(),EscapeUtil.CSV_ESCAPE,",");
        }
        if (!pStack.isEmpty()) {
            handback = pStack.pop();
        }
    }

    /**
     * For POST requests, the key 'client' and 'mbean' must be given in the request payload.
     * Optionally, a 'filter' element with an array of string filters (or a single filter as string)
     * can be given. This filter gets applied for the notification type (see {@link NotificationFilterSupport})
     *
     * @param pMap request map
     * @throws MalformedObjectNameException if the given mbean name is not a valid {@link ObjectName}
     */
    AddCommand(Map<String,?> pMap) throws MalformedObjectNameException {
        super(CommandType.ADD, pMap);
        if (!pMap.containsKey("mbean")) {
            throw new IllegalArgumentException("No MBean name given for " + CommandType.ADD);
        }
        objectName = new ObjectName((String) pMap.get("mbean"));
        Object f = pMap.get("filter");
        if (f != null) {
            filter = f instanceof List ? (List<String>) f : Arrays.asList(f.toString());
        }
        handback = pMap.get("handback");
    }

    /**
     * Objectname of the MBean the listener should connect to
     * @return mbean name
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    /**
     * A list of string filters or <code>null</code> if no
     * filters has been provided
     * @return list of filters
     */
    public List<String> getFilter() {
        return filter;
    }

    /**
     * A handback object. For GET requests this is a String, for POSTS it can be
     * an arbitrary JSON structure.
     *
     * @return handback object or null if none has been provided
     */
    public Object getHandback() {
        return handback;
    }
}
