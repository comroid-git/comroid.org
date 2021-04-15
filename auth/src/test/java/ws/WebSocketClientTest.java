package ws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Polyfill;
import org.comroid.restless.REST;
import org.comroid.restless.adapter.java.JavaHttpAdapter;
import org.comroid.restless.socket.Websocket;
import org.comroid.restless.socket.WebsocketPacket;

import java.io.*;
import java.util.concurrent.ForkJoinPool;

public class WebSocketClientTest {
    private static final Logger logger = LogManager.getLogger();
    private static JavaHttpAdapter HTTP;

    public static void main(String[] args) throws InterruptedException {
        HTTP = new JavaHttpAdapter();

        logger.info("Creating WebSocket");
        Websocket socket = HTTP.createWebSocket(
                ForkJoinPool.commonPool(),
                e -> logger.error("Error in websocket", e),
                Polyfill.uri("ws://127.0.0.1:5674"),
                new REST.Header.List()
        ).join();

        socket.getEventPipeline()
                .filterKey(t -> t == WebsocketPacket.Type.OPEN)
                .peek(packet -> {
                    logger.info("WebSocket Opened! {}", packet);
                    socket.send("hello server");
                });

        socket.getEventPipeline()
                .peekBoth((t, p) -> logger.info("Client received: {} - {}", t, p))
                .filterKey(t -> t == WebsocketPacket.Type.ERROR)
                .peek(packet -> packet.getError().consume(e -> logger.error("Error Occurred", e)));

        Object o = new Object();
        synchronized (o) {
            o.wait();
        }
    }
    public static void mains(String[] args) {
        WebSocketClientTest client = new WebSocketClientTest();
        try {
            client.test();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void test() throws IOException {
        String ip = "127.0.0.1"; // localhost
        int port = 5674;
        java.net.Socket socket = new java.net.Socket(ip,port); // verbindet sich mit Server
        String zuSendendeNachricht = "warum funktioniert der scheiß nicht!";
        schreibeNachricht(socket, zuSendendeNachricht);
        String empfangeneNachricht = leseNachricht(socket);
        System.out.println(empfangeneNachricht);
    }
    void schreibeNachricht(java.net.Socket socket, String nachricht) throws IOException {
        PrintWriter printWriter =
                new PrintWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream()));
        printWriter.print(nachricht);
        printWriter.flush();
    }
    String leseNachricht(java.net.Socket socket) throws IOException {
        BufferedReader bufferedReader =
                new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));
        char[] buffer = new char[200];
        int anzahlZeichen = bufferedReader.read(buffer, 0, 200); // blockiert bis Nachricht empfangen
        String nachricht = new String(buffer, 0, anzahlZeichen);
        return nachricht;
    }
}
