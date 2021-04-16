package org.comroid.oauth.server;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.auth.server.AuthServer;
import org.comroid.auth.user.UserAccount;
import org.zalando.stups.tokens.*;

import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public class OAuth2Server {
    public OAuth2Server(
            ContextualProvider contextualProvider,
            Executor executor,
            String baseUrl,
            InetAddress inetAddress,
            int port
    ) throws IOException {
        ServerSocketChannel open = ServerSocketChannel.open();


        // init user
        UserAccount user = null;

        // init tokens
        AccessTokensBuilder builder = Tokens.createAccessTokensWithUri(Polyfill.uri(baseUrl + "token"));
        builder.usingUserCredentialsProvider(user.asUserCredentialsProvider());
        builder.manageToken("")
                .

        // build tokens
        AccessTokens tokens = builder.start();

        tokens.getAccessToken("hello").
    }
}
