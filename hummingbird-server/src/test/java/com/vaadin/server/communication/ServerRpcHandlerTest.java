/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server.communication;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.TemplateElementStateProviderTest;
import com.vaadin.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentTest.TestComponent;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * @author Vaadin Ltd
 *
 */
public class ServerRpcHandlerTest {

    private static final String TEST_PROPERTY = "test-property";

    @Tag("a")
    public static class ComponentWithMethod extends Component {

        private boolean isInvoked;

        protected void method(int i) {

        }

        @EventHandler
        private void method() {
            isInvoked = true;
        }
    }

    public static class ComponentWithTwoEventHandlerMethodSameName
            extends ComponentWithMethod {

        @EventHandler
        @Override
        protected void method(int i) {

        }

        @EventHandler
        private void method() {
        }
    }

    public static class MethodWithParameters extends ComponentWithMethod {

        private int intArg;
        private String strArg;
        private boolean[] arrayArg;
        private Double[] doubleArg;
        private Integer[] varArg;
        private int[][] doubleArray;
        private JsonValue jsonValue;

        @Override
        @EventHandler
        protected void method(int i) {
            intArg = i;
        }

        @EventHandler
        protected void method1(String str, boolean[] array) {
            strArg = str;
            arrayArg = array;
        }

        @EventHandler
        protected void method2(Double[] arg1, Integer... varArg) {
            doubleArg = arg1;
            this.varArg = varArg;
        }

        @EventHandler
        protected void method3(int[][] array) {
            doubleArray = array;
        }

        @EventHandler
        protected void method4(JsonValue value) {
            jsonValue = value;
        }
    }

    public static class MethodWithVarArgParameter extends ComponentWithMethod {

        private String[] varArg;

        @EventHandler
        protected void method(String... args) {
            varArg = args;
        }

    }

    public static class ComponentWithMethodThrowingException
            extends ComponentWithMethod {

        @EventHandler
        private void method() {
            throw new NullPointerException();
        }
    }

    public static class ComponentWithNoEventHandlerMethod
            extends ComponentWithMethod {

        /**
         * No {@link EventHandler} annotation.
         */
        public void operation() {
        }
    }

    @Before
    public void setUp() {
        Assert.assertNull(System.getSecurityManager());
    }

    @Test
    public void methodIsInvoked() {
        ComponentWithMethod component = new ComponentWithMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                Json.createArray());

        Assert.assertTrue(component.isInvoked);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithoutArgs_argsProvided() {
        JsonArray args = Json.createArray();
        args.set(0, true);
        ComponentWithMethod component = new ComponentWithMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                args);
    }

    @Test(expected = IllegalStateException.class)
    public void twoEventHandlerMethodsWithTheSameName() {
        ComponentWithMethod component = new ComponentWithTwoEventHandlerMethodSameName();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                Json.createArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithParametersIsNotAccepted() {
        ComponentWithMethod component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                Json.createArray());
    }

    @Test
    public void methodWithParametersIsInvoked() {
        JsonArray array = Json.createArray();
        array.set(0, 65);
        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);

        Assert.assertEquals(65, component.intArg);
    }

    @Test
    public void methodWithArrayParamIsInvoked() {
        JsonArray array = Json.createArray();
        array.set(0, "foo");
        JsonArray secondArg = Json.createArray();
        secondArg.set(0, true);
        secondArg.set(1, false);
        array.set(1, secondArg);
        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method1", array);

        Assert.assertEquals("foo", component.strArg);
        Assert.assertArrayEquals(new boolean[] { true, false },
                component.arrayArg);
    }

    @Test
    public void methodWithVarArgIsInvoked_varArgsAreNotArray() {
        JsonArray array = Json.createArray();

        JsonArray firstArg = Json.createArray();
        firstArg.set(0, 3.1d);
        firstArg.set(1, 65.57d);

        array.set(0, firstArg);

        array.set(1, Json.createNull());
        array.set(2, 56);

        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method2", array);

        Assert.assertArrayEquals(
                new Double[] { firstArg.getNumber(0), firstArg.getNumber(1) },
                component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertNull(component.varArg[0]);
        Assert.assertEquals(Integer.valueOf(56), component.varArg[1]);
        Assert.assertEquals(2, component.varArg.length);
    }

    @Test
    public void methodWithDoubleArrayIsInvoked() {
        JsonArray array = Json.createArray();

        JsonArray arg = Json.createArray();

        JsonArray first = Json.createArray();
        first.set(0, 1);
        first.set(1, 2);

        arg.set(0, first);

        JsonArray second = Json.createArray();
        second.set(0, 3);
        second.set(1, 4);

        arg.set(1, second);

        array.set(0, arg);

        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method3", array);

        Assert.assertArrayEquals(new int[] { (int) first.getNumber(0),
                (int) first.getNumber(1) }, component.doubleArray[0]);

        Assert.assertArrayEquals(new int[] { (int) second.getNumber(0),
                (int) second.getNumber(1) }, component.doubleArray[1]);
    }

    @Test
    public void methodWithJsonValueIsInvoked() {
        JsonArray array = Json.createArray();

        JsonObject json = Json.createObject();
        json.put("foo", "bar");
        array.set(0, json);

        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method4", array);

        Assert.assertEquals(component.jsonValue, json);
    }

    @Test
    public void methodWithVarArgIsInvoked_varArgsIsArray() {
        JsonArray array = Json.createArray();

        JsonArray firstArg = Json.createArray();
        firstArg.set(0, 5.6d);
        firstArg.set(1, 78.36d);

        array.set(0, firstArg);

        JsonArray secondArg = Json.createArray();
        secondArg.set(0, 5);
        secondArg.set(1, Json.createNull());
        secondArg.set(2, 2);
        array.set(1, secondArg);

        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method2", array);

        Assert.assertArrayEquals(
                new Double[] { firstArg.getNumber(0), firstArg.getNumber(1) },
                component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertArrayEquals(new Integer[] { (int) secondArg.getNumber(0),
                null, (int) secondArg.getNumber(2) }, component.varArg);
    }

    @Test
    public void methodWithVarArg_acceptNoValues() {
        JsonArray array = Json.createArray();

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);

        Assert.assertEquals(0, component.varArg.length);
    }

    @Test
    public void methodWithSeveralArgsAndVarArg_acceptNoValues() {
        JsonArray array = Json.createArray();

        JsonArray firstArg = Json.createArray();
        firstArg.set(0, 5.6d);
        firstArg.set(1, 78.36d);

        array.set(0, firstArg);

        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method2", array);

        Assert.assertArrayEquals(
                new Double[] { firstArg.getNumber(0), firstArg.getNumber(1) },
                component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertEquals(0, component.varArg.length);
    }

    @Test
    public void methodWithVarArg_acceptOneValue() {
        JsonArray array = Json.createArray();

        array.set(0, "foo");

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);

        Assert.assertEquals(1, component.varArg.length);
        Assert.assertEquals("foo", component.varArg[0]);
    }

    @Test
    public void methodWithVarArg_arrayIsCorrectlyHandled() {
        JsonArray array = Json.createArray();

        JsonArray value = Json.createArray();
        value.set(0, "foo");
        array.set(0, value);

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);

        Assert.assertArrayEquals(new String[] { value.getString(0) },
                component.varArg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullValueIsNotAcceptedForPrimitive() {
        JsonArray array = Json.createArray();
        array.set(0, Json.createNull());
        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);
    }

    @Test(expected = IllegalStateException.class)
    public void noEventHandlerMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "operation", Json.createArray());
    }

    @Test(expected = IllegalStateException.class)
    public void noMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "operation1", Json.createArray());
    }

    @Test
    public void methodThrowsException_exceptionHasCorrectCause() {
        ComponentWithMethodThrowingException component = new ComponentWithMethodThrowingException();
        try {
            ServerRpcHandler.invokeMethod(component, component.getClass(),
                    "method", Json.createArray());
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testElementEventNoData() throws Exception {
        TestComponent c = new TestComponent();
        Element element = c.getElement();
        UI ui = new UI();
        ui.add(c);
        AtomicInteger invocations = new AtomicInteger(0);

        element.addEventListener("test-event",
                e -> invocations.incrementAndGet());
        sendElementEvent(element, ui, "test-event", null);
        Assert.assertEquals(1, invocations.get());
    }

    @Test
    public void testElementEventData() throws Exception {
        TestComponent c = new TestComponent();
        Element element = c.getElement();
        UI ui = new UI();
        ui.add(c);
        AtomicInteger invocationData = new AtomicInteger(0);

        element.addEventListener("test-event", e -> invocationData
                .addAndGet((int) e.getEventData().getNumber("nr")));
        JsonObject eventData = Json.createObject();
        eventData.put("nr", 123);
        sendElementEvent(element, ui, "test-event", eventData);
        Assert.assertEquals(123, invocationData.get());
    }

    @Test
    public void testSynchronizeProperty() throws Exception {
        TestComponent c = new TestComponent();
        Element element = c.getElement();
        UI ui = new UI();
        ui.add(c);
        Assert.assertFalse(element.hasProperty(TEST_PROPERTY));
        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, "value1");
        Assert.assertEquals("value1", element.getPropertyRaw(TEST_PROPERTY));
        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, "value2");
        Assert.assertEquals("value2", element.getPropertyRaw(TEST_PROPERTY));
    }

    public static void sendElementEvent(Element element, UI ui,
            String eventType, JsonObject eventData) throws Exception {
        JsonArray invocationsData = Json.createArray();
        invocationsData.set(0,
                createElementEventInvocation(element, eventType, eventData));
        new ServerRpcHandler().handleInvocations(ui, 1, invocationsData);
    }

    public static JsonObject createElementEventInvocation(Element element,
            String eventType, JsonObject eventData) {
        StateNode node = getInvocationNode(element);
        // Copied from ServerConnector
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_EVENT_TYPE, eventType);

        if (eventData != null) {
            message.put(JsonConstants.RPC_EVENT_DATA, eventData);
        }

        return message;
    }

    private static StateNode getInvocationNode(Element element) {
        return TemplateElementStateProviderTest.getOverrideNode(element)
                .orElse(element.getNode());
    }

    public static void sendSynchronizePropertyEvent(Element element, UI ui,
            String propertyName, String newValue)
            throws InvalidUIDLSecurityKeyException, IOException {
        JsonArray invocationsData = Json.createArray();
        invocationsData.set(0,
                createSyncPropertyInvocation(element, propertyName, newValue));

        new ServerRpcHandler().handleInvocations(ui, 1, invocationsData);
    }

    public static JsonObject createSyncPropertyInvocation(Element element,
            String property, Serializable value) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_TYPE_PROPERTY_SYNC);
        StateNode node = getInvocationNode(element);
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_PROPERTY, property);
        message.put(JsonConstants.RPC_PROPERTY_VALUE,
                JsonCodec.encodeWithoutTypeInfo(value));
        return message;
    }

}