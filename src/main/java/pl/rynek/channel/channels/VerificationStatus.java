package pl.rynek.channel.channels;

/** Weryfikacja sprzedawcy. {@code VERIFIED} daje znak weryfikacji przy nazwie kanału i w kartach ofert. */
public enum VerificationStatus {
    NONE,
    PENDING,
    VERIFIED,
    REJECTED
}
