package de.adito.aditoweb.nbm.nodejs.impl.ls;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.lsp.ILSPFixesFilter;
import org.jetbrains.annotations.NotNull;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

import java.util.regex.Pattern;

import static de.adito.aditoweb.nbm.nodejs.impl.dataobjects.JavaScriptDataObject.JS_EXTENSION;

/**
 * Implementation of fixes filter for js files.
 * If a ADITO module is imported, only the module "@aditosoftware/jdito-types" should be used, not "@aditosoftware/jdito-types/dist/defintion_*"
 *
 * @author s.seemann, 10.01.2022
 */
@ServiceProvider(service = ILSPFixesFilter.class)
public class JSImportFixesFilter implements ILSPFixesFilter
{
  private static final Pattern _PATTERN_TYPES = Pattern.compile("Import '(.+)' from module \"@aditosoftware/jdito-types/dist/definition_\\1\"");

  @Override
  public boolean canFilter(@NotNull FileObject pFileObject)
  {
    return pFileObject.getExt().equals(JS_EXTENSION);
  }

  @Override
  public boolean filter(@NotNull FileObject pFileObject, @NotNull String pFixText)
  {
    return !_PATTERN_TYPES.matcher(pFixText).matches();
  }
}
