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
package com.vaadin.flow.uitest.ui.webcomponent;

import com.vaadin.ui.html.Div;
import com.vaadin.flow.router.View;

public class PaperInputView extends Div implements View {

    public PaperInputView() {
        PaperInput paperInput = new PaperInput("foo");

        add(paperInput);
        paperInput.getElement().addPropertyChangeListener("value",
                event -> showValue(paperInput.getValue()));
    }

    private void showValue(String value) {
        Div div = new Div();
        div.setText(value);
        div.setClassName("update-value");
        add(div);
    }
}
