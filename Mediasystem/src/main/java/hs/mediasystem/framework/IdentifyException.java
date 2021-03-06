package hs.mediasystem.framework;

public class IdentifyException extends Exception {

  public IdentifyException(MediaItem mediaItem, Exception cause) {
    super(mediaItem.toString(), cause);
  }

  public IdentifyException(MediaItem mediaItem) {
    super(mediaItem.toString());
  }

  public IdentifyException(String message) {
    super(message);
  }
}
