package de.adito.aditoweb.nbm.nodejs.impl.actions;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.netbeans.api.project.Project;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link NPMPublishCommandAction}.
 *
 * @author r.hartinger, 09.01.2023
 */
class NPMPublishCommandActionTest
{


  /**
   * Tests the method {@link NPMPublishCommandAction#getCommand()}.
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
      assertArrayEquals(new String[]{"publish"}, new NPMPublishCommandAction().getCommand());
    }
  }


  /**
   * Tests the method {@link NPMPublishCommandAction#getName()}.
   */
  @Nested
  class GetName
  {
    /**
     * Tests the name
     */
    @Test
    void shouldGetName()
    {
      assertEquals("npm publish", new NPMPublishCommandAction().getName());
    }
  }


  /**
   * Tests the method {@link NPMPublishCommandAction#getAfterCommandAction()}.
   */
  @Nested
  class GetAfterCommandAction
  {
    /**
     * Test that there is no interaction with the project after calling {@link NPMPublishCommandAction#getAfterCommandAction()}.
     */
    @Test
    void shouldGetAfterCommandAction()
    {

      NPMPublishCommandAction commandAction = new NPMPublishCommandAction();


      Project project = Mockito.mock(Project.class);

      assertDoesNotThrow(() -> commandAction.getAfterCommandAction().accept(project));

      Mockito.verifyNoInteractions(project);
    }
  }
}