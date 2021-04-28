package de.tectoast.emolga.utils;

import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;

public class YTDataLoader {
    private final String channel;
    private final String thumbnail;

    public YTDataLoader(Video video) {
        VideoSnippet sn = video.getSnippet();
        this.channel = sn.getChannelTitle();
        this.thumbnail = sn.getThumbnails().getMedium().getUrl();
    }

    public YTDataLoader(SearchResult result) {
        SearchResultSnippet sn = result.getSnippet();
        this.channel = sn.getChannelTitle();
        this.thumbnail = sn.getThumbnails().getMedium().getUrl();
    }

    public String getChannel() {
        return channel;
    }

    public String getThumbnail() {
        return thumbnail;
    }
}
