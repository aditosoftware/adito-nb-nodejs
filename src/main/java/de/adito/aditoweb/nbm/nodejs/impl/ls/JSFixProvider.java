package de.adito.aditoweb.nbm.nodejs.impl.ls;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.lsp.ILSPFixProvider;
import org.jetbrains.annotations.*;
import org.netbeans.spi.editor.hints.*;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides additional Fixes for Warnings or Errors passed from the LSP server.
 * Contains only the IgnoreWarningFix (that sets the id of the current warning/error on the ignored list) as yet
 *
 * @author m.kaspera, 26.08.2022
 */
@ServiceProvider(service = ILSPFixProvider.class)
public class JSFixProvider implements ILSPFixProvider
{

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("'\\w+'");

  @Override
  public List<Fix> provideFixes(@NotNull FileObject pFileObject, int pId, @NotNull String pDescription, @NotNull String pSeverity, @Nullable Range pRange)
  {
    String adjustedDescription = VARIABLE_PATTERN.matcher(pDescription).replaceAll("'XXX'");
    return List.of(new IgnoreWarningFix(pId, adjustedDescription, pFileObject));
  }


}
