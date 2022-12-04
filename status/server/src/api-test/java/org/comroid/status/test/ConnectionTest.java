package org.comroid.status.test;

import org.comroid.status.StatusConnection;
import org.junit.Before;
import org.junit.Test;

public class ConnectionTest {
    private StatusConnection connection;

    @Before
    public void setup() {
        connection = new StatusConnection("test-dummy", "null");
    }

    @Test
    public void testRequestService() {
        var srv = connection.requestServiceByName("test-dummy").join();

        System.out.println(srv);
    }
}
