package org.comroid.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.comroid.auth.controller.FlowController;
import org.comroid.auth.controller.GenericController;
import org.comroid.auth.entity.AuthService;
import org.comroid.auth.entity.UserAccount;
import org.comroid.auth.model.AuthorizationRequest;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.repo.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityConfig {
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private ServiceRepository services;
    private final Logger log = Logger.getLogger("SecurityConfig");

    private static KeyPair generateRsaKey() { //(6)
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    private static void forwardResponse(HttpServletResponse response, String toUri) {
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", toUri);
    }

    private static String getRedirectUrl(HttpServletRequest request) {
        return Arrays.stream(request.getQueryString().split("&"))
                .filter(str -> str.startsWith("redirect_url"))
                .findAny()
                .map(str -> str.substring("redirect_url=".length()))
                .orElseThrow();
    }

    private class TestProvider implements AuthenticationProvider {
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            return authentication;
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return false;
        }
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public AuthenticationProvider authenticationProvider() {
        return new TestProvider();
    }

    @Bean
    public RememberMeServices rememberMeServices() {
        return new SpringSessionRememberMeServices();
    }

    @Bean //(1)
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/oauth2/**").authenticated());
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());    // Enable OpenID Connect 1.0
        http
                // Redirect to the login page when not authenticated from the
                // authorization endpoint
                .exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")).and()
                // Accept access tokens for User Info and/or Client Registration
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .userDetailsService(userDetailsService())
                .authenticationManager(authenticationManager())
                .authenticationProvider(authenticationProvider())
                .rememberMe().rememberMeServices(rememberMeServices()).alwaysRemember(true).and()
                .formLogin()
        ;

        return http.build();
    }

    @Bean //(2)
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/login**", "/logout**", "/register**", "/error**", "/favicon.ico").permitAll()
                        .anyRequest().fullyAuthenticated())
                .exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")).and()
                .userDetailsService(userDetailsService())
                .authenticationManager(authenticationManager())
                .authenticationProvider(authenticationProvider())
                .rememberMe().rememberMeServices(rememberMeServices()).alwaysRemember(true).and()
                .formLogin().successHandler(new SimpleUrlAuthenticationSuccessHandler("/account"));
                        //(request, response, authentication) -> accounts.setSessionId(((UserAccount)authentication.getPrincipal()).getId(),request.getSession(true).getId()));
        return http.build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public UserDetailsService userDetailsService() {
        return
                username -> accounts.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Username " + username + " not found"));
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RegisteredClientRepository clients() {
        return new RegisteredClientRepository() {
            @Override
            public void save(RegisteredClient registeredClient) {
                var service = services.findById(registeredClient.getId()).orElseThrow(() -> new ProviderNotFoundException(registeredClient.getId()));
                service.setName(registeredClient.getClientName());
                service.setRequiredScope(service.getRequiredScope());
                services.save(service);
            }

            @Override
            public RegisteredClient findById(String id) {
                return services.findById(id).map(AuthService::getClient).orElse(null);
            }

            @Override
            public RegisteredClient findByClientId(String clientId) {
                return services.findById(clientId).map(AuthService::getClient).orElse(null);
            }
        };
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public AuthenticationManager authenticationManager() {
        return authentication -> {
            var principal = authentication.getPrincipal().toString();
            var found = accounts.findByUsername(principal)
                    .or(() -> accounts.findByEmail(principal));
            if (found.isEmpty())
                return authentication;
            var user = found.get();
            if (encoder().matches(authentication.getCredentials().toString(), user.getPasswordHash())) {
                return user.createAuthentication(Duration.ofDays(7));
                /*return new OAuth2RefreshTokenAuthenticationToken(
                        Base64.getEncoder().encodeToString(generateRsaKey().getPublic().getEncoded()),
                        authentication,
                        null,
                        null
                );*/
            }
            return authentication;
        };
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean //(5)
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean //(7)
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean //(8)
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
    //@Override

    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, ServletException {
        var session = request.getSession(true);
        var sessionId = session.getId();
        var account = accounts.findBySessionId(sessionId)
                .or(() -> Optional.ofNullable(FlowController.pendingAuthorizations.getOrDefault(sessionId, null))
                        .map(AuthorizationRequest::sessionId)
                        .flatMap(accounts::findBySessionId))
                .or(() -> request.getParameterMap().containsKey("password")
                        ? Optional.ofNullable(GenericController.tryLogin(accounts, null, session,
                                request.getParameter("email"),
                                request.getParameter("password"),
                                encoder(),
                                Collections.emptyMap()))
                        .filter("redirect:/account"::equals)
                        .flatMap($ -> accounts.findBySessionId(session.getId()))
                        : Optional.empty());
        var serviceId = Arrays.stream(request.getParameterMap().getOrDefault("client_id", new String[0]))
                .findAny()
                .or(() -> Optional.ofNullable(FlowController.pendingAuthorizations.getOrDefault(sessionId, null))
                        .map(AuthorizationRequest::clientId));
        var service = serviceId.flatMap(services::findById);
        if (service.isEmpty())
            throw new HttpStatusCodeException(HttpStatus.NOT_FOUND, "Client with id " + serviceId.orElse("<empty>") + " not found") {
            };
        else if (account.isPresent()) {
            var validDuration = Duration.ofDays(30);
            var auth = account.get().createAuthentication(validDuration);
            var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                    encoder().encode(account.get().getId() + ':' + service.get().getId() + ':' + sessionId),
                    Instant.now(),
                    Instant.now().plus(validDuration));
            Optional.ofNullable(FlowController.pendingAuthorizations.getOrDefault(sessionId, null))
                    .map(AuthorizationRequest::redirectUri)
                    .or(() -> Optional.ofNullable(request.getParameter("redirect_uri")))
                    .ifPresent(redirectUri -> forwardResponse(response, redirectUri));
            return new OAuth2AccessTokenAuthenticationToken(service.get().getClient(), auth, accessToken);
        } else {
            FlowController.pendingAuthorizations.remove(sessionId);
            FlowController.pendingAuthorizations.put(sessionId, new AuthorizationRequest(
                    request.getParameter("client_id"),
                    request.getParameter("redirect_uri"),
                    sessionId,
                    request.getRequestURL() + "?" + String.join("&", request.getParameterMap()
                            .entrySet()
                            .stream()
                            .filter(entry -> !"password".equals(entry.getKey()))
                            .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()[0]))
                            .toArray(String[]::new))
            ));
            forwardResponse(response, "/flow/login/" + sessionId);
        }
        return null;
    }
}
