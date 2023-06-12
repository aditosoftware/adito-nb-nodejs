package de.adito.aditoweb.nbm.nodejs.impl.ls;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.lsp.ILSPFixesFilter;
import lombok.NonNull;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

import java.util.regex.*;

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
  private static final Pattern _PATTERN_IMPORT = Pattern.compile("Import '(.+)' from module \"(.*)\"");
  private static final String _JDITO_MODULE = "@aditosoftware/jdito-types";

  @Override
  public boolean canFilter(@NonNull FileObject pFileObject)
  {
    return pFileObject.getExt().equals(JS_EXTENSION);
  }

  @Override
  public boolean filter(@NonNull FileObject pFileObject, @NonNull String pFixText)
  {
    Matcher matcher = _PATTERN_IMPORT.matcher(pFixText);
    if (matcher.find())
    {
      String group = matcher.group(2);
      if (group.equals(_JDITO_MODULE))
        return true;

      return !group.contains(_JDITO_MODULE);
    }
    return true;
  }
}
