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
package com.vaadin.flow.router.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParameterDeserializer;
import com.vaadin.flow.router.RouteResolver;
import com.vaadin.flow.server.startup.RouteRegistry;

/**
 * Default implementation of the {@link RouteResolver} interface.
 *
 * @author Vaadin Ltd.
 */
public class DefaultRouteResolver implements RouteResolver {

    @Override
    public NavigationState resolve(ResolveRequest request) {
        RouteRegistry registry = request.getRouter().getRegistry();
        PathDetails path = findPathString(registry,
                request.getLocation().getSegments());
        if (path == null) {
            return null;
        }

        NavigationStateBuilder builder = new NavigationStateBuilder();
        Class<? extends Component> navigationTarget;
        try {
            if (!path.segments.isEmpty()) {
                navigationTarget = getNavigationTargetWithParameter(registry,
                        path.pattern, path.segments);
            } else {
                navigationTarget = getNavigationTarget(registry, path.pattern);
            }

            if (HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
                List<String> pathParameters = getPathParameters(
                        request.getLocation().getPath(), path.path);
                if (!ParameterDeserializer.verifyParameters(navigationTarget,
                        pathParameters)) {
                    return null;
                }
                pathParameters.addAll(0, path.collectedParameters);
                builder.withTarget(navigationTarget, pathParameters);
            } else if (!path.collectedParameters.isEmpty()) {
                builder.withTarget(navigationTarget, path.collectedParameters);
            } else {
                builder.withTarget(navigationTarget);
            }
            builder.withPath(path.path);
        } catch (NotFoundException nfe) {
            String message = "Exception while navigation to path " + path;
            LoggerFactory.getLogger(this.getClass().getName()).warn(message,
                    nfe);
            throw nfe;
        }

        return builder.build();
    }

    private static class PathDetails {
        private final String path;
        private final String pattern;
        private final List<String> segments;
        private final List<String> collectedParameters;

        /**
         * Constructor for path with segment details.
         *
         * @param path
         *            path
         * @param segments
         *            segments for path
         */
        public PathDetails(String path, List<String> segments) {
            this(path, segments, path, Collections.emptyList());
        }

        /**
         * Constructor for path with segment details.
         *
         * @param path
         *            path
         * @param segments
         *            segments for path
         * @param pattern
         *            the route pattern
         * @param collectedParameters
         *            the parameters present on {@code path}
         */
        public PathDetails(String path, List<String> segments, String pattern,
                List<String> collectedParameters) {
            this.path = path;
            this.pattern = pattern;
            this.segments = segments;
            this.collectedParameters = collectedParameters;
        }
    }

    private PathDetails findPathString(RouteRegistry registry,
            List<String> pathSegments) {
        if (pathSegments.isEmpty()) {
            return null;
        }

        Deque<PathDetails> paths = new ArrayDeque<>();
        StringBuilder pathBuilder = new StringBuilder(pathSegments.get(0));
        if (!"".equals(pathSegments.get(0))) {
            paths.push(new PathDetails("", pathSegments));
        }
        for (int i = 0; i < pathSegments.size(); i++) {
            if (i != 0) {
                pathBuilder.append("/").append(pathSegments.get(i));
            }
            findRoutePattern(registry, pathBuilder.toString(),
                    pathSegments.subList(i + 1, pathSegments.size()))
                            .ifPresent(paths::push);
        }
        while (!paths.isEmpty()) {
            PathDetails pathDetails = paths.pop();
            Optional<?> target = registry.getNavigationTarget(pathDetails.pattern,
                    pathDetails.segments);
            if (target.isPresent()) {
                return pathDetails;
            }
        }
        return null;
    }

    /**
     * Looks for a registered route pattern matching the given {@code path}.
     *
     * @param registry
     *            the route registry
     * @param path
     *            the path to match
     * @return the optional path details
     */
    private Optional<PathDetails> findRoutePattern(RouteRegistry registry,
            String path, List<String> segments) {
        if (!registry.hasRoutes()) {
            return Optional.empty();
        }
        List<String> parameters = new ArrayList<>();
        return registry.getRegisteredRoutes().stream().filter(routeData -> {
            String regex = routeData.getUrl();
            Matcher matcher = Pattern.compile(regex).matcher(path);
            if (!matcher.matches()) {
                return false;
            }
            for (int i = 1; i <= matcher.groupCount(); ++i) {
                parameters.add(matcher.group(i));
            }
            return true;
        }).map(routeData -> new PathDetails(path, segments, routeData.getUrl(),
                parameters)).findFirst();
    }

    private Class<? extends Component> getNavigationTarget(
            RouteRegistry registry, String path) throws NotFoundException {
        return registry.getNavigationTarget(path)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "No navigation target found for path '%s'.", path)));
    }

    private Class<? extends Component> getNavigationTargetWithParameter(
            RouteRegistry registry, String path, List<String> segments)
            throws NotFoundException {
        return registry.getNavigationTarget(path, segments)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "No navigation target found for path '%s'.", path)));
    }

    private List<String> getPathParameters(String completePath,
            String routePath) {
        assert completePath != null;
        assert routePath != null;

        String parameterPart = completePath.replaceFirst(routePath, "");
        if (parameterPart.startsWith("/")) {
            parameterPart = parameterPart.substring(1, parameterPart.length());
        }
        if (parameterPart.endsWith("/")) {
            parameterPart = parameterPart.substring(0,
                    parameterPart.length() - 1);
        }
        if (parameterPart.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(parameterPart.split("/"));
    }
}
