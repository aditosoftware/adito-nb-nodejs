package de.adito.aditoweb.nbm.nodejs.impl.ls;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.lsp.ILSPHintsFilter;
import org.jetbrains.annotations.*;
import org.openide.filesystems.FileObject;
import org.openide.text.PositionBounds;
import org.openide.util.lookup.ServiceProvider;

import java.util.regex.Pattern;

import static de.adito.aditoweb.nbm.nodejs.impl.dataobjects.JavaScriptDataObject.JS_EXTENSION;

/**
 * Implementation of hints filter for js-files
 *
 * @author s.seemann, 10.01.2022
 */
@ServiceProvider(service = ILSPHintsFilter.class)
public class JSHintsFilter implements ILSPHintsFilter
{
  private static final Pattern _PATTERN_TYPES = Pattern.compile("Type '.+' is not assignable to type '.+'\\.");

  @Override
  public boolean canFilter(@NotNull FileObject pFileObject)
  {
    return pFileObject.getExt().equals(JS_EXTENSION);
  }

  @Override
  public boolean filter(@NotNull FileObject pFileObject, @Nullable String pId, @NotNull String pDescription, @NotNull String pSeverity, @Nullable PositionBounds pRange)
  {
    // in js there are no types, so this hint should be ignored
    return !_PATTERN_TYPES.matcher(pDescription).matches();
  }
}