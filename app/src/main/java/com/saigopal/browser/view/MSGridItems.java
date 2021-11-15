package com.saigopal.browser.view;

public class MSGridItems {
    String Title;
    String Url;
    String ImageUrl;

    public MSGridItems(String title, String url, String imageUrl) {
        Title = title;
        Url = url;
        ImageUrl = imageUrl;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getUrl() {
        return Url;
    }

    public void setUrl(String url) {
        Url = url;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }
}
