package org.comroid.server.status.test.server;

import org.comroid.mutatio.span.Span;
import org.comroid.restless.REST;
import org.comroid.restless.adapter.okhttp.v4.OkHttp4Adapter;
import org.comroid.restless.body.BodyBuilderType;
import org.comroid.restless.endpoint.CompleteEndpoint;
import org.comroid.status.StatusConnection;
import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.util.StandardValueType;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.util.MultithreadUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.comroid.restless.CommonHeaderNames.AUTHORIZATION;


public class StatusServerTests {
    public static final String LOCAL_BASE_URL = "localhost:42641/";
    private static final CompleteEndpoint pollEndpoint = LocalEndpoint.POLL.complete("test-dummy");
    private StatusServer testServer;
    private StatusConnection connection;
    private Service service;

    @Before
    public void setup() throws IOException {
        DependenyObject.Adapters.HTTP_ADAPTER = new OkHttp4Adapter();
        DependenyObject.Adapters.SERIALIZATION_ADAPTER = FastJSONLib.fastJsonLib;

        // initialize test site
        StatusServer.main("--token", "null", "--test");
        connection = new StatusConnection("test-dummy", "null", Executors.newScheduledThreadPool(4));

        service = connection.getService();
    }

    //@Test(timeout = 20000)
    public void testPolling() {
        connection.refreshTimeout = 5;
        connection.crashedTimeout = 10;

        final Service join = sendPoll().join();

        sendPoll().thenCompose(Service::requestStatus)
                .thenAccept(status -> Assert.assertEquals(Service.Status.ONLINE, status))
                .join();
        MultithreadUtil.futureAfter(6, TimeUnit.SECONDS)
                .thenCompose(nil -> service.requestStatus())
                .thenAccept(status -> Assert.assertEquals(Service.Status.NOT_RESPONDING, status))
                .join();
        MultithreadUtil.futureAfter(6, TimeUnit.SECONDS)
                .thenCompose(nil -> service.requestStatus())
                .thenAccept(status -> Assert.assertEquals(Service.Status.CRASHED, status))
                .join();
        sendPoll().thenCompose(Service::requestStatus)
                .thenAccept(status -> Assert.assertEquals(Service.Status.ONLINE, status))
                .join();
    }

    @After
    public void destroy() {
        testServer.close();
    }

    private CompletableFuture<Service> sendPoll() {
        return connection.getRest().request(Service.Bind.Root)
                .method(REST.Method.POST)
                .endpoint(pollEndpoint)
                .addHeader(AUTHORIZATION, "null")
                .buildBody(BodyBuilderType.OBJECT, obj -> {
                    obj.put("status", StandardValueType.INTEGER, Service.Status.ONLINE.getValue());
                    obj.put("expected", StandardValueType.INTEGER, 5);
                    obj.put("timeout", StandardValueType.INTEGER, 10);
                })
                .execute$autoCache(Service.Bind.Name, connection.getServiceCache())
                .thenApply(Span::requireNonNull);
    }
}
