package com.mad.takecare.model;

import android.location.Location;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

public class Task implements Serializable {

    String taskName;
    private Location location;
    String additionalInfo;
    String locationName;
    String description;
    boolean completed, completedMarker;
    String ID; //id saved in DB
    String accepterID = "none";
    boolean weekly;
    String ownerID;
    String category = "";

    public int getSetPointValue() {
        return setPointValue;
    }

    public void setSetPointValue(int setPointValue) {
        this.setPointValue = setPointValue;
    }

    int setPointValue = 0;

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getLocationName() {
        return locationName;
    }

    public boolean hasValidAccepter(){
        return getAccepterID() != null && !getAccepterID().equals("none") && !getAccepterID().equals("");
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Location getLocation() {
        return location;
    }
    public void setLocation(Location location) {
        this.location = location;
    }

    public String getLocationLong() {
        return locationLong;
    }

    public void setLocationLong(String locationLong) {
        this.locationLong = locationLong;
    }

    public String getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(String locationLat) {
        this.locationLat = locationLat;
    }

    //Location object cant be serialized. Therefore Task object cant be send via intent extra.
    //Use following both and set location to null if send by intent is used
    String locationLong;
    String locationLat;

    public String getAdditionalInfo() {
        return additionalInfo;
    }
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
    public String getDescription() {
        return description;
    }

    public String getOwnerID() {
        return ownerID;
    }
    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }
    public String getAccepterID() {
        return accepterID;
    }
    public void setAccepterID(String accepterID) {
        this.accepterID = accepterID;
    }
    public String getTaskName() {
        return taskName;
    }
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    public boolean isCompleted() {
        return completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    public boolean isCompletedMarker() {
        return completedMarker;
    }
    public void setCompletedMarker(boolean completedMarker) {
        this.completedMarker = completedMarker;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public boolean isWeekly(){
        return weekly;
    }
    public void setWeekly(boolean weekly) {
        this.weekly = weekly;
    }
    public String getID() {
        return ID;
    }
    public void setID(String ID) {
        this.ID = ID;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getPointValue(){
        if(setPointValue == 0) {
            if(getCreatedAt() != null) {
                Duration diff = Duration.between(new Date().toInstant(), getCreatedAt().toInstant()).abs(); //new Date represents now
                if (diff.toDays() == 0) {
                    return 1;
                } else if (diff.toDays() < 3) {
                    return 2;
                } else {
                    return 3;
                }

        }}
        return setPointValue;


    }

    //Time of creation
    Date createdAt;

    public Date getDueTime() {
        return dueTime;
    }

    public void setDueTime(Date dueTime) {
        this.dueTime = dueTime;
    }

    Date dueTime;

    /**
     * Two Tasks are considered equal if they have the same ID.
     * @param other The Task compared to
     * @return True if other has the same ID as this, else false.
     */
    public boolean equals(Task other) {
        if(other == null){
            return false;
        }
        return other.ID == this.ID;
    }
}
