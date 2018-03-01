package com.vaadin.flow.component;

import com.vaadin.flow.router.RouterLayout;

public interface HasParentLayout<R extends RouterLayout> {

    void setParentLayout(R parentLayout);

}
