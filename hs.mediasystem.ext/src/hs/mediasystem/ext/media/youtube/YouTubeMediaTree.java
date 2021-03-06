package hs.mediasystem.ext.media.youtube;

import hs.mediasystem.framework.Media;
import hs.mediasystem.framework.MediaItem;
import hs.mediasystem.framework.MediaRoot;
import hs.mediasystem.framework.MediaTree;
import hs.mediasystem.persist.PersistQueue;

import java.util.ArrayList;
import java.util.List;

public class YouTubeMediaTree implements MediaTree, MediaRoot {
  private static final List<Feed> FEEDS = new ArrayList<>();

  static {
    FEEDS.add(new Feed("Most Viewed", "https://gdata.youtube.com/feeds/api/standardfeeds/most_viewed"));
    FEEDS.add(new Feed("Top Rated", "https://gdata.youtube.com/feeds/api/standardfeeds/top_rated"));
    FEEDS.add(new Feed("Recently Featured", "https://gdata.youtube.com/feeds/api/standardfeeds/recently_featured"));
    FEEDS.add(new Feed("Most Discussed", "https://gdata.youtube.com/feeds/api/standardfeeds/most_discussed"));
    FEEDS.add(new Feed("Top Favourites", "https://gdata.youtube.com/feeds/api/standardfeeds/top_favorites"));
    FEEDS.add(new Feed("Most Responded", "https://gdata.youtube.com/feeds/api/standardfeeds/most_responded"));
    FEEDS.add(new Feed("Most Recent", "https://gdata.youtube.com/feeds/api/standardfeeds/most_recent"));
    FEEDS.add(new Feed("Most Recent Comedy", "https://gdata.youtube.com/feeds/api/standardfeeds/most_recent_Comedy"));
  }

  private List<MediaItem> children;

  public static class Feed {
    private final String name;
    private final String url;

    public Feed(String name, String url) {
      this.name = name;
      this.url = url;
    }

    public String getName() {
      return name;
    }

    public String getUrl() {
      return url;
    }
  }

  @Override
  public List<? extends MediaItem> getItems() {
    if(children == null) {
      children = new ArrayList<>();

      for(Feed feed : FEEDS) {
        Media<?> media = new Media<>(feed.getName());
        children.add(new YouTubeFeed(YouTubeMediaTree.this, feed.getUrl(), feed, media));
      }
    }

    return children;
  }

  @Override
  public String getRootName() {
    return "YouTube";
  }

  @Override
  public PersistQueue getPersister() {
    return null;
  }

  @Override
  public String getId() {
    return "youtubeRoot";
  }

  @Override
  public MediaRoot getParent() {
    return null;
  }
}
