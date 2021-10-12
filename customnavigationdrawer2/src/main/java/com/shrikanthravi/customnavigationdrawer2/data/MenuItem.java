package com.shrikanthravi.customnavigationdrawer2.data;


public class MenuItem {
    String title;
    int imageId;
    int imageIdSmall;

    public MenuItem(String title, int imageId, int imageIdSmall) {
        this.title = title;
        this.imageId = imageId;
        this.imageIdSmall = imageIdSmall;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public int getImageIdSmall() {
        return imageIdSmall;
    }

    public void setImageIdSmall(int imageIdSmall) {
        this.imageIdSmall = imageIdSmall;
    }
}


