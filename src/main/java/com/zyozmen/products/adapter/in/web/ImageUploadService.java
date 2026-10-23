package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.model.Imagen;
import com.zyozmen.products.domain.model.Images;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class ImageUploadService {
    public enum Target { PRODUCT, CATEGORY, CEPA }

    public static final long MAX_SIZE = 2L * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg", "webp", "image/webp");

    public Images prepare(Target target, MultipartFile main, List<MultipartFile> secondary) {
        if (target == null) {
            throw new IllegalArgumentException("El destino de las imagenes es obligatorio");
        }
        if (target == Target.CATEGORY || target == Target.CEPA) {
            Imagen image = convert(main);
            if (image == null) {
                throw new IllegalArgumentException("La foto principal es obligatoria para "
                        + (target == Target.CEPA ? "cepas" : "categorias"));
            }
            return Images.builder().fotoPrincipal(image).fotosSecundarias(List.of()).build();
        }
        List<MultipartFile> files = secondary == null ? List.of() : secondary.stream()
                .filter(Objects::nonNull).filter(file -> !file.isEmpty()).toList();
        if (files.size() > 5) {
            throw new IllegalArgumentException("No se permiten mas de 5 fotos secundarias.");
        }
        return Images.builder().fotoPrincipal(convert(main))
                .fotosSecundarias(files.stream().map(this::convert).toList()).build();
    }

    private Imagen convert(MultipartFile file) {
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
            return new Imagen(name, extension, Base64.getEncoder().encodeToString(bytes));
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo leer el archivo de imagen.", exception);
        }
    }
}
