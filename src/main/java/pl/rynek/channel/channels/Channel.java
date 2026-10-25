package pl.rynek.channel.channels;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "channels")
public class Channel {

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    private String handle;

    private String name;

    private String description;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    private String address;

    @Column(name = "opening_hours")
    private String openingHours;

    @Column(name = "company_name")
    private String companyName;

    private String nip;

    @Column(name = "registered_address")
    private String registeredAddress;

    @Column(name = "verification_status")
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Channel() {
    }

    public Channel(UUID ownerId, String handle, String name, String description, Instant now) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.handle = handle;
        this.name = name;
        this.description = description;
        this.verificationStatus = VerificationStatus.NONE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    public void rename(String name, Instant now) {
        this.name = name;
        this.updatedAt = now;
    }

    public void describe(String description, Instant now) {
        this.description = description;
        this.updatedAt = now;
    }

    /** {@code null} usuwa dane kontaktowe. */
    public void changeContactPhone(String phone, Instant now) {
        this.contactPhone = phone;
        this.updatedAt = now;
    }

    /** {@code null} usuwa dane kontaktowe. */
    public void changeContactEmail(String email, Instant now) {
        this.contactEmail = email;
        this.updatedAt = now;
    }

    public void changeBusinessDetails(String companyName, String nip, String registeredAddress, Instant now) {
        this.companyName = companyName;
        this.nip = nip;
        this.registeredAddress = registeredAddress;
        this.updatedAt = now;
    }

    public void changeAvatar(String url, Instant now) {
        this.avatarUrl = url;
        this.updatedAt = now;
    }

    public void changeBanner(String url, Instant now) {
        this.bannerUrl = url;
        this.updatedAt = now;
    }

    /** Zgłoszenie dokumentów; ponowne zgłoszenie po odrzuceniu lub w trakcie weryfikacji wraca do {@code PENDING}. */
    public void submitForVerification(Instant now) {
        this.verificationStatus = VerificationStatus.PENDING;
        this.updatedAt = now;
    }

    public boolean hasBusinessDetails() {
        return companyName != null;
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getHandle() {
        return handle;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getAddress() {
        return address;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getNip() {
        return nip;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
