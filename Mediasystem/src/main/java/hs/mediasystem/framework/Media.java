package hs.mediasystem.framework;

import hs.mediasystem.dao.Item;
import hs.mediasystem.dao.Source;
import hs.mediasystem.fs.SourceImageHandle;
import hs.mediasystem.screens.Casting;
import hs.mediasystem.screens.Person;
import hs.mediasystem.util.ImageHandle;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Media<T extends Media<T>> extends Entity<T> {
  public final ObjectProperty<Item> item = object("item");

  public final StringProperty title = string();
  public final StringProperty subtitle = string("");
  public final StringProperty description = string();
  public final ObjectProperty<Date> releaseDate = object("releaseDate");
  public final ObjectProperty<Integer> releaseYear = new SimpleObjectProperty<>();
  public final ObjectProperty<String[]> genres = object("genres");
  public final DoubleProperty rating = doubleProperty();
  public final IntegerProperty runtime = integer();

  public final ObjectProperty<ImageHandle> image = object("image");
  public final ObjectProperty<ImageHandle> background = object("background");
  public final ObjectProperty<ImageHandle> banner = object("banner");

  public final ObjectProperty<ObservableList<Casting>> castings = list(new EnricherBuilder<T, List<Casting>>(List.class)
    .require(item)
    .enrich(new EnrichCallback<List<Casting>>() {
      @Override
      public List<Casting> enrich(Object... parameters) {
        Item item = (Item)parameters[0];

        List<hs.mediasystem.dao.Casting> castings = item.getCastings();
        List<Casting> result = new ArrayList<>();

        for(final hs.mediasystem.dao.Casting casting : castings) {
          Person p = create(Person.class);

          p.personRecord.set(casting.getPerson());

          Casting c = new Casting();

          c.media.set(Media.this);
          c.person.set(p);
          c.characterName.set(casting.getCharacterName());
          c.index.set(casting.getIndex());
          c.role.set(casting.getRole());

          result.add(c);
        }

        return result;
      }
    })
    .finish(new FinishEnrichCallback<List<Casting>>() {
      @Override
      public void update(List<Casting> result) {
        castings.set(FXCollections.observableList(result));
      }
    })
    .build()
  );

  public Media(String initialTitle, String initialSubtitle, Integer initialReleaseYear) {
    title.set(initialTitle == null ? "" : initialTitle);
    subtitle.set(initialSubtitle == null ? "" : initialSubtitle);
    releaseYear.set(initialReleaseYear);

    item.addListener(new ChangeListener<Item>() {
      @Override
      public void changed(ObservableValue<? extends Item> observableValue, Item old, Item current) {
        title.set(current.getTitle());
        background.set(createImageHandle(current.getBackground(), current, "background"));
        banner.set(createImageHandle(current.getBanner(), current, "banner"));
        image.set(createImageHandle(current.getPoster(), current, "poster"));
        description.set(current.getPlot());
        rating.set(current.getRating());
        runtime.set(current.getRuntime());
        genres.set(current.getGenres());
        releaseDate.set(current.getReleaseDate());
      }
    });
  }

  public Media(String title) {
    this(title, null, null);
  }

  public Media() {
    this(null);
  }

  private static SourceImageHandle createImageHandle(Source<byte[]> source, Item item, String keyPostFix) {
    String key = "Media:/" + item.getTitle() + "-" + item.getSeason() + "x" + item.getEpisode() + "-" + item.getImdbId() + "-" + keyPostFix;

    return source == null ? null : new SourceImageHandle(source, key);
  }

  @Override
  public String toString() {
    return "Media('" + title.get() +"', " + item.get() + ")";
  }
}
