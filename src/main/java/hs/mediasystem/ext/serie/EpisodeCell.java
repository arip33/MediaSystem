package hs.mediasystem.ext.serie;

import hs.mediasystem.framework.MediaNodeCell;
import hs.mediasystem.framework.MediaItem;
import hs.mediasystem.media.Media;
import hs.mediasystem.screens.DuoLineCell;
import hs.mediasystem.screens.MediaNode;
import hs.mediasystem.util.MapBindings;
import hs.mediasystem.util.StringBinding;
import hs.mediasystem.util.WeakBinder;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;

public class EpisodeCell extends DuoLineCell implements MediaNodeCell {
  private final WeakBinder binder = new WeakBinder();

  @Override
  public void configureCell(MediaNode mediaNode) {
    MediaItem item = mediaNode.getMediaItem();
    StringBinding episodeRange = MapBindings.selectString(mediaNode.dataMapProperty(), Episode.class, "episodeRange");

    binder.unbindAll();

    binder.bind(titleProperty(), MapBindings.selectString(mediaNode.dataMapProperty(), Media.class, "title"));
    binder.bind(ratingProperty(), MapBindings.selectDouble(mediaNode.dataMapProperty(), Media.class, "rating").divide(10));
    binder.bind(extraInfoProperty(), Bindings.when(episodeRange.isNull()).then(new SimpleStringProperty("Special")).otherwise(episodeRange));
    binder.bind(viewedProperty(), item.viewedProperty());

    subtitleProperty().set("");
  }
}