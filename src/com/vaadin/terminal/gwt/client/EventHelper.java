/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.client;

import static com.vaadin.shared.EventId.BLUR;
import static com.vaadin.shared.EventId.FOCUS;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.DomEvent.Type;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Helper class for attaching/detaching handlers for Vaadin client side
 * components, based on identifiers in UIDL. Helpers expect Paintables to be
 * both listeners and sources for events. This helper cannot be used for more
 * complex widgets.
 * <p>
 * Possible current registration is given as parameter. The returned
 * registration (possibly the same as given, should be store for next update.
 * <p>
 * Pseudocode what helpers do:
 * 
 * <pre>
 * 
 * if paintable has event listener in UIDL
 *      if registration is null
 *              register paintable as as handler for event
 *      return the registration
 * else 
 *      if registration is not null
 *              remove the handler from paintable
 *      return null
 * 
 * 
 * </pre>
 * 
 */
public class EventHelper {

    /**
     * Adds or removes a focus handler depending on if the connector has focus
     * listeners on the server side or not.
     * 
     * @param connector
     *            The connector to update. Must implement focusHandler.
     * @param handlerRegistration
     *            The old registration reference or null no handler has been
     *            registered previously
     * @return a new registration handler that can be used to unregister the
     *         handler later
     */
    public static <T extends ComponentConnector & FocusHandler> HandlerRegistration updateFocusHandler(
            T connector, HandlerRegistration handlerRegistration) {
        return updateHandler(connector, FOCUS, handlerRegistration,
                FocusEvent.getType());
    }

    /**
     * Adds or removes a blur handler depending on if the connector has blur
     * listeners on the server side or not.
     * 
     * @param connector
     *            The connector to update. Must implement BlurHandler.
     * @param handlerRegistration
     *            The old registration reference or null no handler has been
     *            registered previously
     * @return a new registration handler that can be used to unregister the
     *         handler later
     */
    public static <T extends ComponentConnector & BlurHandler> HandlerRegistration updateBlurHandler(
            T connector, HandlerRegistration handlerRegistration) {
        return updateHandler(connector, BLUR, handlerRegistration,
                BlurEvent.getType());
    }

    private static <H extends EventHandler> HandlerRegistration updateHandler(
            ComponentConnector connector, String eventIdentifier,
            HandlerRegistration handlerRegistration, Type<H> type) {
        if (connector.hasEventListener(eventIdentifier)) {
            if (handlerRegistration == null) {
                handlerRegistration = connector.getWidget().addDomHandler(
                        (H) connector, type);
            }
        } else if (handlerRegistration != null) {
            handlerRegistration.removeHandler();
            handlerRegistration = null;
        }
        return handlerRegistration;

    }

}
