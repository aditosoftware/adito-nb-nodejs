package de.adito.aditoweb.nbm.nodejs.impl.parser;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;

/**
 * @author w.glanzer, 12.05.2021
 */
public class PackageParser
{
  /**
   * Parses the package.json and extracts all set scripts.
   * Key: ScriptName, Value: Script
   *
   * @param pPackageJson package.json
   * @return the map of scripts
   */
  @NotNull
  public static Map<String, String> parseScripts(@NotNull File pPackageJson)
  {
    try
    {
      return parseScripts(new FileReader(pPackageJson));
    }
    catch (Exception e)
    {
      // just return empty map
    }

    return Map.of();
  }

  /**
   * Parses the package.json and extracts all set scripts.
   * Key: ScriptName, Value: Script
   *
   * @param pPackageJsonReader package.json reader
   * @return the map of scripts
   */
  @NotNull
  public static Map<String, String> parseScripts(@NotNull Reader pPackageJsonReader)
  {
    try
    {
      _Type content = new Gson().fromJson(pPackageJsonReader, _Type.class);
      if (content != null && content.scripts != null)
        return content.scripts;
    }
    catch (Exception e)
    {
      // just return empty map
    }

    return Map.of();
  }

  /**
   * gson type of package.json
   */
  private static class _Type
  {
    public Map<String, String> scripts;
  }

}
