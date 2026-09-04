package com.neumatica.embudo.whatsap.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.neumatica.embudo.whatsap.repository.MediaStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * Implementación del servicio encargado de descargar
 * y almacenar archivos multimedia provenientes de Meta.
 *
 * <p>El proceso realizado por esta clase es:
 *
 * <ol>
 *     <li>Recibir el mediaId de WhatsApp.</li>
 *     <li>Consultar Meta para obtener la URL temporal.</li>
 *     <li>Descargar el archivo desde dicha URL.</li>
 *     <li>Validar opcionalmente su SHA-256.</li>
 *     <li>Almacenar el archivo físicamente.</li>
 *     <li>Retornar una ruta lógica para almacenarla en BD.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaStorageServiceImpl
        implements MediaStorageService {

    /*
     * Cliente HTTP utilizado para comunicarse con Meta.
     */
    private final RestClient restClient = RestClient.create();

    /*
     * Descarga y almacena un archivo multimedia.
     */
    @Override
    public String downloadAndStore(
            String mediaId,
            String mimeType,
            String sha256) {

        /*
         * Validamos que exista el identificador
         * del recurso multimedia.
         */
        if (mediaId == null || mediaId.isBlank()) {

            throw new IllegalArgumentException(
                    "El mediaId no puede ser null o vacío."
            );
        }

        /*
         * Primero consultamos a Meta para obtener
         * la URL temporal de descarga.
         */
        MediaMetadata metadata =
                getMediaMetadata(mediaId);

        if (metadata == null
                || metadata.url() == null
                || metadata.url().isBlank()) {

            throw new IllegalStateException(
                    "Meta no proporcionó una URL para el mediaId: "
                    + mediaId
            );
        }

        /*
         * Determinamos la extensión a partir del MIME.
         */
        String extension =
                resolveExtension(mimeType);

        /*
         * Generamos un nombre completamente interno.
         *
         * No utilizamos directamente el nombre enviado
         * por el usuario para evitar problemas de seguridad.
         */
        String generatedFileName =
                UUID.randomUUID() + extension;

        /*
         * Creamos el directorio de almacenamiento.
         */
        Path directory =
                createStorageDirectory();

        /*
         * Ruta física final.
         */
        Path targetFile =
                directory.resolve(generatedFileName);

        /*
         * Descargamos el archivo desde Meta.
         */
        downloadFile(
                metadata.url(),
                targetFile
        );

        /*
         * Si WhatsApp proporcionó SHA-256,
         * verificamos que el archivo descargado
         * coincida con el hash esperado.
         */
        if (sha256 != null && !sha256.isBlank()) {

            validateSha256(
                    targetFile,
                    sha256
            );
        }

        /*
         * Generamos una ruta lógica.
         *
         * Esta es la que posteriormente almacenaremos
         * en la base de datos.
         */
        String storagePath =
                buildStoragePath(
                        generatedFileName
                );

        log.info(
                "Multimedia almacenado correctamente. mediaId={}, path={}",
                mediaId,
                storagePath
        );

        return storagePath;
    }

    /*
     * Consulta a Meta para obtener los metadatos
     * y la URL temporal del archivo multimedia.
     */
    private MediaMetadata getMediaMetadata(
            String mediaId) {

        String url = String.format(
                "https://graph.facebook.com/%s/%s",
                "v25.0/",
                mediaId
        );

        return restClient.get()
                .uri(url)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer "
                                + "EAAWNon6bi60BSBJQsDz4UFjXGJGEq39Uuxo9fcgNj4QkDpm3WfrPdiZCUZBdGOzNU3u8A1tplKSXfGTlS8KC6NmARGiljZCKnQjTGw8ffJi89KosBh77yAKxZAI0qhWrOkZB2QIpnqyovQe5gchBEI0dX5M7pduHdIrITgOrpUxgMZCwRBiXGaPh7nOA9SHQZDZD"
                )
                .retrieve()
                .body(MediaMetadata.class);
    }

    /*
     * Descarga físicamente el archivo desde la URL
     * temporal proporcionada por Meta.
     */
    private void downloadFile(
            String downloadUrl,
            Path targetFile) {

        restClient.get()
                .uri(downloadUrl)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer "
                                + "EAAWNon6bi60BSBJQsDz4UFjXGJGEq39Uuxo9fcgNj4QkDpm3WfrPdiZCUZBdGOzNU3u8A1tplKSXfGTlS8KC6NmARGiljZCKnQjTGw8ffJi89KosBh77yAKxZAI0qhWrOkZB2QIpnqyovQe5gchBEI0dX5M7pduHdIrITgOrpUxgMZCwRBiXGaPh7nOA9SHQZDZD"
                )
                .exchange((request, response) -> {

                    HttpStatusCode status =
                            response.getStatusCode();

                    if (!status.is2xxSuccessful()) {

                        throw new IllegalStateException(
                                "Meta respondió con HTTP "
                                        + status.value()
                        );
                    }

                    try (InputStream inputStream =
                                 response.getBody()) {

                        if (inputStream == null) {

                            throw new IllegalStateException(
                                    "Meta devolvió un body vacío."
                            );
                        }

                        Files.copy(
                                inputStream,
                                targetFile,
                                StandardCopyOption.REPLACE_EXISTING
                        );

                    } catch (IOException exception) {

                        throw new IllegalStateException(
                                "Error almacenando el archivo multimedia.",
                                exception
                        );
                    }

                    return null;
                });
    }

    /*
     * Crea la estructura de directorios donde
     * serán almacenados los archivos.
     *
     * <p>Ejemplo:
     *
     * uploads/whatsapp/2026/09/
     */
    private Path createStorageDirectory() {

        LocalDate now =
                LocalDate.now();

        Path directory =
                Paths.get(
                        "./uploads/whatsapp",
                        String.valueOf(now.getYear()),
                        String.format(
                                "%02d",
                                now.getMonthValue()
                        )
                );

        try {

            Files.createDirectories(
                    directory
            );

            return directory;

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "No fue posible crear el directorio multimedia: "
                            + directory,
                    exception
            );
        }
    }

    /*
     * Construye la ruta lógica que será almacenada
     * en la base de datos.
     */
    private String buildStoragePath(
            String fileName) {

        LocalDate now =
                LocalDate.now();

        return String.format(
                "whatsapp/%d/%02d/%s",
                now.getYear(),
                now.getMonthValue(),
                fileName
        );
    }

    /*
     * Determina una extensión segura utilizando
     * el tipo MIME recibido.
     */
    private String resolveExtension(
            String mimeType) {

        if (mimeType == null
                || mimeType.isBlank()) {

            return ".bin";
        }

        return switch (
                mimeType.toLowerCase()
        ) {

            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";

            case "video/mp4" -> ".mp4";
            case "video/3gpp" -> ".3gp";

            case "audio/ogg" -> ".ogg";
            case "audio/mpeg" -> ".mp3";
            case "audio/mp4" -> ".m4a";
            case "audio/aac" -> ".aac";

            case "application/pdf" -> ".pdf";

            case "application/msword" -> ".doc";

            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    -> ".docx";

            case "application/vnd.ms-excel"
                    -> ".xls";

            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    -> ".xlsx";

            case "application/vnd.ms-powerpoint"
                    -> ".ppt";

            case "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    -> ".pptx";

            default -> ".bin";
        };
    }

    /*
     * Calcula el SHA-256 del archivo descargado y
     * lo compara con el hash proporcionado por WhatsApp.
     */
    private void validateSha256(
            Path file,
            String expectedSha256) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream =
                         Files.newInputStream(file)) {

                byte[] buffer =
                        new byte[8192];

                int bytesRead;

                while ((bytesRead =
                        inputStream.read(buffer)) != -1) {

                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            String calculatedSha256 =
                    HexFormat.of()
                            .formatHex(
                                    digest.digest()
                            );

            if (!calculatedSha256.equalsIgnoreCase(
                    expectedSha256)) {

                /*
                 * Si el archivo no coincide con el hash,
                 * eliminamos el archivo corrupto.
                 */
                Files.deleteIfExists(file);

                throw new IllegalStateException(
                        "El SHA-256 del archivo multimedia "
                                + "no coincide con el proporcionado por Meta."
                );
            }

        } catch (NoSuchAlgorithmException
                 | IOException exception) {

            throw new IllegalStateException(
                    "No fue posible validar el SHA-256.",
                    exception
            );
        }
    }

    /*
     * Representa la respuesta de Meta al consultar
     * un recurso multimedia.
     */
    private record MediaMetadata(

            String url,

            @JsonProperty("mime_type")
            String mimeType,

            @JsonProperty("sha256")
            String sha256,

            @JsonProperty("file_size")
            Long fileSize
    ) {
    }
}
