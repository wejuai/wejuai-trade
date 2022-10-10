package com.wejuai.trade.sync;

import com.wejuai.entity.mysql.User;
import com.wejuai.trade.core.CoreModule;
import com.wejuai.trade.core.repository.ChargeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableMongoRepositories(basePackageClasses = ChargeRepository.class)
@EnableScheduling
@EntityScan(basePackageClasses = {User.class})
@SpringBootApplication(scanBasePackageClasses = {CoreModule.class, SyncApplication.class})
public class SyncApplication {

    private static final Logger logger = LoggerFactory.getLogger(SyncApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SyncApplication.class);
        SimpleCommandLinePropertySource source = new SimpleCommandLinePropertySource(args);
        addDefaultProfile(app, source);
        Environment env = app.run(args).getEnvironment();
        String port = env.getProperty("server.port");
        logger.info("\nAccess URLs:\n----------------------------------------------------------\n"
                + "Local: \t\thttp://127.0.0.1:{}/actuator/health\n"
                + "----------------------------------------------------------", port);
    }

    /**
     * If no profile has been configured, set by default the "dev" profile.
     */
    private static void addDefaultProfile(SpringApplication app, SimpleCommandLinePropertySource source) {
        if (!source.containsProperty("spring.profiles.active") && !System.getenv().containsKey("SPRING_PROFILES_ACTIVE")) {
            app.setAdditionalProfiles("dev");
        }
    }

}
