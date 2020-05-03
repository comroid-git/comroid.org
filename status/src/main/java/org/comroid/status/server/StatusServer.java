package org.comroid.status.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

import org.comroid.common.ref.Reference;
import org.comroid.dreadpool.ThreadPool;
import org.comroid.listnr.EventHub;
import org.comroid.status.server.entity.ServerEntity;
import org.comroid.status.server.entity.Service;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.node.UniObjectNode;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class StatusServer {
    public static final int                 PORT         = 42641; // hardcoded in server, do not change
    public static final ThreadGroup         THREAD_GROUP = new ThreadGroup("comroid Status Server");
    private final       HttpServer          server;
    private final       ThreadPool                threadPool;
    private final       Cache<UUID, ServerEntity> entityCache;

    private StatusServer(InetAddress host, int port) throws IOException {
        this.server      = HttpServer.create(new InetSocketAddress(host, port), port);
        this.threadPool  = ThreadPool.fixedSize(THREAD_GROUP, 8);
        this.entityCache = new BasicCache<>();

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

    public final Optional<Service> getServiceByName(String name) {
        return entityCache.stream()
                .filter(ref -> !ref.isNull())
                .filter(ref -> ref.process()
                        .test(Service.class::isInstance))
                .map(Reference::requireNonNull)
                .map(Service.class::cast)
                .filter(service -> service.getName()
                        .equals(name))
                .findFirst();
    }

    public static void main(String[] args) throws IOException {
        DiscordBot.INSTANCE.supplyToken(args[0]);

        instance = new StatusServer(InetAddress.getLocalHost(), PORT);

        Runtime.getRuntime()
                .addShutdownHook(new Thread(instance::shutdown));
    }

    private void shutdown() {
    }

    public static StatusServer instance;
}
