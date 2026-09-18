package org.bibliotecaviva.backend.application.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bibliotecaviva.backend.application.dtos.CloudinaryUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png"
    );

    /**
     * Faz upload de uma imagem para o Cloudinary.
     *
     * @param file arquivo de imagem (JPG ou PNG)
     * @return {@link CloudinaryUploadResult} contendo a URL pública e o {@code public_id}
     *         necessário para remoção posterior
     * @throws IllegalArgumentException se o arquivo for vazio ou tiver formato inválido
     * @throws RuntimeException         se ocorrer erro no upload
     */
    public CloudinaryUploadResult uploadImage(MultipartFile file) {
        validateFile(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "image"
            ));

            String secureUrl = (String) result.get("secure_url");
            if (secureUrl == null) {
                secureUrl = (String) result.get("url");
            }
            String publicId = (String) result.get("public_id");

            return new CloudinaryUploadResult(secureUrl, publicId);
        } catch (IOException e) {
            log.error("Erro ao realizar upload para o Cloudinary", e);
            throw new RuntimeException("Falha ao realizar upload da imagem", e);
        }
    }

    /**
     * Remove um asset do Cloudinary pelo seu {@code public_id}.
     *
     * <p>Falhas são registradas em log mas não relançadas — a remoção remota é
     * melhor esforço: o recurso no banco já foi desvinculado ou o registro foi
     * excluído antes desta chamada.
     *
     * @param publicId identificador do asset no Cloudinary (retornado em {@link CloudinaryUploadResult})
     */
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
            log.info("Asset Cloudinary removido: {}", publicId);
        } catch (IOException e) {
            log.error("Falha ao remover asset Cloudinary com public_id={}: {}", publicId, e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de imagem não pode ser vazio");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        boolean validMime = contentType != null
                && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
        boolean validExtension = filename != null
                && (filename.toLowerCase().endsWith(".jpg")
                || filename.toLowerCase().endsWith(".jpeg")
                || filename.toLowerCase().endsWith(".png"));

        if (!validMime && !validExtension) {
            throw new IllegalArgumentException(
                    "Formato de imagem inválido. Apenas JPG/JPEG e PNG são permitidos");
        }
    }
}
