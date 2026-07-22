package su26.uml.be.infrastructure.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

@Slf4j
@Component
@Order(1)
public class TimezoneVerificationConfig implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║              TIMEZONE VERIFICATION                     ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║ JVM default timezone : {}",
                TimeZone.getDefault().getID());
        log.info("║ System property      : {}",
                System.getProperty("user.timezone", "(not set)"));
        log.info("║ ZoneId.systemDefault : {}",
                ZoneId.systemDefault());
        log.info("║ LocalDateTime.now()  : {}",
                LocalDateTime.now());
        log.info("║ Expected TZ          : Asia/Ho_Chi_Minh (UTC+7)");
        log.info("╚══════════════════════════════════════════════════════════╝");

        // Sanity check — warn loudly if something is wrong
        String tz = TimeZone.getDefault().getID();
        if (!"Asia/Ho_Chi_Minh".equals(tz) && !"Asia/Saigon".equals(tz)) {
            log.error("!!! TIMEZONE MISMATCH !!! Expected Asia/Ho_Chi_Minh but got: {}. "
                    + "All timestamps will be incorrect!", tz);
        }
    }
}