package hs.mediasystem.ext.media.serie;

import hs.mediasystem.dao.LocalInfo;
import hs.mediasystem.framework.Decoder;
import hs.mediasystem.framework.NameDecoder;
import hs.mediasystem.framework.NameDecoder.DecodeResult;
import hs.mediasystem.framework.NameDecoder.Hint;

import java.nio.file.Path;

public class EpisodeDecoder implements Decoder {
  private final String serieName;
  private final NameDecoder nameDecoder = new NameDecoder(Hint.EPISODE);

  public EpisodeDecoder(String serieName) {
    assert serieName != null;

    this.serieName = serieName;
  }

  @Override
  public LocalInfo decode(Path path) {
    DecodeResult result = nameDecoder.decode(path.getFileName().toString());

    String sequence = result.getSequence();
    String title = result.getSubtitle();
    Integer year = result.getReleaseYear();

    Integer season = null;
    Integer episode = null;
    Integer endEpisode = null;

    if(sequence != null) {
      String[] split = sequence.split("[-,]");

      season = split[0].isEmpty() ? 1 : Integer.parseInt(split[0]);
      episode = split.length > 1 ? Integer.parseInt(split[1]) : 0;
      endEpisode = split.length > 2 ? Integer.parseInt(split[2]) : episode;
    }

    return new LocalInfo(path.toString(), serieName, title, null, null, year, season, episode, endEpisode);
  }
}
