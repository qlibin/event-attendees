package me.qlibin.demo;

import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.*;

@SpringBootApplication
@EnableCaching
public class Application implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Autowired
	JdbcTemplate jdbcTemplate;

    @Autowired
    EventProcessor eventProcessor;

    @Value("${start_processor_on_run}")
    boolean startProcessorOnRun = true;

    @Override
	public void run(ApplicationArguments args) throws Exception {

        bootstrapDB(args);

        if (startProcessorOnRun) {

            eventProcessor.start();

        }

    }

    private void bootstrapDB(ApplicationArguments args) {

        if (args.containsOption("clean")) {
            log.info("Removing old tables");
    		jdbcTemplate.execute("DROP TABLE IF EXISTS event_attendee");
	    	jdbcTemplate.execute("DROP TABLE IF EXISTS event");
        }

        log.info("Creating tables if not exist");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS event(id int, startTime int, attendees VARCHAR(128), " +
                "PRIMARY KEY i_event_id(id), KEY i_event_startTime(startTime), FULLTEXT INDEX fti_event_attendees(attendees))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS event_attendee(eventId int, attendeeId int, " +
                "KEY i_event_attendee_eventId(eventId), KEY i_event_attendee_attendeeId(attendeeId), " +
                "CONSTRAINT fk_event_attendee_event FOREIGN KEY (eventId) REFERENCES event (id))");

    }

    @Bean
    public GuavaCacheManager cacheManager() {
        GuavaCacheManager guavaCacheManager = new GuavaCacheManager();
        guavaCacheManager.setCacheBuilder(
                CacheBuilder.newBuilder()
                        .expireAfterAccess(5, TimeUnit.MINUTES)
                        .maximumSize(100000)
                        .recordStats()
        );
        return guavaCacheManager;
    }

}
