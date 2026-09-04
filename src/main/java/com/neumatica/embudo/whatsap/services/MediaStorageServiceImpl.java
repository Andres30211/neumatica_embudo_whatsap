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
import java.util.Base64;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.neumatica.embudo.whatsap.repository.MediaStorageService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MediaStorageServiceImpl
        implements MediaStorageService {

    /*
     * Cliente HTTP utilizado para comunicarnos con la API Graph de Meta.
     */
    private final RestClient restClient = RestClient.create();


    /*
     * Versión de la API Graph utilizada actualmente.
     */
    private static final String GRAPH_API_VERSION = "v25.0";

    @Override
    public String downloadAndStore(
            String mediaId,
            String mimeType,
            String sha256) {

        /*
         * Validamos que Meta nos haya enviado un mediaId.
         *
         * Sin este identificador no podemos consultar la información
         * del archivo multimedia en la API de Meta.
         */
        if (mediaId == null || mediaId.isBlank()) {

            throw new IllegalArgumentException(
                    "El mediaId no puede ser null o vacío."
            );
        }

        /*
         * Consultamos a Meta para obtener la URL temporal
         * desde la cual podemos descargar el archivo.
         */
        MediaMetadata metadata =
                getMediaMetadata(mediaId);

        /*
         * Validamos que Meta haya devuelto correctamente
         * la URL temporal del archivo.
         */
        if (metadata == null
                || metadata.url() == null
                || metadata.url().isBlank()) {

            throw new IllegalStateException(
                    "Meta no proporcionó una URL para el mediaId: "
                            + mediaId
            );
        }

        /*
         * Determinamos la extensión del archivo utilizando
         * el MIME type recibido desde WhatsApp.
         *
         * Ejemplo:
         *
         * image/jpeg -> .jpg
         * image/png  -> .png
         * video/mp4  -> .mp4
         */
        String extension =
                resolveExtension(mimeType);

        /*
         * Generamos un nombre único para evitar colisiones
         * entre archivos.
         *
         * Ejemplo:
         *
         * 550e8400-e29b-41d4-a716-446655440000.jpg
         */
        String generatedFileName =
                UUID.randomUUID() + extension;

        /*
         * Creamos el directorio físico donde almacenaremos
         * el archivo.
         *
         * Ejemplo:
         *
         * ./uploads/whatsapp/2026/09/
         */
        Path directory =
                createStorageDirectory();

        /*
         * Construimos la ruta física completa del archivo.
         */
        Path targetFile =
                directory.resolve(generatedFileName);

        /*
         * Descargamos el archivo desde la URL temporal
         * proporcionada por Meta.
         */
        downloadFile(
                metadata.url(),
                targetFile
        );

        /*
         * Determinamos cuál SHA-256 debemos utilizar
         * para validar el archivo.
         *
         * Primero intentamos utilizar el SHA-256 recibido
         * directamente en el webhook.
         *
         * Si no existe, utilizamos el SHA-256 obtenido
         * desde la consulta de metadata a Meta.
         */
        String expectedSha256 = sha256;

        if (expectedSha256 == null
                || expectedSha256.isBlank()) {

            expectedSha256 =
                    metadata.sha256();
        }

        /*
         * Si tenemos SHA-256 disponible, validamos que el
         * archivo descargado sea exactamente el archivo
         * que Meta nos indicó.
         */
        if (expectedSha256 != null
                && !expectedSha256.isBlank()) {

            validateSha256(
                    targetFile,
                    expectedSha256
            );
        }

        /*
         * Construimos la ruta lógica que será almacenada
         * posteriormente en la base de datos.
         *
         * Ejemplo:
         *
         * whatsapp/2026/09/550e8400-e29b-41d4-a716-446655440000.jpg
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

        /*
         * Retornamos únicamente la ruta lógica.
         *
         * El archivo físico permanece en:
         *
         * ./uploads/whatsapp/...
         *
         * Y en la base de datos se guarda:
         *
         * whatsapp/2026/09/archivo.jpg
         */
        return storagePath;
    }

    /*
     * Consulta a Meta la información asociada al mediaId.
     *
     * Meta devuelve información como:
     *
     * - URL temporal de descarga
     * - MIME type
     * - SHA-256
     * - Tamaño del archivo
     */
    private MediaMetadata getMediaMetadata(
            String mediaId) {

        /*
         * Construimos la URL de la API Graph.
         *
         * Ejemplo:
         *
         * https://graph.facebook.com/v25.0/2113718569351118
         */
        String url =
                String.format(
                        "https://graph.facebook.com/%s/%s",
                        GRAPH_API_VERSION,
                        mediaId
                );

        /*
         * Realizamos la petición GET a Meta.
         */
        return restClient.get()
                .uri(url)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + "EAAWNon6bi60BSBJQsDz4UFjXGJGEq39Uuxo9fcgNj4QkDpm3WfrPdiZCUZBdGOzNU3u8A1tplKSXfGTlS8KC6NmARGiljZCKnQjTGw8ffJi89KosBh77yAKxZAI0qhWrOkZB2QIpnqyovQe5gchBEI0dX5M7pduHdIrITgOrpUxgMZCwRBiXGaPh7nOA9SHQZDZD"
                )
                .retrieve()
                .body(MediaMetadata.class);
    }

    /*
     * Descarga físicamente el archivo multimedia desde
     * la URL temporal proporcionada por Meta.
     */
    private void downloadFile(
            String downloadUrl,
            Path targetFile) {

        /*
         * Realizamos la petición GET hacia la URL temporal.
         *
         * La URL temporal también requiere el Access Token.
         */
        restClient.get()
                .uri(downloadUrl)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + "EAAWNon6bi60BSBJQsDz4UFjXGJGEq39Uuxo9fcgNj4QkDpm3WfrPdiZCUZBdGOzNU3u8A1tplKSXfGTlS8KC6NmARGiljZCKnQjTGw8ffJi89KosBh77yAKxZAI0qhWrOkZB2QIpnqyovQe5gchBEI0dX5M7pduHdIrITgOrpUxgMZCwRBiXGaPh7nOA9SHQZDZD"
                )
                .exchange((request, response) -> {

                    HttpStatusCode status =
                            response.getStatusCode();

                    /*
                     * Verificamos que Meta haya respondido
                     * correctamente.
                     */
                    if (!status.is2xxSuccessful()) {

                        throw new IllegalStateException(
                                "Meta respondió con HTTP "
                                        + status.value()
                        );
                    }

                    /*
                     * Obtenemos el stream del archivo.
                     */
                    try (InputStream inputStream =
                                 response.getBody()) {

                        /*
                         * Validamos que realmente exista
                         * contenido en la respuesta.
                         */
                        if (inputStream == null) {

                            throw new IllegalStateException(
                                    "Meta devolvió un body vacío."
                            );
                        }

                        /*
                         * Copiamos el stream directamente al archivo.
                         *
                         * No cargamos todo el archivo en memoria.
                         *
                         * Esto es importante para archivos grandes
                         * como videos.
                         */
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
     * Crea el directorio donde se almacenarán los archivos.
     *
     * Estructura:
     *
     * ./uploads/whatsapp/
     *     └── 2026/
     *         └── 09/
     */
    private Path createStorageDirectory() {

        LocalDate now =
                LocalDate.now();

        /*
         * Construimos la estructura de directorios.
         */
        Path directory =
                Paths.get(
                        "./uploads/whatsapp",
                        String.valueOf(
                                now.getYear()
                        ),
                        String.format(
                                "%02d",
                                now.getMonthValue()
                        )
                );

        try {

            /*
             * Crea todos los directorios necesarios
             * si todavía no existen.
             */
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
     * Construye la ruta lógica que será almacenada en la BD.
     *
     * Ejemplo:
     *
     * whatsapp/2026/09/archivo.jpg
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
     * Determina la extensión del archivo a partir del MIME type.
     */
    private String resolveExtension(
            String mimeType) {

        /*
         * Si Meta no proporciona MIME type,
         * utilizamos una extensión genérica.
         */
        if (mimeType == null
                || mimeType.isBlank()) {

            return ".bin";
        }

        /*
         * Normalizamos el MIME type para evitar problemas
         * con mayúsculas/minúsculas.
         */
        return switch (
                mimeType.toLowerCase()
        ) {

            case "image/jpeg" ->
                    ".jpg";

            case "image/png" ->
                    ".png";

            case "image/webp" ->
                    ".webp";

            case "image/gif" ->
                    ".gif";

            case "video/mp4" ->
                    ".mp4";

            case "video/3gpp" ->
                    ".3gp";

            case "audio/ogg" ->
                    ".ogg";

            case "audio/mpeg" ->
                    ".mp3";

            case "audio/mp4" ->
                    ".m4a";

            case "audio/aac" ->
                    ".aac";

            case "application/pdf" ->
                    ".pdf";

            case "application/msword" ->
                    ".doc";

            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    ".docx";

            case "application/vnd.ms-excel" ->
                    ".xls";

            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    ".xlsx";

            case "application/vnd.ms-powerpoint" ->
                    ".ppt";

            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    ".pptx";

            /*
             * Si aparece un MIME type que todavía no
             * contemplamos, almacenamos el archivo como .bin.
             */
            default ->
                    ".bin";
        };
    }

    /*
     * Valida el SHA-256 del archivo descargado.
     *
     * IMPORTANTE:
     *
     * Meta entrega el SHA-256 en Base64.
     *
     * Por ejemplo:
     *
     * cQRyKQn65TMCvuVBbARxxKlXBYRrv0k2Dunzr6hnWCU=
     *
     * Por lo tanto NO debemos utilizar HexFormat.
     *
     * Debemos calcular el SHA-256 como bytes y posteriormente
     * convertir esos bytes a Base64.
     */
    private void validateSha256(
            Path file,
            String expectedSha256) {

        try {

            /*
             * Creamos el algoritmo SHA-256.
             */
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            /*
             * Abrimos el archivo como stream.
             *
             * Esto permite calcular el hash sin cargar
             * todo el archivo en memoria.
             */
            try (InputStream inputStream =
                         Files.newInputStream(file)) {

                /*
                 * Buffer utilizado para leer el archivo
                 * por bloques.
                 */
                byte[] buffer =
                        new byte[8192];

                int bytesRead;

                /*
                 * Leemos el archivo completo.
                 */
                while ((bytesRead =
                        inputStream.read(buffer)) != -1) {

                    /*
                     * Alimentamos el contenido al algoritmo SHA-256.
                     */
                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            /*
             * Obtenemos los bytes resultantes del SHA-256.
             */
            byte[] hashBytes =
                    digest.digest();

            /*
             * Convertimos los bytes del SHA-256 a Base64.
             *
             * Esto es lo importante para solucionar
             * el error que estabas recibiendo.
             */
            String calculatedSha256 =
                    Base64.getEncoder()
                            .encodeToString(
                                    hashBytes
                            );

            /*
             * Dejamos esta información en DEBUG.
             *
             * No usamos INFO porque el SHA-256 no es necesario
             * mostrarlo normalmente en producción.
             */
            log.debug(
                    "Validación SHA-256. esperado={}, calculado={}",
                    expectedSha256,
                    calculatedSha256
            );

            /*
             * Comparamos el hash calculado con el proporcionado
             * por Meta.
             */
            if (!calculatedSha256.equals(
                    expectedSha256)) {

                /*
                 * Si el archivo no coincide con el hash esperado,
                 * eliminamos el archivo descargado.
                 *
                 * Así evitamos conservar un archivo potencialmente
                 * corrupto o diferente al esperado.
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
     * Obtiene el Access Token desde la variable de entorno.
     *
     * De esta manera evitamos almacenar el token directamente
     * dentro del código fuente.
     */
    /*private String getAccessToken() {

        if (accessToken == null
                || accessToken.isBlank()) {

            throw new IllegalStateException(
                    "No está configurada la variable de entorno "
                            + "WHATSAPP_ACCESS_TOKEN."
            );
        }

        return accessToken;
    }*/

    /*
     * Representa la respuesta que devuelve Meta cuando
     * consultamos un mediaId.
     *
     * Ejemplo de información:
     *
     * {
     *     "url": "...",
     *     "mime_type": "image/jpeg",
     *     "sha256": "...",
     *     "file_size": 123456
     * }
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