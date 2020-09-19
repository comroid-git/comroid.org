package org.comroid.server.status.test.api;

import org.comroid.status.StatusConnection;
import org.comroid.status.entity.Service;
import org.comroid.util.MultithreadUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestStatusConnection {
    private StatusConnection connection;
    private Service service;

    @Before
    public void setup() {
        connection = new StatusConnection("test-dummy", "null", Executors.newScheduledThreadPool(4));
        service = connection.getService();
    }

    //@Test(timeout = 20000) todo: Fix test
    public void testPolling() {
        connection.refreshTimeout = 5;
        connection.crashedTimeout = 10;

        connection.sendPoll()
                .thenCompose(Service::requestStatus)
                .thenAccept(status -> Assert.assertEquals(Service.Status.ONLINE, status))
                .thenCompose(nil -> MultithreadUtil.futureAfter(6, TimeUnit.SECONDS))
                .thenCompose(nil -> service.requestStatus())
                .thenAccept(status -> Assert.assertEquals(Service.Status.NOT_RESPONDING, status))
                .thenCompose(nil -> MultithreadUtil.futureAfter(6, TimeUnit.SECONDS))
                .thenCompose(nil -> service.requestStatus())
                .thenAccept(status -> Assert.assertEquals(Service.Status.CRASHED, status))
                .thenCompose(nil -> connection.sendPoll())
                .thenCompose(nil -> service.requestStatus())
                .thenAccept(status -> Assert.assertEquals(Service.Status.ONLINE, status))
                .join();
    }
}
