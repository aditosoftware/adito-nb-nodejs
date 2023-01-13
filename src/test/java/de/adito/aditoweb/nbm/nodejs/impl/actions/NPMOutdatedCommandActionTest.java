package de.adito.aditoweb.nbm.nodejs.impl.actions;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.netbeans.api.project.Project;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link NPMOutdatedCommandAction}.
 *
 * @author r.hartinger, 09.01.2023
 */
class NPMOutdatedCommandActionTest
{

  /**
   * Tests the method {@link NPMOutdatedCommandAction#getCommand()}.
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
      assertArrayEquals(new String[]{"outdated"}, new NPMOutdatedCommandAction().getCommand());
    }
  }


  /**
   * Tests the method {@link NPMOutdatedCommandAction#getName()}.
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
      assertEquals("npm outdated", new NPMOutdatedCommandAction().getName());
    }
  }


  /**
   * Tests the method {@link NPMOutdatedCommandAction#getAfterCommandAction()}.
   */
  @Nested
  class GetAfterCommandAction
  {
    /**
     * Test that there is no interaction with the project after calling {@link NPMOutdatedCommandAction#getAfterCommandAction()}.
     */
    @Test
    void shouldGetAfterCommandAction()
    {

      NPMOutdatedCommandAction commandAction = new NPMOutdatedCommandAction();

      Project project = Mockito.mock(Project.class);

      assertDoesNotThrow(() -> commandAction.getAfterCommandAction().accept(project));

      Mockito.verifyNoInteractions(project);
    }
  }
}
