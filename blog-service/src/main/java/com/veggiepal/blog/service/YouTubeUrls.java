package com.veggiepal.blog.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pulls the video id out of a YouTube link. Only the id is kept: the service rebuilds the
 * embed and thumbnail URLs itself, so nothing a user typed ever reaches a page.
 */
public final class YouTubeUrls {

    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private static final Set<String> YOUTUBE_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com");

    private YouTubeUrls() {
    }

    public static Optional<String> extractId(String url) {

        if (url == null) {
            return Optional.empty();
        }

        URI uri;

        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();

        if (scheme == null || host == null || path == null
                || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
            return Optional.empty();
        }

        host = host.toLowerCase();
        String candidate = null;

        if (host.equals("youtu.be")) {
            candidate = path.startsWith("/") ? path.substring(1) : null;
        } else if (YOUTUBE_HOSTS.contains(host)) {
            if (path.equals("/watch")) {
                candidate = queryParam(uri.getRawQuery(), "v");
            } else if (path.startsWith("/embed/")) {
                candidate = path.substring("/embed/".length());
            } else if (path.startsWith("/shorts/")) {
                candidate = path.substring("/shorts/".length());
            }
        }

        return candidate != null && VIDEO_ID.matcher(candidate).matches()
                ? Optional.of(candidate)
                : Optional.empty();
    }

    private static String queryParam(String query, String name) {

        if (query == null) {
            return null;
        }

        for (String pair : query.split("&")) {
            if (pair.startsWith(name + "=")) {
                return pair.substring(name.length() + 1);
            }
        }

        return null;
    }
}
