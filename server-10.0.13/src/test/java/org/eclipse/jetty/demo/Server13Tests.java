package org.eclipse.jetty.demo;

import java.net.URI;

import org.eclipse.jetty.demo.common.CommonServerTests;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class Server13Tests extends CommonServerTests
{
    private Server server;

    @BeforeEach
    public void startServer() throws Exception
    {
        server = Server13.createServer(0);
        server.start();
    }

    @Override
    public URI getServerBaseURI()
    {
        return server.getURI().resolve("/");
    }

    @AfterEach
    public void stopServer()
    {
        LifeCycle.stop(server);
    }
}
