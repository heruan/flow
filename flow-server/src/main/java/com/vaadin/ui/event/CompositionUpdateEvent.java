/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.ui.event;

import com.vaadin.ui.common.HtmlComponent;

/**
 * The event when a composition is updated.
 *
 * @author Vaadin Ltd
 */
@DomEvent("compositionupdate")
public class CompositionUpdateEvent extends CompositionEvent {

    /**
     * Creates a new composition event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     * @param data
     *            the string being composed
     * @param locale
     *            language code for the composition event, if available;
     *            otherwise, the empty string
     */
    public CompositionUpdateEvent(HtmlComponent source, boolean fromClient,
            @EventData("event.data") String data,
            @EventData("event.locale") String locale) {
        super(source, fromClient, data, locale);
    }

    /**
     * Creates a new server-side composition event with no additional
     * information.
     *
     * @param source
     *            the component that fired the event
     */
    public CompositionUpdateEvent(HtmlComponent source) {
        super(source);
    }

}
