package org.eclipse.jetty.demo;

import org.eclipse.jetty.demo.common.CookieServlet;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class Server13
{
    public static void main(String[] args) throws Exception
    {
        Server server = createServer(8080);
        server.start();
        server.join();
    }

    public static Server createServer(int port)
    {
        Server server = new Server();

        HttpConfiguration httpConfiguration = new HttpConfiguration();

        httpConfiguration.setHttpCompliance(HttpCompliance.RFC2616);

        httpConfiguration.setRequestCookieCompliance(CookieCompliance.RFC6265);
        httpConfiguration.setResponseCookieCompliance(CookieCompliance.RFC6265);

        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);
        httpConnectionFactory.setRecordHttpComplianceViolations(true);

        ServerConnector connector = new ServerConnector(server, httpConnectionFactory);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");
        servletContextHandler.addServlet(CookieServlet.class, "/cookie/*");

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.addHandler(servletContextHandler);

        server.setHandler(contexts);
        return server;
    }
}
