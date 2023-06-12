package de.adito.aditoweb.nbm.nodejs.impl.actions;

import lombok.NonNull;
import org.jetbrains.annotations.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.netbeans.api.project.Project;
import org.netbeans.modules.masterfs.watcher.IADITOWatcherSymlinkProvider;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link AbstractNPMInstallCommand}.
 *
 * @author r.hartinger, 09.01.2023
 */
class AbstractNPMInstallCommandTest
{

  /**
   * Tests the method {@link AbstractNPMInstallCommand#getAfterCommandAction()}.
   */
  @Nested
  class GetAfterCommandAction
  {
    /**
     * Tests that the method will work with no {@link IADITOWatcherSymlinkProvider} found.
     */
    @Test
    void shouldWorkWithNoSymlinkProvider()
    {
      assertGetAfterCommand(null);
    }

    /**
     * Tests that the method will work with {@link IADITOWatcherSymlinkProvider} found and that the
     * {@link IADITOWatcherSymlinkProvider#rescanProject(Project)} method will be called.
     */
    @Test
    void shouldWorkAndCallSymlinkProvider()
    {
      IADITOWatcherSymlinkProvider iaditoWatcherSymlinkProvider = Mockito.mock(IADITOWatcherSymlinkProvider.class);

      assertGetAfterCommand(iaditoWatcherSymlinkProvider);

      Mockito.verify(iaditoWatcherSymlinkProvider).rescanProject(any());
    }

    /**
     * Basic test method for {@link AbstractNPMInstallCommand#getAfterCommandAction()}.
     * It will create a simple instance of {@link AbstractNPMInstallCommand} and calls the method and the consumer given by the method.
     *
     * @param pWatcherSymlinkProvider the {@link IADITOWatcherSymlinkProvider} that will be returned by {@code Lookup.getDefault().lookup(IADITOWatcherSymlinkProvider.class)}
     */
    private void assertGetAfterCommand(@Nullable IADITOWatcherSymlinkProvider pWatcherSymlinkProvider)
    {
      AbstractNPMInstallCommand abstractNPMInstallCommand = new AbstractNPMInstallCommand()
      {
        @NonNull
        @Override
        protected String[] getCommand()
        {
          return new String[0];
        }

        @Override
        public String getName()
        {
          return "junit";
        }
      };

      BaseAbstractNPMCommandActionUtil.baseGetAfterCommandAction(pWatcherSymlinkProvider, abstractNPMInstallCommand);
    }
  }


}
