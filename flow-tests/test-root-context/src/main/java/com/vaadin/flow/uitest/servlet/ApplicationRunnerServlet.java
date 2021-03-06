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
package com.vaadin.flow.uitest.servlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.vaadin.flow.uitest.servlet.CustomDeploymentConfiguration.Conf;
import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.util.CurrentInstance;

@WebServlet(asyncSupported = true, urlPatterns = { "/run/*" })
public class ApplicationRunnerServlet extends VaadinServlet {

    public static String CUSTOM_SYSTEM_MESSAGES_PROPERTY = "custom-"
            + SystemMessages.class.getName();

    /**
     * The name of the application class currently used. Only valid within one
     * request.
     */
    private LinkedHashSet<String> defaultPackages = new LinkedHashSet<>();

    private transient final ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String initParameter = servletConfig
                .getInitParameter("defaultPackages");
        if (initParameter != null) {
            Collections.addAll(defaultPackages, initParameter.split(","));
        }
        URL url = getService().getClassLoader().getResource(".");
        if ("file".equals(url.getProtocol())) {
            String path = url.getPath();
            try {
                path = new URI(path).getPath();
            } catch (URISyntaxException e) {
                getLogger().log(Level.FINE, "Failed to decode url", e);
            }
            try {
                addDirectories(new File(url.getPath()), defaultPackages);
            } catch (IOException exception) {
                throw new RuntimeException(
                        "Unable to scan classpath to find packages", exception);
            }

        }
    }

    private void addDirectories(File parent, LinkedHashSet<String> packages)
            throws IOException {
        Path root = parent.toPath();
        Files.walkFileTree(parent.toPath(), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                File file = dir.toFile();
                if (file.isDirectory()) {
                    Path relative = root.relativize(file.toPath());
                    packages.add(relative.toString().replace(File.separatorChar,
                            '.'));
                }
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        this.request.set(request);
        try {
            super.service(request, response);
        } finally {
            this.request.set(null);
        }
    }

    @Override
    protected URL getApplicationUrl(HttpServletRequest request)
            throws MalformedURLException {
        @SuppressWarnings("deprecation")
        URL url = super.getApplicationUrl(request);

        String path = url.toString();
        path += getApplicationRunnerApplicationClassName(request);
        path += "/";

        return new URL(path);
    }

    private String getApplicationRunnerApplicationClassName(
            HttpServletRequest request) {
        try {
            return getClassToRun().getName();
        } catch (ClassNotFoundException e) {
            return getApplicationRunnerURIs(request).applicationClassname;
        }
    }

    private final class ProxyDeploymentConfiguration
            implements InvocationHandler, Serializable {
        private final DeploymentConfiguration originalConfiguration;

        private ProxyDeploymentConfiguration(
                DeploymentConfiguration originalConfiguration) {
            this.originalConfiguration = originalConfiguration;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getDeclaringClass() == DeploymentConfiguration.class) {
                // Find the configuration instance to delegate to
                DeploymentConfiguration configuration = findDeploymentConfiguration(
                        originalConfiguration);

                return method.invoke(configuration, args);
            } else {
                return method.invoke(proxy, args);
            }
        }
    }

    // TODO Don't need to use a data object now that there's only one field
    private static class URIS {
        // String staticFilesPath;
        // String applicationURI;
        // String context;
        // String runner;
        String applicationClassname;

    }

    /**
     * Parses application runner URIs.
     *
     * If request URL is e.g.
     * http://localhost:8080/vaadin/run/com.vaadin.demo.Calc then
     * <ul>
     * <li>context=vaadin</li>
     * <li>Runner servlet=run</li>
     * <li>Vaadin application=com.vaadin.demo.Calc</li>
     * </ul>
     *
     * @param request
     * @return string array containing widgetset URI, application URI and
     *         context, runner, application classname
     */
    private static URIS getApplicationRunnerURIs(HttpServletRequest request) {
        final String[] urlParts = request.getRequestURI().toString()
                .split("\\/");
        // String runner = null;
        URIS uris = new URIS();
        String applicationClassname = null;
        String contextPath = request.getContextPath();
        if (urlParts[1].equals(contextPath.replaceAll("\\/", ""))) {
            // class name comes after web context and runner application
            // runner = urlParts[2];
            if (urlParts.length == 3) {
                throw new IllegalArgumentException("No application specified");
            }
            applicationClassname = urlParts[3];

            // uris.applicationURI = "/" + context + "/" + runner + "/"
            // + applicationClassname;
            // uris.context = context;
            // uris.runner = runner;
            uris.applicationClassname = applicationClassname;
        } else {
            // no context
            // runner = urlParts[1];
            if (urlParts.length == 2) {
                throw new IllegalArgumentException("No application specified");
            }
            applicationClassname = urlParts[2];

            // uris.applicationURI = "/" + runner + "/" + applicationClassname;
            // uris.context = context;
            // uris.runner = runner;
            uris.applicationClassname = applicationClassname;
        }

        return uris;
    }

    private Class<?> getClassToRun() throws ClassNotFoundException {
        Class<?> appClass = null;

        String baseName = getApplicationRunnerURIs(
                request.get()).applicationClassname;
        try {
            appClass = getClass().getClassLoader().loadClass(baseName);
            return appClass;
        } catch (Exception e) {
            //
            for (String pkg : defaultPackages) {
                try {
                    appClass = getClass().getClassLoader()
                            .loadClass(pkg + "." + baseName);
                } catch (ClassNotFoundException ee) {
                    // Ignore as this is expected for many packages
                } catch (Exception e2) {
                    // TODO: handle exception
                    getLogger().log(Level.FINE,
                            "Failed to find application class " + pkg + "."
                                    + baseName,
                            e2);
                }
                if (appClass != null) {
                    return appClass;
                }
            }

        }

        throw new ClassNotFoundException(baseName);
    }

    private Logger getLogger() {
        return Logger.getLogger(ApplicationRunnerServlet.class.getName());
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
        // Get the original configuration from the super class
        final DeploymentConfiguration originalConfiguration = new DefaultDeploymentConfiguration(
                getClass(), initParameters, this::scanForResources) {
            @Override
            public String getUIClassName() {
                return getApplicationRunnerApplicationClassName(request.get());
            }
        };

        // And then create a proxy instance that delegates to the original
        // configuration or a customized version
        return (DeploymentConfiguration) Proxy.newProxyInstance(
                DeploymentConfiguration.class.getClassLoader(),
                new Class[] { DeploymentConfiguration.class },
                new ProxyDeploymentConfiguration(originalConfiguration));
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService service = super.createServletService(
                deploymentConfiguration);
        final SystemMessagesProvider provider = service
                .getSystemMessagesProvider();
        service.setSystemMessagesProvider(new SystemMessagesProvider() {

            @Override
            public SystemMessages getSystemMessages(
                    SystemMessagesInfo systemMessagesInfo) {
                if (systemMessagesInfo.getRequest() == null) {
                    return provider.getSystemMessages(systemMessagesInfo);
                }
                Object messages = systemMessagesInfo.getRequest()
                        .getAttribute(CUSTOM_SYSTEM_MESSAGES_PROPERTY);
                if (messages instanceof SystemMessages) {
                    return (SystemMessages) messages;
                }
                return provider.getSystemMessages(systemMessagesInfo);
            }

        });
        return service;
    }

    private DeploymentConfiguration findDeploymentConfiguration(
            DeploymentConfiguration originalConfiguration) throws Exception {
        // First level of cache
        DeploymentConfiguration configuration = CurrentInstance
                .get(DeploymentConfiguration.class);

        if (configuration == null) {
            // Not in cache, try to find a VaadinSession to get it from
            VaadinSession session = VaadinSession.getCurrent();

            if (session == null) {
                /*
                 * There's no current session, request or response when serving
                 * static resources, but there's still the current request
                 * maintained by ApplicationRunnerServlet, and there's most
                 * likely also a HttpSession containing a VaadinSession for that
                 * request.
                 */

                HttpServletRequest currentRequest = VaadinServletService
                        .getCurrentServletRequest();
                if (currentRequest != null) {
                    HttpSession httpSession = currentRequest.getSession(false);
                    if (httpSession != null) {
                        Map<Class<?>, CurrentInstance> oldCurrent = CurrentInstance
                                .setCurrent((VaadinSession) null);
                        try {
                            VaadinServletService service = (VaadinServletService) VaadinService
                                    .getCurrent();
                            session = service.findVaadinSession(
                                    new VaadinServletRequest(currentRequest,
                                            service));
                        } finally {
                            /*
                             * Clear some state set by findVaadinSession to
                             * avoid accidentally depending on it when coding on
                             * e.g. static request handling.
                             */
                            CurrentInstance.restoreInstances(oldCurrent);
                            currentRequest.removeAttribute(
                                    VaadinSession.class.getName());
                        }
                    }
                }
            }

            if (session != null) {
                String name = ApplicationRunnerServlet.class.getName()
                        + ".deploymentConfiguration";
                try {
                    session.lock();
                    configuration = (DeploymentConfiguration) session
                            .getAttribute(name);

                    if (configuration == null) {
                        ApplicationRunnerServlet servlet = (ApplicationRunnerServlet) VaadinServlet
                                .getCurrent();
                        Class<?> classToRun;
                        try {
                            classToRun = servlet.getClassToRun();
                        } catch (ClassNotFoundException e) {
                            /*
                             * This happens e.g. if the UI class defined in the
                             * URL is not found or if this servlet just serves
                             * static resources while there's some other servlet
                             * that serves the UI (e.g. when using /run-push/).
                             */
                            return originalConfiguration;
                        }

                        CustomDeploymentConfiguration customDeploymentConfiguration = classToRun
                                .getAnnotation(
                                        CustomDeploymentConfiguration.class);
                        if (customDeploymentConfiguration != null) {
                            Properties initParameters = new Properties(
                                    originalConfiguration.getInitParameters());

                            for (Conf entry : customDeploymentConfiguration
                                    .value()) {
                                initParameters.put(entry.name(), entry.value());
                            }

                            configuration = new DefaultDeploymentConfiguration(
                                    servlet.getClass(), initParameters,
                                    this::scanForResources);
                        } else {
                            configuration = originalConfiguration;
                        }

                        session.setAttribute(name, configuration);
                    }
                } finally {
                    session.unlock();
                }

                CurrentInstance.set(DeploymentConfiguration.class,
                        configuration);

            } else {
                configuration = originalConfiguration;
            }
        }
        return configuration;
    }
}
