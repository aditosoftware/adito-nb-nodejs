package de.adito.aditoweb.nbm.nodejs.impl.ls;

import com.google.gson.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.project.IProjectVisibility;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.openide.filesystems.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

/**
 * @author m.kaspera, 01.09.2022
 */
public class IgnoredWarningsFacade
{

  private static final IgnoredWarningsCache warningsCache = IgnoredWarningsCache.getInstance();

  private IgnoredWarningsFacade()
  {
  }

  @NotNull
  public static Observable<Set<WarningsItem>> getIgnoredWarnings(@NotNull Project pProject)
  {
    return warningsCache.get(pProject);
  }

  public static void addIgnoredWarning(@NotNull Project pProject, int pId, @NotNull String pDescription) throws IOException
  {
    Project project = getRootProject(pProject);
    Set<WarningsItem> warningsItems = getIgnoredWarnings(project).blockingFirst();
    Stream<WarningsItem> warningsItemStream = Stream.concat(warningsItems.stream(),
                                                            Stream.of(new WarningsItem(pId, pDescription)));
    writeToFile(project, warningsItemStream);
  }

  @NotNull
  private static Project getRootProject(@NotNull Project pProject)
  {
    if(Boolean.TRUE.equals(pProject.getLookup().lookup(IProjectVisibility.class).isVisible()))
      return pProject;
    return Optional.ofNullable(pProject.getProjectDirectory())
        .map(FileObject::getParent)
        .map(FileOwnerQuery::getOwner)
        .map(IgnoredWarningsFacade::getRootProject)
        .orElse(pProject);
  }

  public static void addIgnoredWarnings(@NotNull Project pProject, @NotNull List<WarningsItem> pWarningsItems) throws IOException
  {
    Set<WarningsItem> warningsItems = getIgnoredWarnings(pProject).blockingFirst();
    Stream<WarningsItem> warningsItemStream = Stream.concat(warningsItems.stream(), pWarningsItems.stream());
    writeToFile(pProject, warningsItemStream);
  }

  public static void unIgnoreWarnings(@NotNull Project pProject, @NotNull List<WarningsItem> pWarningsItems) throws IOException
  {
    Set<WarningsItem> warningsItems = getIgnoredWarnings(pProject).blockingFirst();
    pWarningsItems.forEach(warningsItems::remove);
    writeToFile(pProject, warningsItems.stream());
  }

  private static void writeToFile(@NotNull Project pProject, Stream<WarningsItem> pWarningsItemStream) throws IOException
  {
    IgnoreWarningFix.FileContent fileContent = new IgnoreWarningFix.FileContent();
    fileContent.content = pWarningsItemStream
        .distinct()
        .collect(Collectors.toMap(pWarningsItem -> String.valueOf(pWarningsItem.getId()), WarningsItem::getDescription));
    try (FileWriter writer = new FileWriter(IgnoredWarningsCache.getIgnoredWarningsFile(pProject)))
    {
      writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(fileContent));
      writer.flush();
    }
    FileUtil.toFileObject(IgnoredWarningsCache.getIgnoredWarningsFile(pProject)).refresh();
  }

  public static class WarningsItem
  {

    private final int id;
    @NotNull
    private final String description;

    public WarningsItem(int pId, @NotNull String pDescription)
    {
      id = pId;
      description = pDescription;
    }

    public int getId()
    {
      return id;
    }

    @NotNull
    public String getDescription()
    {
      return description;
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
     *
     * @return hash of this object
     */
    @Override
    public int hashCode()
    {
      return Objects.hash(id);
    }
  }
}
