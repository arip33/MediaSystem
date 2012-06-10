package hs.mediasystem.screens;

import hs.mediasystem.framework.MediaItem;
import hs.mediasystem.framework.SubtitleProvider;
import hs.mediasystem.framework.SubtitleProviderException;
import hs.subtitle.SubtitleDescriptor;

import java.util.Collections;
import java.util.List;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class SubtitleQueryService extends Service<List<? extends SubtitleDescriptor>> {
  private MediaItem mediaItem;
  private SubtitleProvider subtitleProvider;

  public void setMediaItem(MediaItem mediaItem) {
    this.mediaItem = mediaItem;
  }

  public void setSubtitleProvider(SubtitleProvider subtitleProvider) {
    this.subtitleProvider = subtitleProvider;
  }

  @Override
  protected Task<List<? extends SubtitleDescriptor>> createTask() {
    final MediaItem mediaItem = this.mediaItem;
    final SubtitleProvider provider = subtitleProvider;

    System.out.println("[FINE] SubtitleQueryService.createTask() - provider=" + subtitleProvider + ": " + mediaItem);

    return new Task<List<? extends SubtitleDescriptor>>() {
      {
        updateMessage("Contacting " + provider.getName() + "...");
      }

      @Override
      protected List<? extends SubtitleDescriptor> call() {
        try {
          List<? extends SubtitleDescriptor> list = provider.query(mediaItem);
          updateMessage("Found " + list.size() + " subtitles at " + provider.getName());
          return list;
        }
        catch(SubtitleProviderException e) {
          updateMessage("Failed to get subtitles from " + provider.getName() + ": " + e.getMessage());
          return Collections.emptyList();
        }
      }
    };
  }
}