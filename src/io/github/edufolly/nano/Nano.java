package io.github.edufolly.nano;

import java.net.ServerSocket;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Eduardo Folly
 */
public class Nano {

    private int port = 8088;
    private int maxConnections = 200;
    private String packageName;
    private String cacheDir = "cache";
    private String logConfigurationFile = "log4j2.xml";
    private String serverName = "Nano API IOT Server";

    public Nano() {
    }

    public int getPort() {
        return port;
    }

    public Nano setPort(int port) {
        this.port = port;
        return this;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public Nano setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    public Nano setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public Nano setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
        return this;
    }

    public String getLogConfigurationFile() {
        return logConfigurationFile;
    }

    public Nano setLogConfigurationFile(String logConfigurationFile) {
        this.logConfigurationFile = logConfigurationFile;
        return this;
    }

    public String getServerName() {
        return serverName;
    }

    public Nano setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public void start() {
        System.setProperty("log4j.configurationFile", logConfigurationFile);

        Logger logger = LogManager.getRootLogger();

        try {
            ServerSocket server = new ServerSocket(port, maxConnections);

            logger.info("{} aguardando conexões na porta {}.", serverName, port);

            while (true) {
                Socket connection = server.accept();
                (new ApiServer(this, connection)).start();
            }
        } catch (java.net.BindException ex) {
            logger.fatal("A porta {} está em uso por outro programa. ({})",
                    port, ex.getMessage());
            System.exit(0);
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
            System.exit(0);
        }

    }
}
