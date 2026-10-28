package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.model.Image;
import com.zyozmen.products.domain.model.Images;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Service
public class ImageUploadService {
    public enum Target {CATEGORY, PRODUCT}

    public static final long MAX_SIZE = 2L * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg", "webp", "image/webp");

    public Images prepare(Target target, MultipartFile main) {
        if (target == null) {
            throw new IllegalArgumentException("El destino de las imagenes es obligatorio");
        }
        if (target == Target.CATEGORY || target == Target.PRODUCT) {
            Image image = convert(main);
            if (image == null) {
                throw new IllegalArgumentException("La foto principal es obligatoria para "
                        + (target == Target.PRODUCT ? "productos" : "categorias"));
            }
            return Images.builder().fotoPrincipal(image).build();
        }
        return Images.builder().fotoPrincipal(convert(main)).build();
    }

    private Image convert(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("La imagen no puede superar 2 MB");
        }
        String name = file.getOriginalFilename();
        String extension = name == null || !name.contains(".") ? ""
                : name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String contentType = CONTENT_TYPES.get(extension);
        if (contentType == null || !contentType.equals(file.getContentType())) {
            throw new IllegalArgumentException("La imagen debe ser PNG, JPG o WebP con tipo de contenido correspondiente");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_SIZE || bytes.length == 0) {
                throw new IllegalArgumentException("La imagen debe contener datos y no superar 2 MB");
            }
            return new Image(name, extension, Base64.getEncoder().encodeToString(bytes));
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo leer el archivo de imagen.", exception);
        }
    }
}
