package com.winhub.hernandez.alsaserver;

import com.winhub.hernandez.xconnector.Client;
import com.winhub.hernandez.xconnector.ConnectionHandler;

public class ALSAClientConnectionHandler implements ConnectionHandler {
    @Override
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(new ALSAClient());
    }

    @Override
    public void handleConnectionShutdown(Client client) {
        ((ALSAClient)client.getTag()).release();
    }
}
