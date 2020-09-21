package org.comroid.server.status.test.server;

import org.comroid.restless.adapter.okhttp.v4.OkHttp3Adapter;
import org.comroid.status.DependenyObject;
import org.comroid.status.StatusConnection;
import org.comroid.status.entity.Service;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.util.MultithreadUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class TestStatusServer {
    private StatusConnection connection;
    private Service service;

    @Before
    public void setup() {
        DependenyObject.Adapters.HTTP_ADAPTER = new OkHttp3Adapter();
        DependenyObject.Adapters.SERIALIZATION_ADAPTER = FastJSONLib.fastJsonLib;
        connection = new StatusConnection("test-dummy", "null", Executors.newScheduledThreadPool(4));
        service = connection.getService();
    }

    @Test(timeout = 20000)
    public void testPolling() {
        connection.refreshTimeout = 5;
        connection.crashedTimeout = 10;

        final Service join = connection.sendPoll().join();

        connection.sendPoll()
                .thenCompose(Service::requestStatus)
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
        connection.sendPoll()
                .thenCompose(Service::requestStatus)
                .thenAccept(status -> Assert.assertEquals(Service.Status.ONLINE, status))
                .join();
    }
}
