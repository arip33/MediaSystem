package hs.mediasystem.screens;

import hs.mediasystem.beans.AsyncImageProperty;
import hs.mediasystem.framework.MediaNodeCell;
import hs.mediasystem.framework.MediaItem;
import hs.mediasystem.media.Serie;
import hs.mediasystem.util.WeakBinder;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BannerCell extends HBox implements MediaNodeCell {
  private final WeakBinder binder = new WeakBinder();
  private final int fitWidth;

  private final Label title = new Label() {{
    setId("selectItem-listCell-title");
  }};

  public BannerCell(int fitWidth) {
    this.fitWidth = fitWidth;

    getChildren().add(new VBox() {{
      getChildren().add(title);
      HBox.setHgrow(this, Priority.ALWAYS);
    }});
  }

  public BannerCell() {
    this(350);
  }

  @Override
  public void configureCell(MediaNode mediaNode) {
    final MediaItem item = mediaNode.getMediaItem();

    binder.unbindAll();

    // A banner from TVDB is 758 x 140

    if(item != null) {
      final AsyncImageProperty asyncImageProperty = new AsyncImageProperty();
      StringProperty titleProperty = new SimpleStringProperty();

      Serie serie = item.get(Serie.class);

      binder.bind(titleProperty, item.getMedia().titleProperty());
      binder.bind(asyncImageProperty.imageHandleProperty(), serie.bannerProperty());

      title.setMinHeight(fitWidth * 140 / 758);

      binder.bind(title.textProperty(), Bindings.when(asyncImageProperty.isNull()).then(titleProperty).otherwise(""));
      binder.bind(title.graphicProperty(), Bindings.when(asyncImageProperty.isNull()).then((ImageView)null).otherwise(new ImageView() {{
        imageProperty().bind(asyncImageProperty);
        setPreserveRatio(true);
        setFitWidth(fitWidth);
      }}));
    }
  }
}