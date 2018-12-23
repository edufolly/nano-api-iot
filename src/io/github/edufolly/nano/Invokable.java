package io.github.edufolly.nano;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import io.github.edufolly.nano.annotations.BodyParam;
import io.github.edufolly.nano.annotations.MediaTypeEnum;
import io.github.edufolly.nano.annotations.PathParam;
import io.github.edufolly.nano.annotations.QueryStringParam;
import io.github.edufolly.nano.annotations.ReturnType;
import io.github.edufolly.nano.util.InvokableException;
import io.github.edufolly.nano.util.Request;
import io.github.edufolly.nano.util.Response;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Eduardo Folly
 */
public class Invokable {

    private static final Logger LOGGER = LogManager.getRootLogger();
    private static final transient Pattern PATTERN = Pattern.compile(":([^/]+?)/");
    private static final transient Gson GSON = new Gson();
    //--
    private final Nano nano;
    private final Class clazz;
    private final Method method;
    private final Pattern pattern;
    private final long cacheTime;
    private String[] params;

    /**
     *
     * @param nano
     * @param clazz
     * @param method
     * @param endpoint
     * @param pattern
     * @param cacheTime
     */
    public Invokable(Nano nano, Class clazz, Method method, String endpoint,
            Pattern pattern, long cacheTime) {

        this.nano = nano;
        this.clazz = clazz;
        this.method = method;
        this.pattern = pattern;
        this.cacheTime = cacheTime;

        Matcher matcher = PATTERN.matcher(endpoint);
        List<String> p = new ArrayList();
        while (matcher.find()) {
            p.add(matcher.group(1));
        }

        params = new String[p.size()];

        params = p.toArray(params);
    }

    /**
     *
     * @return
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     *
     * @return
     */
    public Method getMethod() {
        return method;
    }

    /**
     *
     * @param request
     * @param response
     * @return
     * @throws InvokableException
     */
    public String invoke(Request request, Response response) throws InvokableException {
        try {
            MediaTypeEnum mediaType = MediaTypeEnum.JSON;

            File cacheDir = null;
            if (cacheTime > 0) {
                String hash = request.getHash();
                cacheDir = new File(nano.getCacheDir(), hash);
                if (cacheDir.exists()) {
                    long now = System.currentTimeMillis() / 1000l;
                    for (File file : cacheDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().endsWith(".cache");
                        }
                    })) {
                        long cacheExp = Long.parseLong(file.getName().split("\\.")[0]);
                        if (cacheExp > now) {
                            byte[] encoded = Files.readAllBytes(Paths.get(file.toURI()));
                            return new String(encoded, "UTF-8");
                        }
                    }
                }
            }

            Map<String, String> map = new HashMap();

            Matcher matcher = pattern.matcher(request.getHttpMethod() + " "
                    + request.getPath());

            if (matcher.find()) {
                for (int i = 0; i < params.length; i++) {
                    map.put(params[i], matcher.group(i + 1));
                }
            }

            Class<?>[] parameterTypes = method.getParameterTypes();

            Object objects[] = new Object[parameterTypes.length];

            int i = 0;

            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().equals(PathParam.class)) {
                    for (String key : ((PathParam) annotation).value()) {
                        if (map.containsKey(key)) {
                            objects[i] = cast(parameterTypes[i], map.get(key));
                        } else {
                            objects[i] = null;
                        }
                        i++;
                    }
                } else if (annotation.annotationType().equals(QueryStringParam.class)) {
                    for (String key : ((QueryStringParam) annotation).value()) {
                        if (request.containsQueryString(key)) {
                            objects[i] = cast(parameterTypes[i], request.getQueryString(key));
                        } else {
                            objects[i] = null;
                        }
                        i++;
                    }
                } else if (annotation.annotationType().equals(BodyParam.class)) {
                    objects[i] = GSON.fromJson(request.getBody(), parameterTypes[i]);
                    i++;
                } else if (annotation.annotationType().equals(ReturnType.class)) {
                    mediaType = ((ReturnType) annotation).value();
                }
            }

            Object invoked = method.invoke(clazz.newInstance(), objects);

            String retorno;

            switch (mediaType) {
                case HTML:
                    response.addHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                    retorno = invoked.toString();
                    break;
                case TEXT:
                    response.addHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
                    retorno = invoked.toString();
                    break;
                default:
                    response.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

                    response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

                    response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                            "origin, content-type, accept, authorization");

                    response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                            "true");

                    response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                            "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, COPY");

                    retorno = GSON.toJson(invoked);
                    break;
            }

            if (cacheDir != null) {
                if (cacheDir.exists()) {
                    for (File file : cacheDir.listFiles()) {
                        file.delete();
                    }
                    if (!cacheDir.delete()) {
                        LOGGER.error("Não foi possível excluir cache: {}", cacheDir.toString());
                    }
                }

                if (!cacheDir.mkdirs()) {
                    LOGGER.error("Não foi possível criar cache: {}", cacheDir.toString());
                }

                String fileName = ((System.currentTimeMillis() / 1000l) + cacheTime) + ".cache";
                PrintWriter pw = new PrintWriter(new File(cacheDir, fileName));
                pw.print(retorno);
                pw.flush();
                pw.close();
            }

            return retorno;
        } catch (InvocationTargetException e) {
            throw new InvokableException("[" + request.getPath() + "]\n"
                    + e.getTargetException().getMessage(), e);
        } catch (Exception ex) {
            throw new InvokableException("[" + request.getPath() + "]\n"
                    + ex.getMessage(), ex);
        }

    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return clazz.getName() + " - " + method.getName();
    }

    /**
     *
     * @param type
     * @param value
     * @return
     */
    private Object cast(Class type, String value) throws UnsupportedEncodingException {

        value = URLDecoder.decode(value, "UTF-8");

        if (type.equals(Integer.class)) {
            return Integer.parseInt(value);
        } else if (type.equals(int.class)) {
            return Integer.parseInt(value);
        } else if (type.equals(Long.class)) {
            return Long.parseLong(value);
        } else if (type.equals(long.class)) {
            return Long.parseLong(value);
        } else if (type.equals(float.class)) {
            return Float.parseFloat(value);
        } else if (type.equals(Float.class)) {
            return Float.parseFloat(value);
        } else if (type.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        } else if (type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        } else if (type.equals(String.class)) {
            return value;
        } else {
            return type.cast(value);
        }
    }

}
