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
package com.vaadin.router;

import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;

/**
 * This is a basic default error view shown on exceptions during navigation.
 */
@Tag(Tag.DIV)
public class InternalServerError extends Component
        implements HasErrorParameter<Exception> {

    @Override
    public int setErrorParameter(BeforeNavigationEvent event,
            ErrorParameter<Exception> parameter) {
        String exceptionText;
        if (parameter.hasCustomMessage()) {
            exceptionText = String.format(
                    "There was an exception while trying to navigate to '%s' with the exception message '%s'",
                    event.getLocation().getPath(),
                    parameter.getCustomMessage());
        } else {
            exceptionText = String.format(
                    "There was an exception while trying to navigate to '%s'",
                    event.getLocation().getPath());
        }
        getElement().setText(exceptionText);
        return 500;
    }
}
