package com.jimrealty.listingagent.amenity;

public class OverpassFetchException extends RuntimeException {
    public OverpassFetchException(String message) { super(message); }
    public OverpassFetchException(String message, Throwable cause) { super(message, cause); }
}