package io.github.edufolly.nano;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import io.github.edufolly.nano.util.HttpStatus;
import io.github.edufolly.nano.util.Request;
import io.github.edufolly.nano.util.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Eduardo Folly
 */
public class ApiServer extends Thread {

    private static final Logger LOGGER = LogManager.getRootLogger();
    private static final Locale LOCALE = new Locale("en", "US");
    private static final SimpleDateFormat SDF
            = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", LOCALE);
    private static EndpointCache endpointCache = null;
    //--
    private Nano nano;
    private Socket connectedClient;

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     *
     * @param nano
     * @param connectedClient
     */
    public ApiServer(Nano nano, Socket connectedClient) {
        this.nano = nano;
        this.connectedClient = connectedClient;
    }

    /**
     *
     */
    @Override
    public void run() {
        Request request = null;

        Response response = null;
        assert response != null;

        try {
            LOGGER.trace("Conectado: {}:{}",
                    connectedClient.getInetAddress(),
                    connectedClient.getPort());

            response = new Response(connectedClient.getOutputStream());

            request = new Request(connectedClient.getInputStream());

            if(endpointCache == null) {
                endpointCache = new EndpointCache(nano);
            }
            
            Invokable invokable = endpointCache.find(request);

            if (invokable == null) {
                throw new Exception("Endpoint não encontrado. "
                        + "Method: " + request.getHttpMethod()
                        + " - Path: " + request.getPath());
            }

            String body = invokable.invoke(request, response);

            if (body == null) {
                response.clearHeaders();
                response.setHttpStatus(HttpStatus.SC_NO_CONTENT);
            } else {
                response.setHttpStatus(HttpStatus.SC_OK);
                response.setBody(body);
            }

            response.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    "origin, content-type, accept, authorization");

            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                    "true");

            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, COPY");
        } catch (Exception ex) {
            LOGGER.error(connectedClient.getInetAddress() + ":"
                    + connectedClient.getPort(), ex);

            if (request != null) {
                LOGGER.debug(request);
            }

            try {
                response.setHttpStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                Erro erro = new Erro(ex.getMessage(), sw.toString(), request);
                Gson gson = new Gson();
                response.setBody(gson.toJson(erro));
            } catch (Exception exx) {
                LOGGER.error(exx.getMessage(), exx);
            }
        } finally {
            try {
                if (request != null) {
                    response.addHeader(HttpHeaders.SERVER, "Nano API IOT Server");
                    response.flush();
                }
            } catch (Exception exx) {
                LOGGER.error(exx.getMessage(), exx);
            }
        }

        LOGGER.trace("Desconectado.");
    }

    /**
     *
     */
    class Erro {

        private String mensagem;
        private String stacktrace;
        private Request request;

        public Erro() {
        }

        public Erro(String mensagem, String stacktrace, Request request) {
            this.mensagem = mensagem;
            this.stacktrace = stacktrace;
            this.request = request;
        }

        public String getMensagem() {
            return mensagem;
        }

        public void setMensagem(String mensagem) {
            this.mensagem = mensagem;
        }

        public String getStacktrace() {
            return stacktrace;
        }

        public void setStacktrace(String stacktrace) {
            this.stacktrace = stacktrace;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

    }
}
