package fr.android.carnetvoyage.model;


public class Entry {
    private long id;
    private String title;
    private String note;
    private String photoPath;
    private double latitude;
    private double longitude;
    private String address;   // rue obtenue par géocodage inverse
    private long timestamp;    // date de création (en millisecondes)
    private long remoteId;    // ID sur le serveur MySQL (-1 si pas synchro)

    public Entry() {
        this.id = -1;
        this.remoteId = -1;
    }

    public Entry(long id, String title, String note, String photoPath,
                 double latitude, double longitude, String address, long timestamp, long remoteId) {
        this.id = id;
        this.title = title;
        this.note = note;
        this.photoPath = photoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.timestamp = timestamp;
        this.remoteId = remoteId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getRemoteId() { return remoteId; }
    public void setRemoteId(long remoteId) { this.remoteId = remoteId; }
}
