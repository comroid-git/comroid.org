package ws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.webkit.server.WebsocketServer;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketServerTest {
    public static final int PORT = 5674;
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws IOException, InterruptedException {
        logger.info("Starting Server");
        WebsocketServer server = new WebsocketServer(
                null,
                Executors.newScheduledThreadPool(8),
                "localhost",
                InetAddress.getLoopbackAddress(),
                PORT);

        server.getConnectionPipeline()
                .peek(connection -> {
                    logger.info("connection received");
                    connection.sendText("hello client");
                    //connection.sendText("hello client2");
                });

        Thread.sleep(1000);
        WebSocketClientTest.main(args);
    }

    public static void mains(String[] args) throws NoSuchAlgorithmException {
        WebSocketServerTest server = new WebSocketServerTest();
        try {
            server.test();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void test() throws IOException, NoSuchAlgorithmException {
        java.net.ServerSocket serverSocket = new java.net.ServerSocket(PORT);
        java.net.Socket client = warteAufAnmeldung(serverSocket);
        String nachricht = leseNachricht(client);
        logger.debug("empfangen: {}", nachricht);

        Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(nachricht);
        match.find();
        byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                + "Connection: Upgrade\r\n"
                + "Upgrade: websocket\r\n"
                + "Sec-WebSocket-Accept: "
                + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        String resp = new String(response);
        schreibeNachricht(client, resp);
        schreibeNachricht(client, "hello world");
        schreibeNachricht(client, "dis is websocket");
    }

    java.net.Socket warteAufAnmeldung(java.net.ServerSocket serverSocket) throws IOException {
        java.net.Socket socket = serverSocket.accept(); // blockiert, bis sich ein Client angemeldet hat
        return socket;
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

    void schreibeNachricht(java.net.Socket socket, String nachricht) throws IOException {
        PrintWriter printWriter =
                new PrintWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream()));
        printWriter.print(nachricht);
        printWriter.flush();
    }
}
