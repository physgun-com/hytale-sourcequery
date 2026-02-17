package com.infraly.sourcequery;

import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.infraly.sourcequery.server.A2SQueryServer;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceQueryPlugin extends JavaPlugin {

    private static final String VERSION = "1.1.0";
    private static final String GITHUB_API = "https://api.github.com/repos/physgun-com/hytale-sourcequery/releases/latest";

    private A2SQueryServer queryServer;
    private ScheduledExecutorService scheduler;

    public SourceQueryPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        int port = resolveQueryPort();

        queryServer = new A2SQueryServer(port, getLogger());

        try {
            queryServer.start();
            getLogger().at(Level.INFO).log("A2S query server listening on UDP port %d", port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            getLogger().at(Level.SEVERE).log("Failed to start A2S query server: interrupted");
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SourceQuery-UpdateCheck");
            t.setDaemon(true);
            return t;
        });

        if (this.shouldCheckForUpdates()) {
            scheduler.schedule(this::checkForUpdates, 30, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (queryServer != null) {
            queryServer.stop();
        }
    }

    private int resolveQueryPort() {
        String env = System.getenv("QUERY_PORT");
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException e) {
                getLogger().at(Level.WARNING).log("Invalid QUERY_PORT: %s", env);
            }
        }
        return Options.getOptionSet().valuesOf(Options.BIND).getFirst().getPort() + 1;
    }

    private boolean shouldCheckForUpdates() {
        String env = System.getenv("SOURCEQUERY_UPDATE_CHECK");
        if (env != null) {
            return env.equalsIgnoreCase("true") || env.equalsIgnoreCase("1");
        }
        return true;
    }

    private void checkForUpdates() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API))
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"");
                Matcher matcher = pattern.matcher(response.body());

                if (matcher.find()) {
                    String latest = matcher.group(1);
                    if (!VERSION.equals(latest)) {
                        getLogger().at(Level.INFO).log("Update available: v%s (current: v%s)", latest, VERSION);
                        getLogger().at(Level.INFO).log("Download: https://github.com/physgun-com/hytale-sourcequery/releases/latest");
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail - update check is non-critical
        }
    }
}
