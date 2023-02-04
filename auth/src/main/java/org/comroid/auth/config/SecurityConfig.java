package org.comroid.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.comroid.auth.controller.FlowController;
import org.comroid.auth.controller.GenericController;
import org.comroid.auth.entity.AuthService;
import org.comroid.auth.model.AuthorizationRequest;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.repo.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
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
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Component
@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
@Import(OAuth2AuthorizationServerConfiguration.class)
public class SecurityConfig extends AbstractAuthenticationProcessingFilter implements UserDetailsService, AuthenticationManager {
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private ServiceRepository services;

    protected SecurityConfig() {
        super(request -> "/oauth2/authorize".equals(request.getRequestURI()));
        setAuthenticationManager(this);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity security) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(security);
        security.formLogin().disable()
                .userDetailsService(this)
                .authenticationManager(this)
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .csrf().disable();
        return security.build();
    }

    @Bean
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
    public UserDetailsService users() {
        return username -> accounts.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Username " + username + " not found"));
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwk) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwk);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .build();
    }

    private static KeyPair generateRsaKey() {
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return accounts.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User with name " + username + " not found"));
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        System.out.println("authentication.isAuthenticated() = " + authentication.isAuthenticated());
        System.out.println("authentication.getAuthorities() = " + Arrays.toString(authentication.getAuthorities().toArray()));
        System.out.println("authentication.getCredentials() = " + authentication.getCredentials());
        System.out.println("authentication.getPrincipal() = " + authentication.getPrincipal());
        System.out.println("authentication.getDetails() = " + authentication.getDetails());
        return authentication;
    }

    @Override
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
            var auth = account.get().createAuthentication(encoder(), validDuration);
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

    private static void forwardResponse(HttpServletResponse response, String toUri) {
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", toUri);
    }
}
