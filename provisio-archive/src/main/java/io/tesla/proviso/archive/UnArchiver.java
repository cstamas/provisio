package io.tesla.proviso.archive;

import io.provis.model.ProvisioContext;
import io.provis.model.RuntimeEntry;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.codehaus.plexus.util.SelectorUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

// useRoot
// directories
//   includes
//   excludes

// There should be a full inventory of what has gone into the archive
// make a fluent interface

@Named
@Singleton
public class UnArchiver {

  private final List<String> includes;
  private final List<String> excludes;
  private final boolean useRoot;
  private final boolean flatten;
  
  public UnArchiver(List<String> includes, List<String> excludes, boolean useRoot, boolean flatten) {
    this.includes = includes;
    this.excludes = excludes;
    this.useRoot = useRoot;
    this.flatten = flatten;
  }

  public Map<String,RuntimeEntry> unarchive(File archive, File outputDirectory) throws IOException {
    return unarchive(archive, outputDirectory, null);
  }
  
  public Map<String,RuntimeEntry> unarchive(File archive, File outputDirectory, ProvisioContext context) throws IOException {
    //
    // These are the contributions that unpacking this archive is providing
    //
    Map<String,RuntimeEntry> entries = new HashMap<String,RuntimeEntry>();

    if (outputDirectory.exists() == false) {
      outputDirectory.mkdirs();
    }

    ArchiveInputStream ais = null;

    if (archive.getName().endsWith(".zip") || archive.getName().endsWith(".jar")) {
      ais = new ZipArchiveInputStream(new FileInputStream(archive));
    } else if (archive.getName().endsWith(".tgz") || archive.getName().endsWith("tar.gz")) {
      ais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archive)));
    }

    if (ais == null) {
      throw new RuntimeException("Cannot detect how to read " + archive.getName());
    }

    ArchiveEntry archiveEntry;
    while ((archiveEntry = ais.getNextEntry()) != null) {
      String entryName = archiveEntry.getName();
      if (useRoot == false) {
        entryName = entryName.substring(entryName.indexOf('/') + 1);
      }
      //
      // If we get an exclusion that matches then just carry on.
      //
      boolean exclude = false;
      if (!excludes.isEmpty()) {
        for (String excludePattern : excludes) {
          if (isExcluded(excludePattern, entryName)) {
            exclude = true;
            break;
          }
        }
      }

      if (exclude) {
        continue;
      }

      //
      // need useRoot = false and step in so just truncate the beginning of the name entry
      //
      boolean include = false;
      if (!includes.isEmpty()) {
        for (String includePattern : includes) {
          if (isIncluded(includePattern, entryName)) {
            include = true;
            break;
          }
        }
      } else {
        include = true;
      }

      if (include == false) {
        continue;
      }

      //
      // So with an entry we may want to take a set of entry in a set of directories and flatten them
      // into one directory, or we may want to preserve the directory structure.
      //

      if (flatten) {
        entryName = entryName.substring(entryName.lastIndexOf("/") + 1);
      } else {
        if (archiveEntry.isDirectory()) {
          createDir(new File(outputDirectory, entryName));
          continue;
        }
      }

      File outputFile = new File(outputDirectory, entryName);

      //
      // If we take an archive and flatten it into the output directory the first entry will
      // match the output directory which exists so it will cause an error trying to make it
      //
      if (outputFile.getAbsolutePath().equals(outputDirectory.getAbsolutePath())) {
        continue;
      }

      if (!outputFile.getParentFile().exists()) {
        createDir(outputFile.getParentFile());
      }

      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
      ByteStreams.copy(ais, outputStream);
      Closeables.closeQuietly(outputStream);
      
      int mode = 0;
      if(archiveEntry instanceof ZipArchiveEntry) {
        mode = ((ZipArchiveEntry)archiveEntry).getUnixMode();
      } else if(archiveEntry instanceof TarArchiveEntry) {
        mode = ((TarArchiveEntry)archiveEntry).getMode();        
      }
      
      // Preserve the executable bit on the way out
      if(FileMode.EXECUTABLE_FILE.equals(mode)) {
        outputFile.setExecutable(true);
      }
      
      entries.put(entryName, new RuntimeEntry(entryName,mode));      
    }

    //
    // The ArchiveInputStream needs to be closed when the whole archive is processed.
    //
    Closeables.closeQuietly(ais);

    return entries;
  }

  private void createDir(File dir) {
    if (dir.exists() == false) {
      dir.mkdirs();
    }
  }

  private boolean isExcluded(String excludePattern, String entry) {
    return SelectorUtils.match(excludePattern, entry);
  }

  private boolean isIncluded(String includePattern, String entry) {
    return SelectorUtils.match(includePattern, entry);
  }
  
  //  
  //    Archiver archiver = Archiver.builder()
  //        .includes("**/*.java")
  //        .includes(Iterable<String>)
  //        .excludes("**/*.properties")
  //        .excludes(Iterable<String>)
  //        .flatten(true)
  //        .useRoot(false)
  //        .build();
  
  
  public static UnArchiverBuilder builder() {
    return new UnArchiverBuilder();
  }
  
  public static class UnArchiverBuilder {

    private List<String> includes = new ArrayList<String>();
    private List<String> excludes = new ArrayList<String>();
    private boolean useRoot = true;
    private boolean flatten = false;
    
    public UnArchiverBuilder includes(String... includes) {
      return includes(ImmutableList.copyOf(includes));
    }

    public UnArchiverBuilder includes(Iterable<String> includes) {
      Iterables.addAll(this.includes, includes);
      return this;
    }

    public UnArchiverBuilder excludes(String... excludes) {
      return excludes(ImmutableList.copyOf(excludes));
    }

    public UnArchiverBuilder excludes(Iterable<String> excludes) {
      Iterables.addAll(this.excludes, excludes);
      return this;
    }

    public UnArchiverBuilder useRoot(boolean useRoot) {
      this.useRoot = useRoot;
      return this;
    }

    public UnArchiverBuilder flatten(boolean flatten) {
      this.flatten = flatten;
      return this;
    }

    public UnArchiver build() {
      return new UnArchiver(includes, excludes, useRoot, flatten);
    }
    
  }
  
  
}