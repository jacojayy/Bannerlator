package com.winhub.hernandez.xconnector;

public interface ConnectionHandler {
    void handleConnectionShutdown(Client client);

    void handleNewConnection(Client client);
}