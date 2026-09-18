package org.bibliotecaviva.backend.application.dtos;

/**
 * Holds the result of a Cloudinary upload operation.
 *
 * @param url      public HTTPS URL of the uploaded asset
 * @param publicId Cloudinary public_id required to delete or replace the asset later
 */
public record CloudinaryUploadResult(String url, String publicId) {
}
