package br.com.credjam;

import java.awt.Desktop;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class CredjamApplication {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${app.auto-open-browser:true}")
    private boolean autoOpenBrowser;

    public static void main(String[] args) {
        SpringApplication.run(CredjamApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserOnStartup() {
        if (!autoOpenBrowser || !Desktop.isDesktopSupported()) {
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI("http://localhost:" + serverPort));
            }
        } catch (Exception ignored) {
            // Browser opening is best-effort and must not block application startup.
        }
    }
}
