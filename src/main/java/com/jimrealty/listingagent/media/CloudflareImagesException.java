package com.jimrealty.listingagent.media;

/**
 * Wraps any failure from a Cloudflare Images API call: network, HTTP error,
 * malformed response, or success=false in the response envelope.
 *
 * RuntimeException — callers may catch but are not required to.
 * MediaIngestionService catches per-file and logs; one bad upload does not
 * abort the whole listing's ingestion.
 */
public class CloudflareImagesException extends RuntimeException {
    public CloudflareImagesException(String message) { super(message); }
    public CloudflareImagesException(String message, Throwable cause) { super(message, cause); }
}
