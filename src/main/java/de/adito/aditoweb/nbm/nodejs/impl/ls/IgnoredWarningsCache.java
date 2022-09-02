package de.adito.aditoweb.nbm.nodejs.impl.ls;

import com.google.gson.Gson;
import de.adito.notification.INotificationFacade;
import de.adito.observables.netbeans.FileObservable;
import de.adito.util.reactive.cache.ObservableCache;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;

import java.io.*;
import java.util.*;

/**
 * @author m.kaspera, 26.08.2022
 */
public class IgnoredWarningsCache
{

  private static final IgnoredWarningsCache INSTANCE = new IgnoredWarningsCache();
  private final ObservableCache cacheObs = new ObservableCache();

  private IgnoredWarningsCache()
  {
  }

  @NotNull
  public static IgnoredWarningsCache getInstance()
  {
    return INSTANCE;
  }

  @NotNull
  public Observable<Set<IgnoredWarningsFacade.WarningsItem>> get(@NotNull Project pProject)
  {
    return cacheObs.calculateParallel(pProject, () -> {
      try
      {
        File ignoredWarningsFile = getIgnoredWarningsFile(pProject);
        return FileObservable.createForPlainFile(ignoredWarningsFile)
            .map(pFile -> readIgnoredWarnings(ignoredWarningsFile));
      }
      catch (IOException pE)
      {
        INotificationFacade.INSTANCE.error(pE);
        return Observable.just(Set.of());
      }
    });
  }

  @SuppressWarnings("ResultOfMethodCallIgnored") // ignore for mkdirs and createNewFile
  @NotNull
  File getIgnoredWarningsFile(@NotNull Project pProject) throws IOException
  {
    File ignoredWarnings = new File(FileUtil.toFile(pProject.getProjectDirectory()), ".aditoprj/ignoredWarnings");
    if (!ignoredWarnings.exists())
    {
      ignoredWarnings.getParentFile().mkdirs();
      ignoredWarnings.createNewFile();
    }
    return ignoredWarnings;
  }

  private Set<IgnoredWarningsFacade.WarningsItem> readIgnoredWarnings(@Nullable File pIgnoreFile) throws FileNotFoundException
  {
    if (pIgnoreFile == null)
      return Set.of();
    IgnoreWarningFix.FileContent fileContent = new Gson().fromJson(new FileReader(pIgnoreFile), IgnoreWarningFix.FileContent.class);
    HashSet<IgnoredWarningsFacade.WarningsItem> warningsSet = new HashSet<>();
    Set<Map.Entry<String, String>> entrySet = Optional.ofNullable(fileContent)
        .map(pFileContent -> pFileContent.content)
        .map(Map::entrySet)
        .orElse(Set.of());
    for (Map.Entry<String, String> warningItem : entrySet)
    {
      warningsSet.add(new IgnoredWarningsFacade.WarningsItem(Integer.parseInt(warningItem.getKey()), warningItem.getValue()));
    }
    return warningsSet;
  }

}
