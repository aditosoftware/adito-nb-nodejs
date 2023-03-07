package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nodejs.impl.ls.TypeScriptLanguageServerProvider;
import org.jetbrains.annotations.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.modules.lsp.client.spi.ServerRestarter;
import org.openide.util.Lookup;

import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;

/**
 * Test class for {@link RestartLSPAction}.
 *
 * @author r.hartinger, 07.03.2023
 */
class RestartLSPActionTest
{

  /**
   * Tests that a new {@link RestartLSPAction} is created with having the correct key - values.
   */
  @Test
  void testConstructor()
  {
    RestartLSPAction restartLSPAction = new RestartLSPAction();

    Map<String, Object> expected = Map.of("Name", "Restart LSP", "IconResource", "de/adito/aditoweb/nbm/nodejs/impl/actions/restart.svg");

    Map<String, Object> actual = new HashMap<>();
    // Transforming all key - values in the restartAction
    Arrays.stream(restartLSPAction.getKeys())
        .filter(Objects::nonNull)
        // keys should be strings, so we need to transform them
        .filter(String.class::isInstance)
        .map(String.class::cast)
        // finding the value to each key and add it to the actual map
        .forEach(pKey -> {
          Object value = restartLSPAction.getValue(pKey);
          actual.put(pKey, value);
        });

    assertEquals(expected, actual);
  }

  /**
   * Tests the method {@link RestartLSPAction#actionPerformed(ActionEvent, JTextComponent)}
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class ActionPerformed
  {

    /**
     * @return the arguments for {@link #shouldActionPerformed(int, ServerRestarter, String)}
     */
    @NotNull
    private Stream<Arguments> shouldActionPerformed()
    {
      ServerRestarter serverRestarter = Mockito.spy(ServerRestarter.class);

      return Stream.of(
          // no mime type -> will not call restart
          Arguments.of(0, null, null),
          Arguments.of(0, serverRestarter, null),

          // mime type is there, but the method will not find any lsp
          Arguments.of(0, null, "text/json"),
          Arguments.of(0, serverRestarter, "text/json"),

          // correct mime type, LSP can be restarted, if the server restarter is there
          Arguments.of(0, null, "text/javascript"),
          Arguments.of(0, null, "text/typescript"),
          Arguments.of(1, serverRestarter, "text/javascript"),
          Arguments.of(1, serverRestarter, "text/typescript")
      );
    }

    /**
     * Tests if the action is only performed, when an LSP is found, and it contains a ServerRestarter.
     * In these tests, the {@link TypeScriptLanguageServerProvider} will only be found, if the mime type ends with {@code script} (e.g. typescript or javascript).
     *
     * @param pCallStopServer  how often {@link TypeScriptLanguageServerProvider#stopServer(ServerRestarter)} will be called.
     * @param pServerRestarter the {@link ServerRestarter} that should be returned by {@link TypeScriptLanguageServerProvider#getServerRestarter()}
     * @param pMimeType        the mime type that should be returned by the document in the {@link JTextComponent}
     */
    @ParameterizedTest
    @MethodSource
    void shouldActionPerformed(int pCallStopServer, @Nullable ServerRestarter pServerRestarter, @Nullable String pMimeType)
    {
      Document document = Mockito.spy(Document.class);
      Mockito.doReturn(pMimeType).when(document).getProperty("mimeType");

      JTextComponent textComponent = Mockito.mock(JTextComponent.class);
      Mockito.doReturn(document).when(textComponent).getDocument();

      RestartLSPAction restartLSPAction = new RestartLSPAction();

      try (MockedStatic<MimeLookup> mimeLookupMockedStatic = Mockito.mockStatic(MimeLookup.class))
      {
        TypeScriptLanguageServerProvider typeScriptLanguageServerProvider = Mockito.spy(TypeScriptLanguageServerProvider.class);
        Mockito.doReturn(pServerRestarter).when(typeScriptLanguageServerProvider).getServerRestarter();

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(typeScriptLanguageServerProvider).when(lookup).lookup(TypeScriptLanguageServerProvider.class);

        // return the correct lookup when the mime type ends with script
        mimeLookupMockedStatic.when(() -> MimeLookup.getLookup(endsWith("script"))).thenReturn(lookup);
        // return a lookup with nothing in it, when the mime type ends with json
        mimeLookupMockedStatic.when(() -> MimeLookup.getLookup(endsWith("json"))).thenReturn(Mockito.mock(Lookup.class));

        restartLSPAction.actionPerformed(null, textComponent);

        Mockito.verify(typeScriptLanguageServerProvider, Mockito.times(pCallStopServer)).stopServer(any());
      }
    }
  }

}
