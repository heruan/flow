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
package com.vaadin.flow.uitest.ui;

import com.vaadin.ui.html.Div;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.angular.model.TemplateModel;

/**
 * @author Vaadin Ltd
 *
 */
public class ElementTemplateEventListenerView extends Div implements View {

    public ElementTemplateEventListenerView() {
        add(new InlineTemplate<>(
                "<div (click)='$element.id=\"foo\"' class='target'>"
                        + "AngularTemplate element with event listener</div>",
                TemplateModel.class));
    }

}
