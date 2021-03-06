package hs.mediasystem.screens;

import hs.mediasystem.framework.Media;
import hs.mediasystem.util.MapBindings;
import hs.mediasystem.util.ThreadSafeDateFormat;

import java.text.DateFormat;
import java.util.Date;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;

public class MediaItemFormatter {
  private static final ThreadSafeDateFormat DATE_FORMAT = new ThreadSafeDateFormat(DateFormat.getDateInstance(DateFormat.MEDIUM));

  public static StringBinding releaseYearBinding(final MediaNode node) {
    return new StringBinding() {
      final ObjectBinding<Date> selectReleaseDate = MapBindings.select(node.media, "releaseDate");
      final ObjectBinding<Integer> selectReleaseYear = MapBindings.select(node.media, "releaseYear");

      {
        bind(selectReleaseDate, selectReleaseYear);
      }

      @Override
      protected String computeValue() {
        String releaseTime = selectReleaseDate.get() == null ? null : String.format("%tY", selectReleaseDate.get());

        if(releaseTime == null) {
          releaseTime = selectReleaseYear.get() == null ? "" : "" + selectReleaseYear.get();
        }

        return releaseTime;
      }
    };
  }

  public static StringBinding releaseTimeBinding(final ObservableValue<Media<?>> media) {
    return new StringBinding() {
      final ObjectBinding<Date> selectReleaseDate = MapBindings.select(media, "releaseDate");
      final ObjectBinding<Integer> selectReleaseYear = MapBindings.select(media, "releaseYear");

      {
        bind(selectReleaseDate, selectReleaseYear);
      }

      @Override
      protected String computeValue() {
        String releaseTime = selectReleaseDate.get() == null ? null : DATE_FORMAT.format(selectReleaseDate.get());

        if(releaseTime == null) {
          releaseTime = selectReleaseYear.get() == null ? "" : "" + selectReleaseYear.get();
        }

        return releaseTime;
      }
    };
  }

  public static StringBinding formattedDate(final ObservableValue<Date> date) {
    return new StringBinding() {
      {
        bind(date);
      }

      @Override
      protected String computeValue() {
        String releaseTime = date.getValue() == null ? null : DATE_FORMAT.format(date.getValue());

        return releaseTime;
      }
    };
  }
}
