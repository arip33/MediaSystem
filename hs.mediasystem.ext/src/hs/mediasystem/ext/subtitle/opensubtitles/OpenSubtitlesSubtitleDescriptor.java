
package hs.mediasystem.ext.subtitle.opensubtitles;


import hs.subtitle.SubtitleDescriptor;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

/**
 * Describes a subtitle on OpenSubtitles.
 *
 * @see OpenSubtitlesXmlRpc
 */
public class OpenSubtitlesSubtitleDescriptor implements SubtitleDescriptor {

  public static enum Property {
    IDSubtitle,
    IDSubtitleFile,
    IDSubMovieFile,
    IDMovie,
    IDMovieImdb,
    SubFileName,
    SubFormat,
    SubHash,
    SubSize,
    MovieHash,
    MovieByteSize,
    MovieName,
    MovieNameEng,
    MovieYear,
    MovieReleaseName,
    MovieTimeMS,
    MovieImdbRating,
    SubLanguageID,
    ISO639,
    LanguageName,
    UserID,
    UserNickName,
    SubAddDate,
    SubAuthorComment,
    SubComments,
    SubDownloadsCnt,
    SubRating,
    SubBad,
    SubActualCD,
    SubSumCD,
    MatchedBy,
    SubtitlesLink,
    SubDownloadLink,
    ZipDownloadLink;

    public static <V> EnumMap<Property, V> asEnumMap(Map<String, V> stringMap) {
      EnumMap<Property, V> enumMap = new EnumMap<>(Property.class);

      // copy entry set to enum map
      for (Entry<String, V> entry : stringMap.entrySet()) {
        try {
          enumMap.put(Property.valueOf(entry.getKey()), entry.getValue());
        } catch (IllegalArgumentException e) {
          // illegal enum constant, just ignore
        }
      }

      return enumMap;
    }
  }


  private final Map<Property, String> properties;


  public OpenSubtitlesSubtitleDescriptor(Map<Property, String> properties) {
    this.properties = properties;
  }


  public String getProperty(Property key) {
    return properties.get(key);
  }


  @Override
  public String getName() {
    return getProperty(Property.SubFileName);
  }


  @Override
  public String getLanguageName() {
    return getProperty(Property.LanguageName);
  }


  @Override
  public String getType() {
    return getProperty(Property.SubFormat);
  }

  @Override
  public MatchType getMatchType() {
    String matchedBy = getProperty(Property.MatchedBy);

    if(matchedBy != null) {
      if(matchedBy.equalsIgnoreCase("moviehash")) {
        return MatchType.HASH;
      }
      if(matchedBy.equalsIgnoreCase("imdbid")) {
        return MatchType.ID;
      }
      if(matchedBy.equalsIgnoreCase("fulltext")) {
        return MatchType.NAME;
      }
    }

    return MatchType.UNKNOWN;
  }

  public int getLength() {
    return Integer.parseInt(getProperty(Property.SubSize));
  }


  public String getMovieHash() {
    return getProperty(Property.MovieHash);
  }


  public long getMovieByteSize() {
    return Long.parseLong(getProperty(Property.MovieByteSize));
  }

  @Override
  public byte[] getSubtitleRawData() throws IOException {
    URL resource = new URL(getProperty(Property.SubDownloadLink));

    try(DataInputStream stream = new DataInputStream(new GZIPInputStream(resource.openStream()))) {
      byte[] data = new byte[getLength()];

      stream.readFully(data);

      return data;
    }
  }

  @Override
  public int hashCode() {
    return getProperty(Property.IDSubtitle).hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof OpenSubtitlesSubtitleDescriptor) {
      OpenSubtitlesSubtitleDescriptor other = (OpenSubtitlesSubtitleDescriptor) object;
      return getProperty(Property.IDSubtitle).equals(other.getProperty(Property.IDSubtitle));
    }

    return false;
  }


  @Override
  public String toString() {
    return String.format("%s [%s]", getName(), getLanguageName());
  }
}