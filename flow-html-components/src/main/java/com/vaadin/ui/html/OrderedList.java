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
package com.vaadin.ui.html;

import com.vaadin.ui.common.HtmlContainer;
import com.vaadin.ui.event.ClickNotifier;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;

/**
 * Component representing a <code>&lt;p&gt;</code> element.
 *
 * @author Vaadin Ltd
 */
@Tag(Tag.OL)
public class OrderedList extends HtmlContainer implements ClickNotifier {

    /**
     * Creates a new empty ordered list.
     */
    public OrderedList() {
        super();
    }

    /**
     * Creates a new ordered list with the given list items.
     *
     * @param items
     *            the list items
     */
    public OrderedList(ListItem... items) {
        super(items);
    }
}
