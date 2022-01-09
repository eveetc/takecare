package com.mad.takecare.model;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.List;

public class User {

    String usrname;
    String description;
    String favouriteCategory;
    int expEarned;
    String mail;
    int level;
    int userID;
    int taskCount = -1;

    public boolean canAcceptTask(){
        return getMaximumAcceptedTasks() > getTaskCount();
    }

    /**
     * Adds XP to the User
     * @param amount The XP the user gets
     * @return True if the User leveled up, false if not.
     */
    public boolean addEXP(int amount){
        boolean result = false;
        expEarned += amount;
        while(expEarned > getExpToNextLevel()){
            result = true;
            level++;
        }
        return result;
    }

    public int getMaximumAcceptedTasks(){
        if(level > 7){
            return 5;
        } else if(level > 6){
            return 4;
        } else if(level > 4){
            return 3;
        } else if(level > 2){
            return 2;
        } else {
            return 1;
        }
    }

    public int getUserID() {
        return userID;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }


    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getExpToNextLevel(){
        return getExpToNextLevel(level);
    }

    public static  int getExpToNextLevel(int currentLevel){
        return (int)Math.round(1*(Math.pow(currentLevel+1, 1.75)));
    }

    public int getExpEarned() {
        return expEarned;
    }

    /**
     * Only for debugging purposes. Use addEXP instead.
     * @param expEarned
     */
    public void setExpEarned(int expEarned) {
        this.expEarned = expEarned;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFavouriteCategory() {
        return favouriteCategory;
    }

    public void setFavouriteCategory(String favouriteCategory) {
        this.favouriteCategory = favouriteCategory;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    Bitmap image;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    String objectId; //id saved in DB
    List<Task> acceptedTasks;
    List<Task> ownedTasks;

    public String getUsrname() {
        return usrname;
    }

    public void setUsrname(String usrname) {
        this.usrname = usrname;
    }

    public List<Task> getAcceptedTasks() {
        return acceptedTasks;
    }

    public void setAcceptedTasks(List<Task> acceptedTasks) {
        this.acceptedTasks = acceptedTasks;
    }

    public List<Task> getOwnedTasks() {
        return ownedTasks;
    }

    public void setOwnedTasks(List<Task> ownedTasks) {
        this.ownedTasks = ownedTasks;
    }

    /**
     * Two Users are considered equal if they have the same ID.
     * @param other The other user
     * @return True if this.userID is the same as other.userID
     */
    public boolean equals(User other){
        if(other == null){
            return false;
        }
        return other.userID == this.userID;
    }
}
