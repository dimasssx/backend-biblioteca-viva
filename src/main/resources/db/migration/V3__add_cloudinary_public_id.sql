-- Persists the Cloudinary public_id alongside the asset URL so the application
-- can delete or replace remote assets without relying on URL parsing.
-- The column is nullable: existing rows have no public_id and are left unchanged.

ALTER TABLE public.art
    ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR(255) NULL;

ALTER TABLE public.infographic
    ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR(255) NULL;

ALTER TABLE public.other_work
    ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR(255) NULL;

ALTER TABLE public.news
    ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR(255) NULL;
