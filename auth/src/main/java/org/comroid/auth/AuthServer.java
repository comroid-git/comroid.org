package org.comroid.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.comroid.api.io.FileHandle;
import org.comroid.auth.entity.AuthService;
import org.comroid.auth.repo.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.integration.IntegrationDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.StreamSupport;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EntityScan(basePackages = "org.comroid.auth.entity")
@EnableJpaRepositories
@Configuration
public class AuthServer extends SpringBootServletInitializer implements WebMvcConfigurer {
    public static final FileHandle PATH_BASE = new FileHandle("/srv/auth/", true); // server path base
    public static final FileHandle DB_FILE = PATH_BASE.createSubFile("db.json");
    public static final FileHandle MAILER_FILE = PATH_BASE.createSubFile("mailer.json");

    public static void main(String[] args) {
        SpringApplication.run(AuthServer.class);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AuthServer.class);
    }

    @Bean
    public DataSource dataSource() throws IOException {
        var dbInfo = new ObjectMapper().readValue(DB_FILE.openReader(), DBInfo.class);
        return DataSourceBuilder.create()
                .url(dbInfo.url)
                .username(dbInfo.username)
                .password(dbInfo.password)
                .build();
    }

    @Bean
    public JavaMailSender mailSender() throws IOException {
        var mailerInfo = new ObjectMapper().readValue(MAILER_FILE.openReader(), MailerInfo.class);
        var sender = new JavaMailSenderImpl();

        sender.setHost(mailerInfo.host);
        sender.setPort(mailerInfo.port);

        sender.setUsername(mailerInfo.username);
        sender.setPassword(mailerInfo.password);

        var props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.enable", "true");
        //props.put("mail.debug", "true");

        return sender;
    }

    @Bean
    public ScheduledExecutorService executor() {
        return Executors.newScheduledThreadPool(4);
    }

    @Bean
    public IntegrationDataSourceScriptDatabaseInitializer customIntegrationDataSourceInitializer(DataSource dataSource) {
        // workaround from https://github.com/spring-projects/spring-boot/issues/28897#issuecomment-985389508
        return new IntegrationDataSourceScriptDatabaseInitializer(dataSource, new DatabaseInitializationSettings());
    }

    private static class DBInfo {
        public String url;
        public String username;
        public String password;
    }

    private static class MailerInfo {
        public String host;
        public int port;
        public String username;
        public String password;
    }
}
