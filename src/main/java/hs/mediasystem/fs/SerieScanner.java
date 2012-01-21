package hs.mediasystem.fs;

import hs.mediasystem.db.LocalInfo;
import hs.mediasystem.db.MediaType;
import hs.mediasystem.framework.Scanner;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SerieScanner implements Scanner<Serie> {

  @Override
  public List<Serie> scan(Path scanPath) {
    List<Serie> series = new ArrayList<>();

    try {
      try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(scanPath)) {
        for(Path path : dirStream) {
          if(Files.isDirectory(path) && !path.getFileName().toString().startsWith(".")) {
            LocalInfo localInfo = new LocalInfo(path, MediaType.SERIE, path.getFileName().toString(), null, null, null, null, null);

            series.add(new Serie(localInfo));
          }
        }
      }
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }

    return series;
  }
}
