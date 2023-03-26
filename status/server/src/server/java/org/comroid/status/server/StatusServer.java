package org.comroid.status.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.io.FileHandle;
import org.comroid.spring.pushover.PushoverConfig;
import org.comroid.status.entity.Service;
import org.comroid.status.server.auth.Token;
import org.comroid.status.server.auth.TokenProvider;
import org.comroid.status.server.controller.ServiceController;
import org.comroid.status.server.repo.ServiceRepository;
import org.comroid.status.server.repo.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackageClasses = {ServiceController.class, ServiceRepository.class, TokenRepository.class, TokenProvider.class})
@EntityScan(basePackageClasses = {Service.class, Token.class})
@EnableJpaRepositories
@Configuration
public class StatusServer implements ContextualProvider.Underlying {
    //http://localhost:42641/services

    //public static final AdapterDefinition CONTEXT;
    public static final FileHandle PATH_BASE = new FileHandle("/srv/status/", true); // server path base
    public static final FileHandle DB_FILE = PATH_BASE.createSubFile("db.json");
    public static final FileHandle PUSHOVER_FILE = PATH_BASE.createSubFile("pushover.json");
    public static final String ADMIN_TOKEN_NAME = "admin$access$token";
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        SpringApplication.run(StatusServer.class);
    }

    @Bean
    public DataSource getDataSource(@Autowired ObjectMapper objectMapper) throws SQLException, IOException {
        var dbInfo = objectMapper.readValue(DB_FILE.openReader(), DBInfo.class);
        return DataSourceBuilder.create()
                .driverClassName(DriverManager.getDriver(dbInfo.url).getClass().getName())
                .url(dbInfo.url)
                .username(dbInfo.username)
                .password(dbInfo.password)
                .build();
    }

    @Bean
    public PushoverConfig getPushoverConfig(@Autowired ObjectMapper objectMapper) throws IOException {
        var pushoverInfo = objectMapper.readValue(PUSHOVER_FILE.openReader(), PushoverInfo.class);
        return PushoverConfig.builder()
                .token(pushoverInfo.token)
                .user(pushoverInfo.user)
                .title("status.comroid.org")
                .build();
    }

    @Bean
    public ScheduledExecutorService getExecutor() {
        return Executors.newScheduledThreadPool(4);
    }

    private static class DBInfo {
        public String url;
        public String username;
        public String password;
    }

    private static class PushoverInfo {
        public String token;
        public String user;
    }
}
