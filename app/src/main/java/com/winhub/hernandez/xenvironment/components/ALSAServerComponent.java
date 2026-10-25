package com.winhub.hernandez.xenvironment.components;

import com.winhub.hernandez.alsaserver.ALSAClientConnectionHandler;
import com.winhub.hernandez.alsaserver.ALSARequestHandler;
import com.winhub.hernandez.xconnector.UnixSocketConfig;
import com.winhub.hernandez.xconnector.XConnectorEpoll;
import com.winhub.hernandez.xenvironment.EnvironmentComponent;

public class ALSAServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private final UnixSocketConfig socketConfig;

    public ALSAServerComponent(UnixSocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = new XConnectorEpoll(socketConfig, new ALSAClientConnectionHandler(), new ALSARequestHandler());
        connector.setMultithreadedClients(true);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }
}
