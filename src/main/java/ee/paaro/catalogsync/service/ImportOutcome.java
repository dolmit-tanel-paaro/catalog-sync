package ee.paaro.catalogsync.service;

public enum ImportOutcome {

    /** Applied against a category that already existed. */
    APPLIED,

    /** Applied against a placeholder created on the spot for a category we have not seen. */
    APPLIED_WITH_PLACEHOLDER,

    /** A placeholder category was filled in, and anything parked against it released. */
    PLACEHOLDER_PROMOTED,

    /** Seen before. Nothing written. */
    DUPLICATE
}
