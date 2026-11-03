package com.example.DrogueriaLosTresHermanos.catalogo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Fotos de productos. El tipo se comprueba por el contenido del archivo y el nombre lo genera el servidor. */
@Service
public class FotosProductos {

    public static final long TAMANO_MAXIMO = 3 * 1024 * 1024;
    private static final Pattern NOMBRE = Pattern.compile("^[a-f0-9-]{36}\\.(jpg|png|webp)$");
    private static final Map<String, String> TIPOS = Map.of("jpg", "image/jpeg", "png", "image/png", "webp", "image/webp");

    private final Path carpeta;

    public FotosProductos(@Value("${app.fotos}") String carpeta) {
        this.carpeta = Path.of(carpeta);
    }

    /** Guarda la foto y devuelve su nombre. Lanza IllegalArgumentException con un mensaje para el usuario. */
    public String guardar(MultipartFile archivo) throws IOException {
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("La foto no puede superar 3 MB.");
        }
        byte[] cabecera;
        try (InputStream in = archivo.getInputStream()) {
            cabecera = in.readNBytes(12);
        }
        String extension = extensionPorContenido(cabecera)
            .orElseThrow(() -> new IllegalArgumentException("La foto debe ser JPG, PNG o WEBP."));
        Files.createDirectories(carpeta);
        String nombre = UUID.randomUUID() + "." + extension;
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, carpeta.resolve(nombre));
        }
        return nombre;
    }

    public Optional<Path> ruta(String nombre) {
        if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path p = carpeta.resolve(nombre);
        return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
    }

    public String tipoDe(String nombre) {
        return TIPOS.getOrDefault(nombre.substring(nombre.lastIndexOf('.') + 1), "application/octet-stream");
    }

    private static Optional<String> extensionPorContenido(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("jpg");
        }
        if (b.length >= 8 && Arrays.equals(Arrays.copyOf(b, 8), new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return Optional.of("png");
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("webp");
        }
        return Optional.empty();
    }
}
