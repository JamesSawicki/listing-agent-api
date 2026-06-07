package com.jimrealty.listingagent.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin client over the Cloudflare Images v1 REST API.
 *
 * Responsibility: take bytes, upload, return Cloudflare Image ID. Nothing else.
 * Does NOT know about MLS Grid, Listings, or display variants.
 *
 * ── API ───────────────────────────────────────────────────────────────────
 * POST https://api.cloudflare.com/client/v4/accounts/{account_id}/images/v1
 *   Headers: Authorization: Bearer {api_token}
 *            Content-Type: multipart/form-data
 *   Body:    file=<image bytes>
 *
 * ── Response shape ────────────────────────────────────────────────────────
 *   { "success": true,
 *     "result":  { "id": "abc...", "filename": "...", "variants": [...] },
 *     "errors":  [],
 *     "messages": [] }
 *
 * We capture result.id. Variant URLs are constructed on the frontend via:
 *   https://imagedelivery.net/{account_hash}/{id}/{variant}
 *
 * ── Auth ──────────────────────────────────────────────────────────────────
 * Requires two env vars set at startup:
 *   CF_ACCOUNT_ID         — long hex from Cloudflare dashboard
 *   CF_IMAGES_API_TOKEN   — token created with "Cloudflare Images: Edit"
 * If either is missing/blank, isConfigured() returns false and uploadBytes
 * throws. MediaIngestionService checks isConfigured() before calling.
 */
@Component
public class CloudflareImagesClient {

    private static final Logger log = LoggerFactory.getLogger(CloudflareImagesClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String uploadEndpoint;
    private final boolean configured;

    public CloudflareImagesClient(
            ObjectMapper objectMapper,
            @Value("${cloudflare.images.account-id:}") String accountId,
            @Value("${cloudflare.images.api-token:}") String apiToken) {

        this.objectMapper = objectMapper;
        this.configured = !accountId.isBlank() && !apiToken.isBlank();

        if (configured) {
            this.uploadEndpoint = String.format(
                    "https://api.cloudflare.com/client/v4/accounts/%s/images/v1", accountId);
            this.restClient = RestClient.builder()
                    .defaultHeader("Authorization", "Bearer " + apiToken)
                    .build();
            log.info("CloudflareImagesClient initialized for account [{}]", accountId);
        } else {
            this.uploadEndpoint = null;
            this.restClient = null;
            log.warn("CloudflareImagesClient NOT configured (missing CF_ACCOUNT_ID or CF_IMAGES_API_TOKEN). " +
                     "Media ingestion calls will be no-ops.");
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    /**
     * Uploads image bytes and returns the Cloudflare Image ID.
     *
     * @param bytes    the image file bytes
     * @param filename a hint filename (e.g. "listing_NST12345_photo_1.jpg") —
     *                 stored by Cloudflare in image metadata; useful for debugging
     * @return the CF Image ID on success
     * @throws CloudflareImagesException on any failure (network, HTTP, parse, success=false)
     */
    public String uploadBytes(byte[] bytes, String filename) {
        if (!configured) {
            throw new CloudflareImagesException("CloudflareImagesClient not configured");
        }
        if (bytes == null || bytes.length == 0) {
            throw new CloudflareImagesException("Cannot upload empty byte array");
        }

        // ByteArrayResource needs a filename override or Spring's multipart encoder
        // will use the default "ByteArrayResource" string which Cloudflare may reject.
        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override public String getFilename() { return filename; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        try {
            String responseJson = restClient.post()
                    .uri(uploadEndpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null) {
                throw new CloudflareImagesException("Empty response from Cloudflare Images");
            }

            JsonNode root = objectMapper.readTree(responseJson);
            if (!root.path("success").asBoolean(false)) {
                throw new CloudflareImagesException(
                        "Cloudflare returned success=false: " + root.path("errors").toString());
            }

            String id = root.path("result").path("id").asText(null);
            if (id == null || id.isBlank()) {
                throw new CloudflareImagesException("Response missing result.id: " + responseJson);
            }
            return id;

        } catch (RestClientException e) {
            throw new CloudflareImagesException(
                    "HTTP error uploading to Cloudflare Images: " + e.getMessage(), e);
        } catch (CloudflareImagesException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CloudflareImagesException(
                    "Failed to upload to Cloudflare Images: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an image by CF Image ID. Best-effort: logs and swallows errors.
     * Called when a listing's MlgCanView flips false (record-level deletion)
     * or when a delta sync detects a Media record removed by MLS Grid.
     */
    public void deleteImage(String imageId) {
        if (!configured || imageId == null || imageId.isBlank()) return;
        try {
            restClient.delete()
                    .uri(uploadEndpoint + "/" + imageId)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Deleted Cloudflare Image [{}]", imageId);
        } catch (Exception e) {
            log.warn("Failed to delete Cloudflare Image [{}]: {}", imageId, e.getMessage());
        }
    }
}
