package de.adito.aditoweb.nbm.nodejs.impl.ls;

import com.google.common.cache.*;
import com.google.gson.Gson;
import de.adito.observables.netbeans.FileObservable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author m.kaspera, 26.08.2022
 */
public class IgnoredWarningsCache
{

  private static final IgnoredWarningsCache INSTANCE = new IgnoredWarningsCache();
  private final CompositeDisposable disposable = new CompositeDisposable();
  private final LoadingCache<Project, Set<IgnoreWarningFix.WarningsItem>> warningsCache = CacheBuilder.newBuilder().build(new CacheLoader<>()
  {
    @Override
    public Set<IgnoreWarningFix.WarningsItem> load(@NotNull Project pProject) throws Exception
    {
      File ignoredWarningsFile = getIgnoredWarningsFile(pProject);
      disposable.add(FileObservable.createForPlainFile(ignoredWarningsFile)
                         .map(pFile -> readIgnoredWarnings(ignoredWarningsFile)).subscribe(pWarningsItems -> warningsCache.put(pProject, pWarningsItems)));
      return readIgnoredWarnings(ignoredWarningsFile);
    }
  });

  private IgnoredWarningsCache()
  {
  }

  static IgnoredWarningsCache getInstance()
  {
    return INSTANCE;
  }

  public Set<IgnoreWarningFix.WarningsItem> get(@NotNull Project pProject)
  {
    try
    {
      return warningsCache.get(pProject);
    }
    catch (ExecutionException pE)
    {
      return Set.of();
    }
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

  private Set<IgnoreWarningFix.WarningsItem> readIgnoredWarnings(@Nullable File pIgnoreFile) throws FileNotFoundException
  {
    if (pIgnoreFile == null)
      return Set.of();
    IgnoreWarningFix.FileContent fileContent = new Gson().fromJson(new FileReader(pIgnoreFile), IgnoreWarningFix.FileContent.class);
    HashSet<IgnoreWarningFix.WarningsItem> warningsSet = new HashSet<>();
    Set<Map.Entry<String, String>> entrySet = Optional.ofNullable(fileContent)
        .map(pFileContent -> pFileContent.content)
        .map(Map::entrySet)
        .orElse(Set.of());
    for (Map.Entry<String, String> warningItem : entrySet)
    {
      warningsSet.add(new IgnoreWarningFix.WarningsItem(Integer.parseInt(warningItem.getKey()), warningItem.getValue()));
    }
    return warningsSet;
  }

}
