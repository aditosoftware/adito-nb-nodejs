package de.adito.aditoweb.nbm.nodejs.impl.ls;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.netbeans.spi.editor.hints.*;
import org.openide.filesystems.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;

/**
 *
 *
 * @author m.kaspera, 26.08.2022
 */
public class IgnoreWarningFix implements Fix
{
  private final int id;
  private final String description;
  private final FileObject fileObject;

  public IgnoreWarningFix(int pId, @NotNull String pDescription, @NotNull FileObject pFileObject)
  {
    id = pId;
    description = pDescription;
    fileObject = pFileObject;
  }

  @Override
  public String getText()
  {
    return "Ignore all warnings of type: '" + description + "'";
  }

  @Override
  public ChangeInfo implement() throws Exception
  {
    Project project = FileOwnerQuery.getOwner(fileObject);
    Set<WarningsItem> warningsItems = IgnoredWarningsCache.getInstance().get(project);
    FileContent fileContent = new FileContent();
    Stream<WarningsItem> warningsItemStream = Stream.concat(warningsItems.stream(), Stream.of(new WarningsItem(id, description)));
    fileContent.content = warningsItemStream.collect(Collectors.toMap(pWarningsItem -> String.valueOf(pWarningsItem.id), pWarningsItem -> pWarningsItem.description));
    try(FileWriter writer = new FileWriter(IgnoredWarningsCache.getInstance().getIgnoredWarningsFile(project))) {
      writer.write(new Gson().toJson(fileContent));
      writer.flush();
    }
    // "touch" the given file so that the warnings/errors are generated anew
    Files.setLastModifiedTime(FileUtil.toFile(fileObject).toPath(), FileTime.from(Instant.now()));
    return new ChangeInfo();
  }

  static class FileContent
  {
    public Map<String, String> content;
  }

  static class WarningsItem
  {

    int id;
    String description;

    public WarningsItem(int pId, String pDescription)
    {
      id = pId;
      description = pDescription;
    }

    /**
     * equals overwritten, only the id is important
     *
     * @param pO other object
     * @return true if the object should be considered the same as pO
     */
    @Override
    public boolean equals(Object pO)
    {
      if (this == pO) return true;
      if (pO == null || getClass() != pO.getClass()) return false;
      WarningsItem that = (WarningsItem) pO;
      return id == that.id;
    }

    /**
     * since equals is overwritten, we also need to overwrite hashCode such that equal objects have the same hash. Again, only the id is considered
     * important
     * @return hash of this object
     */
    @Override
    public int hashCode()
    {
      return Objects.hash(id);
    }
  }
}
