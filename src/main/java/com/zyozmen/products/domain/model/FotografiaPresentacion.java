package com.zyozmen.products.domain.model;

import java.util.Base64;
import java.util.Locale;
import java.util.Set;

public final class FotografiaPresentacion {
    public static final int MAX_BYTES = 200 * 1024;
    private static final int MAX_BASE64_LENGTH = 4 * ((MAX_BYTES + 2) / 3);
    private static final Set<String> EXTENSIONES = Set.of("png", "jpg", "jpeg", "webp");

    private FotografiaPresentacion() {
    }

    public static Imagen validar(Imagen imagen) {
        if (imagen == null) {
            return null;
        }
        if (imagen.getNombre() == null || imagen.getNombre().isBlank() || imagen.getNombre().length() > 255
                || imagen.getExtension() == null) {
            throw new IllegalArgumentException("La fotografia requiere nombre y extension");
        }
        String extension = imagen.getExtension().toLowerCase(Locale.ROOT);
        String nombre = imagen.getNombre();
        int punto = nombre.lastIndexOf('.');
        if (!EXTENSIONES.contains(extension) || punto < 0
                || !nombre.substring(punto + 1).equalsIgnoreCase(extension)) {
            throw new IllegalArgumentException("La fotografia debe ser PNG, JPG o WebP con extension correspondiente al nombre");
        }
        String contenido = imagen.getFilepart();
        if (contenido == null || contenido.isEmpty()) {
            throw new IllegalArgumentException("La fotografia debe contener Base64 puro en filepart");
        }
        if (contenido.length() > MAX_BASE64_LENGTH) {
            throw new IllegalArgumentException("La fotografia no puede superar 200 KiB (204800 bytes)");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(contenido);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El filepart de la fotografia debe ser Base64 puro valido, sin prefijo data:", ex);
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("La fotografia debe contener datos y no superar 200 KiB (204800 bytes)");
        }
        return new Imagen(nombre, extension, Base64.getEncoder().encodeToString(bytes));
    }
}
