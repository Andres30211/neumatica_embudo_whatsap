package com.neumatica.embudo.whatsap.repository;

/*
 * Servicio encargado de obtener y almacenar archivos
 * multimedia provenientes de WhatsApp.
 */
public interface MediaStorageService {

    /**
     * Descarga un archivo multimedia desde Meta y lo
     * almacena físicamente en el servidor.
     *
     * @param mediaId identificador del recurso multimedia
     *                proporcionado por WhatsApp.
     * @param mimeType tipo MIME del archivo.
     * @param sha256 hash SHA-256 proporcionado por WhatsApp.
     * @return ruta lógica donde quedó almacenado el archivo.
     */
    String downloadAndStore(
            String mediaId,
            String mimeType,
            String sha256
    );
}