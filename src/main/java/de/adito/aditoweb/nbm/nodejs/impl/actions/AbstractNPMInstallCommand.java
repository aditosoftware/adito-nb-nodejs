package de.adito.aditoweb.nbm.nodejs.impl.actions;

import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.netbeans.modules.masterfs.watcher.IADITOWatcherSymlinkProvider;
import org.openide.util.Lookup;

import java.util.function.Consumer;

/**
 * Abstract class for all node commands that have something to do with updates.
 * In this class, only the {@link #getAfterCommandAction()} method is implemented. This method will look for updating symlink / local usages of a project.
 *
 * @author r.hartinger, 09.01.2023
 */
public abstract class AbstractNPMInstallCommand extends AbstractNPMCommandAction
{

  @NotNull
  @Override
  protected Consumer<Project> getAfterCommandAction()
  {
    return pProject -> {
      // updates symlink / local usage of a project after the npm install
      IADITOWatcherSymlinkProvider watcherSymlinkProvider = Lookup.getDefault().lookup(IADITOWatcherSymlinkProvider.class);
      if (watcherSymlinkProvider != null)
        watcherSymlinkProvider.rescanProject(pProject);
    };
  }

}
