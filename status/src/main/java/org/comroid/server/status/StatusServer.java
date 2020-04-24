package org.comroid.server.status;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.comroid.common.Polyfill;
import org.comroid.dreadpool.ThreadPool;
import org.comroid.server.status.entity.StatusServerEntity;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.cache.ProvidedCache;

import com.sun.net.httpserver.HttpServer;

public class StatusServer {
    public static void main(String[] args) throws IOException {
        new StatusServer(InetAddress.getLocalHost(), 580);
    }

    private static final ThreadGroup THREAD_GROUP = new ThreadGroup("comroid Status Server");

    public FastJSONLib getSerializationLibrary() {
        return FastJSONLib.fastJsonLib;
    }

    private final HandlerContainer                handlers = new HandlerContainer(this);
    private final HttpServer                      server;
    private final ThreadPool                      threadPool;
    private final Cache<UUID, StatusServerEntity> entityCache;

    private StatusServer(InetAddress host, int port) throws IOException {
        this.server      = HttpServer.create(new InetSocketAddress(host, port), port);
        this.threadPool  = ThreadPool.fixedSize(THREAD_GROUP, 8);
        this.entityCache = new ProvidedCache<UUID, StatusServerEntity>(threadPool,
                id -> Polyfill.failedFuture(new UnsupportedOperationException())
        ) {
            @Override
            public boolean canProvide() {
                return false;
            }
        };

        server.setExecutor(threadPool);
        server.createContext("/hello", handlers.HELLO);
        server.createContext("/status", handlers.STATUS_UPDATE);

        this.server.start();
    }
}
