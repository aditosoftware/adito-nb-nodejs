package de.adito.aditoweb.nbm.nodejs.impl.actions;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.netbeans.modules.masterfs.watcher.IADITOWatcherSymlinkProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link NPMInstallCommandAction}.
 *
 * @author r.hartinger, 09.01.2023
 */
class NPMInstallCommandActionTest
{
  
  /**
   * Tests the method {@link NPMInstallCommandAction#getAfterCommandAction()}.
   */
  @Nested
  class GetAfterCommandAction
  {
    /**
     * Tests that the method from {@link AbstractNPMInstallCommand} is called. This test is simpler than the test in {@link AbstractNPMInstallCommandTest}.
     */
    @Test
    void shouldGetAfterCommandAction()
    {
      IADITOWatcherSymlinkProvider watcherSymlinkProvider = Mockito.mock(IADITOWatcherSymlinkProvider.class);

      BaseAbstractNPMCommandActionUtil.baseGetAfterCommandAction(watcherSymlinkProvider, new NPMInstallCommandAction());

      Mockito.verify(watcherSymlinkProvider).rescanProject(any());
    }
  }


  /**
   * Tests the method {@link NPMInstallCommandAction#getName()}.
   */
  @Nested
  class GetName
  {
    /**
     * Tests the name.
     */
    @Test
    void shouldGetName()
    {
      assertEquals("npm install", new NPMInstallCommandAction().getName());
    }
  }


  /**
   * Tests the method {@link NPMInstallCommandAction#getCommand()}.
   */
  @Nested
  class GetCommand
  {
    /**
     * Tests the command.
     */
    @Test
    void shouldGetCommand()
    {
      assertArrayEquals(new String[]{"install"}, new NPMInstallCommandAction().getCommand());
    }
  }
}
