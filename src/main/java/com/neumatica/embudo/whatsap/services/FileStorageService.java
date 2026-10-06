package com.neumatica.embudo.whatsap.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.neumatica.embudo.whatsap.dto.media.StoredFile;

@Service
public class FileStorageService {

    private final Path storageRoot;

    public FileStorageService(
            @Value("${app.file-storage.path:uploads/whatsapp}") String storagePath
    ) {

        this.storageRoot = Paths.get(storagePath)
                .toAbsolutePath()
                .normalize();

        try {

            Files.createDirectories(storageRoot);

        } catch (IOException e) {

            throw new RuntimeException(
                    "No fue posible crear el directorio de almacenamiento: "
                            + storageRoot,
                    e
            );
        }
    }

    public StoredFile store(
            MultipartFile file,
            UUID conversationId
    ) {

        validateFile(file);

        if (conversationId == null) {

            throw new IllegalArgumentException(
                    "El conversationId es obligatorio."
            );
        }

        String originalFileName =
                StringUtils.cleanPath(
                        file.getOriginalFilename() == null
                                ? "archivo"
                                : file.getOriginalFilename()
                );

        if (originalFileName.contains("..")) {

            throw new IllegalArgumentException(
                    "El nombre del archivo no es válido."
            );
        }

        String mimeType =
                file.getContentType();

        if (mimeType == null || mimeType.isBlank()) {

            mimeType = "application/octet-stream";
        }

        String extension =
                getExtension(originalFileName);

        String generatedFileName =
                UUID.randomUUID()
                        + extension;

        Path conversationDirectory =
                storageRoot.resolve(
                        conversationId.toString()
                ).normalize();

        try {

            Files.createDirectories(
                    conversationDirectory
            );

            Path target =
                    conversationDirectory
                            .resolve(generatedFileName)
                            .normalize();

            if (!target.startsWith(conversationDirectory)) {

                throw new IllegalArgumentException(
                        "Ruta de archivo no válida."
                );
            }

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String sha256 =
                    calculateSha256(target);

            return StoredFile.builder()
                    .originalFileName(originalFileName)
                    .storagePath(
                            target.toString()
                    )
                    .mimeType(mimeType)
                    .sha256(sha256)
                    .size(file.getSize())
                    .build();

        } catch (IOException e) {

            throw new RuntimeException(
                    "No fue posible almacenar el archivo.",
                    e
            );
        }
    }

    public void delete(String storagePath) {

        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        try {

            Files.deleteIfExists(
                    Paths.get(storagePath)
            );

        } catch (IOException e) {

            System.err.println(
                    "No fue posible eliminar el archivo: "
                            + storagePath
            );
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "El archivo es obligatorio."
            );
        }

        /*
         * 100 MB como límite inicial.
         *
         * WhatsApp puede imponer límites específicos
         * dependiendo del tipo de multimedia.
         */
        long maxSize =
                100L * 1024L * 1024L;

        if (file.getSize() > maxSize) {

            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo permitido de 100 MB."
            );
        }
    }

    private String getExtension(
            String fileName
    ) {

        int index =
                fileName.lastIndexOf(".");

        if (index == -1) {
            return "";
        }

        return fileName.substring(index)
                .replaceAll(
                        "[^a-zA-Z0-9.]",
                        ""
                );
    }

    private String calculateSha256(
            Path file
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream =
                         Files.newInputStream(file)) {

                byte[] buffer =
                        new byte[8192];

                int bytesRead;

                while (
                        (bytesRead =
                                inputStream.read(buffer))
                                != -1
                ) {

                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            return HexFormat.of()
                    .formatHex(
                            digest.digest()
                    );

        } catch (
                NoSuchAlgorithmException
                        | IOException e
        ) {

            throw new RuntimeException(
                    "No fue posible calcular SHA-256.",
                    e
            );
        }
    }
}
