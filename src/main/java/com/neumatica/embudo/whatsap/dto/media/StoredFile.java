package com.neumatica.embudo.whatsap.dto.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    private String originalFileName;

    private String storagePath;

    private String mimeType;

    private String sha256;

    private long size;
}
