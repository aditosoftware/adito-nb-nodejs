package de.adito.aditoweb.nbm.nodejs.impl.ls;

import com.google.gson.Gson;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.project.IProjectVisibility;
import de.adito.notification.INotificationFacade;
import de.adito.observables.netbeans.FileObservable;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.*;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 26.08.2022
 */
@ServiceProvider(service = IgnoredWarningsProvider.class, path = "Projects/de-adito-project/Lookup")
public class IgnoredWarningsProvider
{

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
   * IMPORTANT: only use this method if the instance of this serviceProvider was obtained by querying the lookup of the actual project,
   * if you got obtained this instance by querying the default lookup use the get(Project pProject) method.
   *
   * @return Observable of the Set of WarningsItems that are ignored for the given project. Contains items of the parent project(s) if this project
   * is only used as a module, or an empty Observable if this serviceProvider did not get a project
   */
  @NotNull
  public Observable<Set<IgnoredWarningsFacade.WarningsItem>> get()
  {
    if (project != null)
      return get(project);
    else
      return Observable.just(Set.of());
  }

  /**
   * @param pProject Project whose warningsItems should be obtained
   * @return Observable of the Set of WarningsItems that are ignored for the given project. Contains items of the parent project(s) if this project
   * is only used as a module
   */
  @NotNull
  public Observable<Set<IgnoredWarningsFacade.WarningsItem>> get(@NotNull Project pProject)
  {
    if (warningsObs == null)
    {
      try
      {
        if (Boolean.TRUE.equals(pProject.getLookup().lookup(IProjectVisibility.class).isVisible()))
        {
          File ignoredWarningsFile = getIgnoredWarningsFile(pProject);
          warningsObs = FileObservable.createForPlainFile(ignoredWarningsFile)
              .map(pFile -> readIgnoredWarnings(ignoredWarningsFile));
        }
        else
        {
          List<Observable<Set<IgnoredWarningsFacade.WarningsItem>>> warningsItemsObs = new ArrayList<>();
          Project currentProj = pProject;
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
      catch (IOException pE)
      {
        INotificationFacade.INSTANCE.error(pE);
        warningsObs = Observable.just(Set.of());
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
  @NotNull
  private static Set<IgnoredWarningsFacade.WarningsItem> combineSetArray(@NotNull Object[] setSet)
  {
    //noinspection unchecked array contains sets of WarningsItems, but has to be object array to satisfy signatures
    return Arrays.stream(setSet)
        .map(pX -> ((Set<IgnoredWarningsFacade.WarningsItem>) pX))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored") // ignore for mkdirs and createNewFile
  @NotNull
  public static File getIgnoredWarningsFile(@NotNull Project pProject) throws IOException
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
