package org.comroid.server.status;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

import org.comroid.dreadpool.ThreadPool;
import org.comroid.listnr.EventHub;
import org.comroid.server.status.entity.StatusServerEntity;
import org.comroid.server.status.entity.service.Service;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.node.UniObjectNode;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class StatusServer {
    private final        HttpServer                      server;
    private final        ThreadPool                      threadPool;
    private final        EventHub<UniObjectNode>         eventHub;
    private final        EventContainer                  eventContainer;
    private final        Cache<UUID, StatusServerEntity> entityCache;

    private StatusServer(InetAddress host, int port) throws IOException {
        this.server         = HttpServer.create(new InetSocketAddress(host, port), port);
        this.threadPool     = ThreadPool.fixedSize(THREAD_GROUP, 8);
        this.eventHub       = new EventHub<>(threadPool);
        this.eventContainer = new EventContainer(this);
        this.entityCache    = new BasicCache<>();

        server.setExecutor(threadPool);
        server.createContext("/", eventContainer.CONTEXT_HANDLER);

        this.server.start();
    }

    public FastJSONLib getSerializationLibrary() {
        return FastJSONLib.fastJsonLib;
    }

    public final Cache<UUID, StatusServerEntity> getEntityCache() {
        return entityCache;
    }

    public final HttpServer getServer() {
        return server;
    }

    public final ThreadPool getThreadPool() {
        return threadPool;
    }

    public final EventHub<HttpExchange, UniObjectNode> getEventHub() {
        return eventHub;
    }

    public final EventContainer getEventContainer() {
        return eventContainer;
    }

    public final Optional<Service> getServiceByID(UUID id) {
        return entityCache.stream(it -> it.equals(id))
                .findAny()
                .map(Service.class::cast);
    }

    public static void main(String[] args) throws IOException {
        new StatusServer(InetAddress.getLocalHost(), 580);
    }
    private static final ThreadGroup                     THREAD_GROUP = new ThreadGroup("comroid Status Server");
}
