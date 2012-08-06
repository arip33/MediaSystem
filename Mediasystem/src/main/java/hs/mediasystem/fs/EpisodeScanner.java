package hs.mediasystem.fs;

import hs.mediasystem.dao.LocalInfo;
import hs.mediasystem.framework.Decoder;
import hs.mediasystem.framework.Scanner;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EpisodeScanner implements Scanner<LocalInfo> {
  private static final Pattern EXTENSION_PATTERN = Pattern.compile("(?i).+\\.(avi|flv|mkv|mov|mp4|mpg|mpeg)");

  private final Decoder decoder;

  public EpisodeScanner(Decoder decoder) {
    this.decoder = decoder;
  }

  @Override
  public List<LocalInfo> scan(Path rootPath) {
    try {
      List<Path> scanPaths = new ArrayList<>();

      scanPaths.add(rootPath);

      try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(rootPath)) {
        for(Path path : dirStream) {
          if(Files.isDirectory(path)) {
            scanPaths.add(path);
          }
        }
      }

      List<LocalInfo> results = new ArrayList<>();

      for(Path scanPath : scanPaths) {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(scanPath)) {
          for(Path path : dirStream) {
            if(!Files.isDirectory(path) && path.getFileName().toString().matches(EXTENSION_PATTERN.pattern())) {
              LocalInfo localInfo = decoder.decode(path);

              if(localInfo != null) {
                results.add(localInfo);
              }
              else {
                System.err.println("EpisodeScanner: Could not decode as episode: " + path);
              }
            }
          }
        }
      }

      return results;
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }
  }
}
