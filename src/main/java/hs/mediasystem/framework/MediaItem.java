package hs.mediasystem.framework;

import hs.mediasystem.db.LocalInfo;
import hs.mediasystem.util.ImageHandle;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MediaItem {
  public enum State {STANDARD, QUEUED, ENRICHED}

  private final MediaTree mediaTree;
  private final MediaItem parent;
  private final LocalInfo<?> localInfo;

  private State state = State.STANDARD;
  private String language;
  private String tagline;
  private String imdbId;

  private MediaItem(MediaTree mediaTree, MediaItem parent, final LocalInfo<?> localInfo) {
    this.mediaTree = mediaTree;
    this.parent = parent;
    this.localInfo = localInfo;

    groupName.set(localInfo.getGroupName() == null ? "" : localInfo.getGroupName());
    title.bind(new StringBinding() {
      {
        bind(officialTitle);
      }

      @Override
      protected String computeValue() {
        if(localInfo.getType().equals("EPISODE")) {
          if(getOfficialTitle() != null && !getOfficialTitle().isEmpty()) {
            return getOfficialTitle();
          }
          else if(localInfo.getTitle() != null && !localInfo.getTitle().isEmpty()) {
            return localInfo.getTitle();
          }

          return localInfo.getGroupName() + " " + localInfo.getSeason() + "x" + localInfo.getEpisode();
        }

        return localInfo.getTitle();
      }
    });
    subtitle.set(localInfo.getSubtitle() == null ? "" : localInfo.getSubtitle());
    releaseYear.set(localInfo.getReleaseYear());
    season.set(localInfo.getSeason());
    episode.set(localInfo.getEpisode());
  }

  public MediaItem(MediaTree mediaTree, final LocalInfo<?> localInfo) {
    this(mediaTree, null, localInfo);
  }

  public MediaItem(MediaItem parent, final LocalInfo<?> localInfo) {
    this(parent.getMediaTree(), parent, localInfo);
  }

  public MediaItem getParent() {
    return parent;
  }

  public LocalInfo<?> getLocalInfo() {
    return localInfo;
  }

  public String getMediaType() {
    return localInfo.getType();
  }

  public String getUri() {
    return localInfo.getUri();
  }

  public String getImdbId() {
    return imdbId;
  }

  public void setImdbId(String imdbId) {
    this.imdbId = imdbId;
  }

  public String getShortTitle() {
    return getTitle();
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getTagline() {
    return tagline;
  }

  public void setTagline(String tagline) {
    this.tagline = tagline;
  }

  public State getState() {
    return state;
  }

  public synchronized void setEnriched() {
    this.state = State.ENRICHED;
  }

  @Override
  public String toString() {
    return "('" + localInfo.getTitle() + "', MediaItem[subtitle=" + localInfo.getSubtitle() + ", type=" + localInfo.getType() + "])";
  }

  public boolean isLeaf() {
    return true;
  }

  public List<? extends MediaItem> children() {
    return Collections.emptyList();
  }

  public MediaTree getMediaTree() {
    return mediaTree;
  }

  private synchronized void queueForEnrichment() {
    if(state != State.ENRICHED) {
      mediaTree.queue(this);
      state = State.QUEUED;
    }
  }

  private final StringProperty groupName = new SimpleStringProperty();
  public String getGroupName() { return groupName.get(); }
  public StringProperty groupNameProperty() { return groupName; }

  private final StringProperty officialTitle = new SimpleStringProperty();
  public String getOfficialTitle() { return officialTitle.get(); }
  public StringProperty officialTitleProperty() { return officialTitle; }

  private final StringProperty title = new SimpleStringProperty();
  public String getTitle() { return title.get(); }
  public StringProperty titleProperty() { return title; }

  private final StringProperty subtitle = new SimpleStringProperty();
  public String getSubtitle() { return subtitle.get(); }
  public StringProperty subtitleProperty() { return subtitle; }

  private final ObjectProperty<Integer> season = new SimpleObjectProperty<>();
  public Integer getSeason() { return season.get(); }
  public ObjectProperty<Integer> seasonProperty() { return season; }

  private final ObjectProperty<Integer> episode = new SimpleObjectProperty<>();
  public Integer getEpisode() { return episode.get(); }
  public ObjectProperty<Integer> episodeProperty() { return episode; }

  private final ObjectProperty<Integer> releaseYear = new SimpleObjectProperty<>();
  public Integer getReleaseYear() { return releaseYear.get(); }
  public ObjectProperty<Integer> releaseYearProperty() { return releaseYear; }

  private final IntegerProperty runtime = new SimpleIntegerProperty() {
    @Override
    public int get() {
      queueForEnrichment();
      return super.get();
    }
  };
  public int getRuntime() { return runtime.get(); }
  public IntegerProperty runtimeProperty() { return runtime; }

  private final ObjectProperty<String[]> genres = new SimpleObjectProperty<String[]>(new String[0]) {
    @Override
    public String[] get() {
      queueForEnrichment();
      return super.get();
    }
  };
  public String[] getGenres() { return genres.get(); }
  public ObjectProperty<String[]> genresProperty() { return genres; }

  private final StringProperty plot = new SimpleStringProperty() {
    @Override
    public String get() {
      queueForEnrichment();
      return super.get();
    }
  };
  public String getPlot() { return plot.get(); }
  public StringProperty plotProperty() { return plot; }

  private final ObjectProperty<Date> releaseDate = new SimpleObjectProperty<Date>() {
    @Override
    public Date get() {
      queueForEnrichment();
      return super.get();
    }
  };
  public Date getReleaseDate() { return releaseDate.get(); }
  public ObjectProperty<Date> releaseDateProperty() { return releaseDate; }

  private final DoubleProperty rating = new SimpleDoubleProperty() {
    @Override
    public double get() {
      queueForEnrichment();
      return super.get();
    }
  };
  public Double getRating() { return rating.get(); }
  public DoubleProperty ratingProperty() { return rating; }

  private final ObjectProperty<ImageHandle> background = new SimpleObjectProperty<ImageHandle>() {
    @Override
    public ImageHandle get() {
      queueForEnrichment();
      return super.get();
    }
  };
  public ImageHandle getBackground() { return background.get(); }
  public ObjectProperty<ImageHandle> backgroundProperty() { return background; }

  private final ObjectProperty<ImageHandle> banner = new SimpleObjectProperty<ImageHandle>() {
    @Override
    public ImageHandle get() {
      queueForEnrichment();
      return super.get();
    }
  };
  public ImageHandle getBanner() { return banner.get(); }
  public ObjectProperty<ImageHandle> bannerProperty() { return banner; }

  private final ObjectProperty<ImageHandle> poster = new SimpleObjectProperty<ImageHandle>() {
    @Override
    public ImageHandle get() {
      queueForEnrichment();
      return super.get();
    }
  };
  public ImageHandle getPoster() { return poster.get(); }
  public ObjectProperty<ImageHandle> posterProperty() { return poster; }
}
