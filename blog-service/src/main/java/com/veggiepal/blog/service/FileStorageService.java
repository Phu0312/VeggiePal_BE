package com.veggiepal.blog.service;

public interface FileStorageService {

    /** Stores the object and returns its public URL. */
    String upload(String key, byte[] content, String contentType);

    /** Deletes the object behind a URL returned by {@link #upload}; other URLs are ignored. */
    void delete(String url);
}
