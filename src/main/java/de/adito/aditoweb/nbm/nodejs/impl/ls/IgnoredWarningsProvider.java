package de.adito.aditoweb.nbm.nodejs.impl.ls;

import com.google.gson.Gson;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.project.IProjectVisibility;
import de.adito.observables.netbeans.FileObservable;
import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.project.*;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 26.08.2022
 */
@ServiceProvider(service = IgnoredWarningsProvider.class, path = "Projects/de-adito-project/Lookup")
public class IgnoredWarningsProvider
{

  private static final Logger _LOGGER = Logger.getLogger(IgnoredWarningsProvider.class.getName());

  private Observable<Set<IgnoredWarningsFacade.WarningsItem>> warningsObs = null;
  private final Project project;

  public IgnoredWarningsProvider()
  {
    project = null;
  }

  public IgnoredWarningsProvider(Project pProject)
  {
    project = pProject;
  }

  /**
   * IMPORTANT: this method requires a project, so it only works
   * if the instance of this serviceProvider was obtained by querying the lookup of the actual project.
   *
   * @return Observable of the Set of WarningsItems that are ignored for the given project. Contains items of the parent project(s) if this project
   * is only used as a module, or an empty Observable if this serviceProvider did not get a project
   */
  @NonNull
  public Observable<Set<IgnoredWarningsFacade.WarningsItem>> get()
  {
    if (project == null)
      throw new IllegalStateException("IgnoredWarningsProvider has to be obtained from the project lookup");
    if (warningsObs == null)
    {
      if (Boolean.TRUE.equals(project.getLookup().lookup(IProjectVisibility.class).isVisible()))
      {
        File ignoredWarningsFile = getIgnoredWarningsFile(project);
        warningsObs = FileObservable.createForPlainFile(ignoredWarningsFile)
            .map(pFile -> readIgnoredWarnings(ignoredWarningsFile));
      }
      else
      {
        List<Observable<Set<IgnoredWarningsFacade.WarningsItem>>> warningsItemsObs = new ArrayList<>();
        Project currentProj = project;
        boolean breakLoop = false;
        while (currentProj != null && !breakLoop)
        {
          breakLoop = (Boolean.TRUE.equals(currentProj.getLookup().lookup(IProjectVisibility.class).isVisible()));
          File ignoredWarningsFile = getIgnoredWarningsFile(currentProj);
          warningsItemsObs.add(FileObservable.createForPlainFile(ignoredWarningsFile)
                                   .map(pFile -> readIgnoredWarnings(ignoredWarningsFile)));
          currentProj = FileOwnerQuery.getOwner(currentProj.getProjectDirectory().getParent());
        }
        warningsObs = Observable.combineLatest(warningsItemsObs, IgnoredWarningsProvider::combineSetArray);
      }
    }
    return warningsObs;
  }

  /**
   * Combine an array containing sets of warningsItems to a single set
   *
   * @param setSet array of sets of warningsItems
   * @return combined set
   */
  @NonNull
  private static Set<IgnoredWarningsFacade.WarningsItem> combineSetArray(Object @NonNull [] setSet)
  {
    //noinspection unchecked array contains sets of WarningsItems, but has to be object array to satisfy signatures
    return Arrays.stream(setSet)
        .map(pX -> ((Set<IgnoredWarningsFacade.WarningsItem>) pX))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Returns the ignored warnings file for the given project
   *
   * @param pProject the project
   * @return the ignored warnings file
   */
  @NonNull
  public static File getIgnoredWarningsFile(@NonNull Project pProject)
  {
    return new File(FileUtil.toFile(pProject.getProjectDirectory()), ".aditoprj/ignoredWarnings");
  }

  /**
   * Reads, and parses the provided ignored warnings file into a set of {@link IgnoredWarningsFacade.WarningsItem}
   *
   * @param pIgnoreFile the ignored warnings file
   * @return the set of {@link IgnoredWarningsFacade.WarningsItem}
   */
  private static Set<IgnoredWarningsFacade.WarningsItem> readIgnoredWarnings(@Nullable File pIgnoreFile)
  {
    if (pIgnoreFile == null || !pIgnoreFile.exists())
      return Set.of();
    HashSet<IgnoredWarningsFacade.WarningsItem> warningsSet = new HashSet<>();
    try
    {
      Set<Map.Entry<String, String>> entrySet = Optional.ofNullable(readIgnoredWarningsFileContent(pIgnoreFile))
          .map(pFileContent -> pFileContent.content)
          .map(Map::entrySet)
          .orElse(Set.of());
      for (Map.Entry<String, String> warningItem : entrySet)
      {
        warningsSet.add(new IgnoredWarningsFacade.WarningsItem(Integer.parseInt(warningItem.getKey()), warningItem.getValue()));
      }
    }
    catch (IOException pE)
    {
      _LOGGER.log(Level.WARNING, "Could not read ignored warnings file", pE);
      return Set.of();
    }
    return warningsSet;
  }

  /**
   * Reads, and parses the provided ignored warnings file
   *
   * @param pIgnoreFile the ignored warnings file
   * @return the content of the ignored warnings file parsed as {@link IgnoreWarningFix.FileContent} or null if the file is empty
   * @throws IOException if reading the ignored warnings file failed
   */
  @Nullable
  private static IgnoreWarningFix.FileContent readIgnoredWarningsFileContent(@NonNull File pIgnoreFile) throws IOException
  {
    try (FileReader fr = new FileReader(pIgnoreFile))
    {
      return new Gson().fromJson(fr, IgnoreWarningFix.FileContent.class);
    }
  }
}
