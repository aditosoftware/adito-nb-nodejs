package de.adito.aditoweb.nbm.nodejs.impl.ls;

import com.google.gson.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.project.IProjectVisibility;
import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
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


  private IgnoredWarningsFacade()
  {
  }

  @NonNull
  public static Observable<Set<WarningsItem>> getIgnoredWarnings(@NonNull Project pProject)
  {
    return Optional.ofNullable(pProject.getLookup().lookup(IgnoredWarningsProvider.class))
        .map(IgnoredWarningsProvider::get)
        .orElse(Observable.just(Set.of()));
  }

  public static void addIgnoredWarning(@NonNull Project pProject, int pId, @NonNull String pDescription) throws IOException
  {
    Project project = getRootProject(pProject);
    Set<WarningsItem> warningsItems = getIgnoredWarnings(project).blockingFirst();
    Stream<WarningsItem> warningsItemStream = Stream.concat(warningsItems.stream(),
                                                            Stream.of(new WarningsItem(pId, pDescription)));
    writeToFile(project, warningsItemStream);
  }

  @NonNull
  private static Project getRootProject(@NonNull Project pProject)
  {
    if (Boolean.TRUE.equals(pProject.getLookup().lookup(IProjectVisibility.class).isVisible()))
      return pProject;
    return Optional.ofNullable(pProject.getProjectDirectory())
        .map(FileObject::getParent)
        .map(FileOwnerQuery::getOwner)
        .map(IgnoredWarningsFacade::getRootProject)
        .orElse(pProject);
  }

  public static void addIgnoredWarnings(@NonNull Project pProject, @NonNull List<WarningsItem> pWarningsItems) throws IOException
  {
    Set<WarningsItem> warningsItems = getIgnoredWarnings(pProject).blockingFirst();
    Stream<WarningsItem> warningsItemStream = Stream.concat(warningsItems.stream(), pWarningsItems.stream());
    writeToFile(pProject, warningsItemStream);
  }

  public static void unIgnoreWarnings(@NonNull Project pProject, @NonNull List<WarningsItem> pWarningsItems) throws IOException
  {
    Set<WarningsItem> warningsItems = getIgnoredWarnings(pProject).blockingFirst();
    pWarningsItems.forEach(warningsItems::remove);
    writeToFile(pProject, warningsItems.stream());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored") // ignore for delete, mkdirs and createNewFile
  private static void writeToFile(@NonNull Project pProject, Stream<WarningsItem> pWarningsItemStream) throws IOException
  {
    IgnoreWarningFix.FileContent fileContent = new IgnoreWarningFix.FileContent();
    fileContent.content = pWarningsItemStream
        .distinct()
        .collect(Collectors.toMap(pWarningsItem -> String.valueOf(pWarningsItem.getId()), WarningsItem::getDescription));

    File file = IgnoredWarningsProvider.getIgnoredWarningsFile(pProject);

    if (fileContent.content.size() == 0)
    {
      if (file.exists())
        FileUtil.toFileObject(file).delete();
      return;
    }

    if (!file.exists())
    {
      file.getParentFile().mkdirs();
      file.createNewFile();
    }

    try (FileWriter writer = new FileWriter(file))
    {
      writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(fileContent));
      writer.flush();
    }

    FileUtil.toFileObject(file).refresh();
  }

  public static class WarningsItem
  {

    private final int id;
    @NonNull
    private final String description;

    public WarningsItem(int pId, @NonNull String pDescription)
    {
      id = pId;
      description = pDescription;
    }

    public int getId()
    {
      return id;
    }

    @NonNull
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
