package io.github.edufolly.nano;

import com.google.common.reflect.ClassPath;
import io.github.edufolly.nano.annotations.COPY;
import io.github.edufolly.nano.annotations.Cache;
import io.github.edufolly.nano.annotations.DELETE;
import io.github.edufolly.nano.annotations.GET;
import io.github.edufolly.nano.annotations.HEAD;
import io.github.edufolly.nano.annotations.OPTIONS;
import io.github.edufolly.nano.annotations.PATCH;
import io.github.edufolly.nano.annotations.POST;
import io.github.edufolly.nano.annotations.PUT;
import io.github.edufolly.nano.annotations.Path;
import io.github.edufolly.nano.util.Request;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author Eduardo Folly
 */
public class EndpointCache {

    private final Map<Pattern, Invokable> cache = new HashMap<>();

    /**
     *
     * @param nano
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public EndpointCache(Nano nano) throws IOException, ClassNotFoundException {

        final ClassLoader loader = Thread.currentThread()
                .getContextClassLoader();

        ClassPath classpath = ClassPath.from(loader);

        for (ClassPath.ClassInfo classInfo : classpath
                .getTopLevelClasses(nano.getPackageName())) {

            Class clazz = classInfo.load();

            String classEndpoint = "";

            if (clazz.isAnnotationPresent(Path.class)) {
                Path annotation = (Path) clazz.getAnnotation(Path.class);
                classEndpoint = annotation.value();

                if (!classEndpoint.startsWith("/")) {
                    classEndpoint = "/" + classEndpoint;
                }

                if (classEndpoint.endsWith("/")) {
                    classEndpoint = classEndpoint
                            .substring(0, classEndpoint.length() - 1);
                }
            }

            for (Method method : clazz.getMethods()) {

                String finalEndpoint = classEndpoint;

                if (method.isAnnotationPresent(Path.class)) {

                    String methodEndpoint = method
                            .getAnnotation(Path.class).value();

                    if (!methodEndpoint.startsWith("/")) {
                        methodEndpoint = "/" + methodEndpoint;
                    }

                    finalEndpoint += methodEndpoint;
                }

                if (!finalEndpoint.isEmpty()) {

                    if (!finalEndpoint.endsWith("/")) {
                        finalEndpoint += "/";
                    }

                    // Busca parâmetros
                    String patternEndpoint = finalEndpoint
                            .replaceAll(":([^/]+?)/", "([^/]+?)/");

                    // Método
                    String prefix = null;

                    if (method.isAnnotationPresent(GET.class)) {
                        prefix = "GET";
                    } else if (method.isAnnotationPresent(POST.class)) {
                        prefix = "POST";
                    } else if (method.isAnnotationPresent(PUT.class)) {
                        prefix = "PUT";
                    } else if (method.isAnnotationPresent(DELETE.class)) {
                        prefix = "DELETE";
                    } else if (method.isAnnotationPresent(PATCH.class)) {
                        prefix = "PATCH";
                    } else if (method.isAnnotationPresent(COPY.class)) {
                        prefix = "COPY";
                    } else if (method.isAnnotationPresent(HEAD.class)) {
                        prefix = "HEAD";
                    } else if (method.isAnnotationPresent(OPTIONS.class)) {
                        prefix = "OPTIONS";
                    }

                    // Cache
                    long cacheTime = 0;
                    if (method.isAnnotationPresent(Cache.class)) {
                        cacheTime = method.getAnnotation(Cache.class).value();
                    }

                    if (prefix != null) {
                        Pattern compile = Pattern
                                .compile(prefix + " " + patternEndpoint + "$");

                        cache.put(compile, new Invokable(nano, clazz, method,
                                finalEndpoint, compile, cacheTime));
                    }
                }
            }
        }
    }

    /**
     *
     * @param request
     * @return
     */
    public Invokable find(Request request) {
        return find(request.getHttpMethod() + " " + request.getPath());
    }

    /**
     *
     * @param find
     * @return
     */
    public Invokable find(String find) {
        for (Pattern pattern : cache.keySet()) {
            if (pattern.matcher(find).matches()) {
                return cache.get(pattern);
            }
        }

        return null;
    }

}
