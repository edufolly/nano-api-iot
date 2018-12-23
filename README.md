# nano-api-iot

## How to use:

```java
public static void main(String[] args) {

    new Nano()
            .setPort(8088)
            .setMaxConnections(200)
            .setCacheDir("cache")
            .setPackageName("io.github.edufolly.nano.start.ws")
            .setLogConfigurationFile("log4j2.xml")
            .setServerName("Nano API IOT Server")
            .start();
}
```

## Examples:

```java
package io.github.edufolly.nano.start.ws;

import io.github.edufolly.nano.annotations.BodyParam;
import io.github.edufolly.nano.annotations.GET;
import io.github.edufolly.nano.annotations.MediaTypeEnum;
import io.github.edufolly.nano.annotations.POST;
import io.github.edufolly.nano.annotations.Path;
import io.github.edufolly.nano.annotations.PathParam;
import io.github.edufolly.nano.annotations.QueryStringParam;
import io.github.edufolly.nano.annotations.ReturnType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Eduardo Folly
 */
@Path("/api/test")
public class ApiTeste {

    @GET
    @ReturnType(MediaTypeEnum.TEXT)
    @Path("/text")
    public String testGetText() {
        return "Test OK!!";
    }

    @GET
    @ReturnType(MediaTypeEnum.HTML)
    @Path("/html")
    public String testGetHtml() {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<title>Test OK!!</title>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<h1>Test OK!!</h1>");
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }

    @GET
    @ReturnType(MediaTypeEnum.JSON)
    @Path("/json")
    public Map testGetJson() {
        Map map = new HashMap();
        map.put("test", "OK!!");
        return map;
    }

    @GET
    @Path("/null")
    public Object testNull() {
        return null;
    }

    @GET
    @Path("/void")
    public void testVoid() {
        // Nothing.
    }

    @GET
    @Path("/erro")
    public Object testException() throws Exception {
        throw new Exception("My error message!");
    }

    @GET
    @Path("/param/:pint/:pstring")
    @PathParam({"pint", "pstring"})
    public Map<String, Object> testGetParam(int pint, String pstring) {
        Map map = new HashMap();
        map.put("test", "OK!!");
        map.put("pint", pint);
        map.put("pstring", pstring);
        return map;
    }

    @GET
    @Path("/qs")
    @QueryStringParam({"q"})
    public Map<String, Object> testGetQueryString(String q) {
        Map map = new HashMap();
        map.put("test", "OK!!");
        map.put("q", q);
        return map;
    }

    @POST
    @BodyParam
    public Map<String, Object> testPost(Map<String, Object> map) {
        map.put("test", "OK!!");
        return map;
    }
}
```
