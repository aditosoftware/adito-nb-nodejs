package de.adito.aditoweb.nbm.nodejs.impl.actions;

import lombok.*;
import org.jetbrains.annotations.*;
import org.mockito.*;
import org.netbeans.api.project.Project;
import org.netbeans.modules.masterfs.watcher.IADITOWatcherSymlinkProvider;
import org.openide.util.Lookup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Basic utility class for testing {@link AbstractNPMCommandAction} and its subclasses.
 *
 * @author r.hartinger, 09.01.2023
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseAbstractNPMCommandActionUtil
{

  /**
   * Creates a basis test method for {@link AbstractNPMCommandAction#getAfterCommandAction()}.
   * The method tests that the method call does not throw any error and that {@link Lookup#getDefault()} is called.
   *
   * @param pWatcherSymlinkProvider   the {@link IADITOWatcherSymlinkProvider} that should be returned by {@code Lookup.getDefault().lookup(IADITOWatcherSymlinkProvider.class)}.
   * @param abstractNPMInstallCommand the implementation of an {@link AbstractNPMCommandAction} that should call the method
   */
  public static void baseGetAfterCommandAction(@Nullable IADITOWatcherSymlinkProvider pWatcherSymlinkProvider, @NonNull AbstractNPMInstallCommand abstractNPMInstallCommand)
  {
    Project project = Mockito.spy(Project.class);

    try (MockedStatic<Lookup> lookupMockedStatic = Mockito.mockStatic(Lookup.class))
    {
      Lookup lookup = Mockito.mock(Lookup.class);
      Mockito.doReturn(pWatcherSymlinkProvider).when(lookup).lookup(IADITOWatcherSymlinkProvider.class);

      lookupMockedStatic.when(Lookup::getDefault).thenReturn(lookup);

      assertDoesNotThrow(() -> abstractNPMInstallCommand.getAfterCommandAction().accept(project));

      Mockito.verify(lookup).lookup(IADITOWatcherSymlinkProvider.class);
      lookupMockedStatic.verify(Lookup::getDefault);
    }
  }

}
