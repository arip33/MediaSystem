package hs.mediasystem.screens.selectmedia;

import hs.mediasystem.dao.Setting.PersistLevel;
import hs.mediasystem.framework.MediaRoot;
import hs.mediasystem.framework.MediaRootType;
import hs.mediasystem.framework.SettingUpdater;
import hs.mediasystem.framework.SettingsStore;
import hs.mediasystem.screens.MediaNode;
import hs.mediasystem.screens.MediaNodeEvent;
import hs.mediasystem.util.GridPaneUtil;
import hs.mediasystem.util.ServiceTracker;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import javax.inject.Inject;

import org.osgi.framework.BundleContext;

public class StandardView extends StackPane implements SelectMediaView {

  private final ObjectProperty<EventHandler<MediaNodeEvent>> onNodeSelected = new SimpleObjectProperty<>();
  @Override public ObjectProperty<EventHandler<MediaNodeEvent>> onNodeSelected() { return onNodeSelected; }

  private final ObjectProperty<EventHandler<MediaNodeEvent>> onNodeAlternateSelect = new SimpleObjectProperty<>();
  @Override public ObjectProperty<EventHandler<MediaNodeEvent>> onNodeAlternateSelect() { return onNodeAlternateSelect; }

  private final ReadOnlyObjectWrapper<MediaNode> focusedNode = new ReadOnlyObjectWrapper<>();
  @Override public ReadOnlyObjectProperty<MediaNode> focusedNodeProperty() { return focusedNode.getReadOnlyProperty(); }

  private final ServiceTracker<StandardLayoutExtension> standardLayoutExtensionTracker;
  private final BackgroundPane backgroundPane = new BackgroundPane();
  private final SettingUpdater<StandardLayoutExtension> settingUpdater;

  private final ObjectProperty<StandardLayoutExtension> layoutExtension = new SimpleObjectProperty<>();

  private MediaNode currentRoot;
  private StandardLayout layout;

  @Inject
  public StandardView(SettingsStore settingsStore, BundleContext bundleContext) {
    standardLayoutExtensionTracker = new ServiceTracker<>(bundleContext, StandardLayoutExtension.class);

    getStylesheets().add("select-media/duo-pane-select-media-view.css");

    GridPane stage = GridPaneUtil.create(new double[] {100}, new double[] {90, 10});

    stage.add(backgroundPane, 0, 0);
    stage.add(new StackPane() {{
      getStyleClass().add("stage");
    }}, 0, 1);

    getChildren().add(stage);

    layoutExtension.addListener(new ChangeListener<StandardLayoutExtension>() {
      @Override
      public void changed(ObservableValue<? extends StandardLayoutExtension> observableValue, StandardLayoutExtension oldLayoutExtension, StandardLayoutExtension layoutExtension) {
        MediaNode selectedNode = null;

        if(layout != null) {
          if(currentRoot.equals(layout.getRoot())) {
            selectedNode = layout.getSelectedNode();  // it only makes sense to re-select the focused node if the root did not change
          }

          layout.onNodeSelected().set(null);
          layout.onNodeAlternateSelect().set(null);
          focusedNode.unbind();

          getChildren().remove(layout);
        }

        layout = layoutExtension.createLayout();
        getChildren().add((Node)layout);

        backgroundPane.mediaNodeProperty().bind(layout.mediaNodeBinding());

        layout.onNodeSelected().set(onNodeSelected.get());
        layout.onNodeAlternateSelect().set(onNodeAlternateSelect.get());
        focusedNode.bind(layout.focusedNodeProperty());

        layout.setRoot(currentRoot);

        if(selectedNode != null) {
          setSelectedNode(selectedNode);
        }
      }
    });

    settingUpdater = new SettingUpdater<>(settingsStore, new StringConverter<StandardLayoutExtension>() {
      @Override
      public StandardLayoutExtension fromString(String id) {
        for(StandardLayoutExtension extension : availableLayoutExtensions) {
          if(extension.getId().equals(id)) {
            return extension;
          }
        }

        return null;
      }

      @Override
      public String toString(StandardLayoutExtension extension) {
        return extension.getId();
      }
    });

    layoutExtension.addListener(settingUpdater);
  }

  @Override
  public void setRoot(MediaNode root) {
    determineAvailableLayouts(root.getMediaRoot());

    settingUpdater.setBackingSetting("MediaSystem:SelectMedia", PersistLevel.PERMANENT, "View:" + root.getId());

    StandardLayoutExtension layoutExtension = settingUpdater.getStoredValue(availableLayoutExtensions.get(0));

    currentRoot = root;

    if(!layoutExtension.equals(this.layoutExtension.get())) {
      this.layoutExtension.set(layoutExtension);
    }
    else {
      layout.setRoot(root);
    }
  }

  @Override
  public void requestFocus() {
    if(layout != null) {
      ((Node)layout).requestFocus();
    }
  }

  @Override
  public MediaNode getSelectedNode() {
    return layout == null ? null : layout.getSelectedNode();
  }

  @Override
  public void setSelectedNode(MediaNode mediaNode) {
    if(layout != null) {
      layout.setSelectedNode(mediaNode);
    }
  }

  private static final Map<Class<? extends MediaRoot>, MediaRootType> TYPES = new HashMap<>();

  public static void registerLayout(Class<? extends MediaRoot> cls, MediaRootType type) {
    TYPES.put(cls, type);
  }

  private final ObservableList<StandardLayoutExtension> availableLayoutExtensions = FXCollections.observableArrayList();

  private void determineAvailableLayouts(MediaRoot root) {
    availableLayoutExtensions.clear();

    MediaRootType rootType = TYPES.get(root.getClass());

    for(StandardLayoutExtension extension : standardLayoutExtensionTracker.getServices()) {
      if(extension.getSupportedMediaRootTypes().contains(rootType)) {
        availableLayoutExtensions.add(extension);
      }
    }

    Collections.sort(availableLayoutExtensions, new Comparator<StandardLayoutExtension>() {
      @Override
      public int compare(StandardLayoutExtension o1, StandardLayoutExtension o2) {
        return o1.getTitle().compareTo(o2.getTitle());
      }
    });
  }

  @Override
  public ObservableList<StandardLayoutExtension> availableLayoutExtensionsList() {
    return availableLayoutExtensions;
  }

  @Override
  public ObjectProperty<StandardLayoutExtension> layoutExtensionProperty() {
    return layoutExtension;
  }
}
