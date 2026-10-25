package com.neumatica.embudo.whatsap.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class AudioConversionService {

    public Path convertWebmToOggOpus(Path input) throws IOException {
        Path output = Files.createTempFile("whatsapp-audio-", ".ogg");

        try {
            Process process = new ProcessBuilder(
                    "ffmpeg",
                    "-nostdin",
                    "-y",
                    "-i", input.toAbsolutePath().toString(),
                    "-map", "0:a:0",
                    "-vn",
                    "-c:a", "libopus",
                    "-ac", "1",
                    "-b:a", "32k",
                    "-f", "ogg",
                    output.toAbsolutePath().toString()
            )
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            boolean finished;

            try {
                finished = process.waitFor(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
                throw new IOException(
                        "La conversión de audio fue interrumpida.", e);
            }

            if (!finished) {
                process.destroyForcibly();
                throw new IOException(
                        "La conversión de audio superó los 60 segundos.");
            }

            if (process.exitValue() != 0 || Files.size(output) == 0) {
                throw new IOException(
                        "FFmpeg no pudo convertir el archivo a OGG/Opus.");
            }

            return output;

        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(output);
            throw e;
        }
    }
}

