package com.saigopal.browser.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Process;
import android.util.Log;

import com.saigopal.browser.database.RecordAction;
import com.saigopal.browser.unit.RecordUnit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class AdBlock implements com.saigopal.browser.data.repo.WhitelistAdapter {
    private static final String FILE = "hosts.txt";
    // Pre-size to avoid re-hashing and allocations during startup
    private static final Set<String> hosts = Collections.synchronizedSet(new HashSet<>(65000, 0.75f));
    private static final List<String> whitelist = Collections.synchronizedList(new ArrayList<>());
    @SuppressLint("ConstantLocale")
    private static final Locale locale = Locale.getDefault();
    private static volatile boolean loaded = false;

    private static void loadHosts(final Context context) {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            AssetManager manager = context.getAssets();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(manager.open(FILE)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (line.startsWith("0.0.0.0 ") || line.startsWith("127.0.0.1 ")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length > 1) line = parts[1];
                    }
                    int hashIdx = line.indexOf('#');
                    if (hashIdx != -1) line = line.substring(0, hashIdx).trim();
                    if (!line.isEmpty()) {
                        hosts.add(line.toLowerCase(locale));
                    }
                }
                loaded = true;
            } catch (IOException i) {
                Log.w("browser", "Error loading hosts", i);
            }
        });
        thread.setName("AdBlock-Loader");
        thread.setDaemon(true);
        thread.start();
    }

    private synchronized static void loadDomains(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        whitelist.clear();
        whitelist.addAll(action.listDomains(RecordUnit.TABLE_WHITELIST));
        action.close();
    }

    public static String getDomain(String url) throws URISyntaxException {
        if (url == null || url.trim().isEmpty()) return "";
        url = url.toLowerCase(locale);

        int index = url.indexOf('/', 8); // -> http://(7) and https://(8)
        if (index != -1) {
            url = url.substring(0, index);
        }

        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain == null) {
            return url;
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private final Context context;

    public AdBlock(Context context) {
        this.context = context.getApplicationContext();

        if (hosts.isEmpty()) {
            loadHosts(this.context);
        }
        loadDomains(this.context);
    }

    public boolean isWhite(String url) {
        if (url == null || url.isEmpty()) return false;
        if (url.toLowerCase(locale).contains("gamezop.com")) {
            return true;
        }
        try {
            String domain = getDomain(url);
            if (domain != null && (domain.equals("gamezop.com") || domain.endsWith(".gamezop.com"))) {
                return true;
            }
            for (String whiteDomain : whitelist) {
                if (domain.equals(whiteDomain) || domain.endsWith("." + whiteDomain)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            for (String domain : whitelist) {
                if (url.contains(domain)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDomainWhitelisted(String domain) {
        if (domain == null || domain.isEmpty()) return false;
        String clean = domain.trim().toLowerCase(locale);
        if (clean.equals("gamezop.com") || clean.endsWith(".gamezop.com")) {
            return true;
        }
        for (String w : whitelist) {
            if (clean.equals(w) || clean.endsWith("." + w)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAd(String url) {
        if (url == null || url.isEmpty()) return false;
        String domain;
        try {
            domain = getDomain(url);
        } catch (Exception u) {
            return false;
        }
        if (domain == null || domain.isEmpty()) return false;
        domain = domain.toLowerCase(locale);

        if (domain.equals("gamezop.com") || domain.endsWith(".gamezop.com")) {
            return false;
        }

        // Direct domain check
        if (hosts.contains(domain)) {
            return true;
        }

        // Subdomain check: e.g. "a.b.c.doubleclick.net" -> checks parent domains
        int dot = domain.indexOf('.');
        while (dot != -1) {
            domain = domain.substring(dot + 1);
            if (hosts.contains(domain)) {
                return true;
            }
            dot = domain.indexOf('.');
        }

        return false;
    }

    public synchronized void addDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) return;
        String clean = domain.trim().toLowerCase(locale);
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addDomain(clean, RecordUnit.TABLE_WHITELIST);
        action.close();
        if (!whitelist.contains(clean)) {
            whitelist.add(clean);
        }
    }

    public synchronized void removeDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) return;
        String clean = domain.trim().toLowerCase(locale);
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.deleteDomain(clean, RecordUnit.TABLE_WHITELIST);
        action.close();
        whitelist.remove(clean);
    }

    public synchronized void clearDomains() {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_WHITELIST);
        action.close();
        whitelist.clear();
    }
}
