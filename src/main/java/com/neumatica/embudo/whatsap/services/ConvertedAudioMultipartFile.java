package com.neumatica.embudo.whatsap.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConvertedAudioMultipartFile implements MultipartFile {

    private final byte[] bytes;
    private final String filename;

    public ConvertedAudioMultipartFile(Path path, String filename)
            throws IOException {
        this.bytes = Files.readAllBytes(path);
        this.filename = filename;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return "audio/ogg";
    }

    @Override
    public boolean isEmpty() {
        return bytes.length == 0;
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    @Override
    public byte[] getBytes() {
        return bytes.clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void transferTo(Path dest) throws IOException {
        Files.write(dest, bytes);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException {
        Files.write(dest.toPath(), bytes);
    }
}

