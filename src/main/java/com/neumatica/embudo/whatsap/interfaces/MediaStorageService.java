package com.neumatica.embudo.whatsap.interfaces;

public interface MediaStorageService {

    String downloadAndStore(
            String mediaId,
            String mimeType,
            String sha256
    );

    void delete(String storagePath);
}
