package de.adito.aditoweb.nbm.nodejs.impl.parser;

import com.google.gson.Gson;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.INodeJSExecBase;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

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
   * Parses the output of "npm list" and returns the binaries,
   * so that we don't have to use the ".cmd" wrapper.
   *
   * @param pJsonData output of "npm list"
   * @return a map of binaries and their INodeJSExecBase, pointing to the resolved javascript file
   */
  @NotNull
  public static Map<String, INodeJSExecBase> parseBinaries(@NotNull String pJsonData)
  {
    try
    {
      _Type root = new Gson().fromJson(pJsonData, _Type.class);
      if (root.dependencies != null)
      {
        Map<String, INodeJSExecBase> res = new HashMap<>();
        for (Map.Entry<String, _Type> dep : root.dependencies.entrySet())
        {
          if (dep.getValue().bin == null)
            continue;
          for (Map.Entry<String, String> binary : dep.getValue().bin.entrySet())
            res.put(binary.getKey(), INodeJSExecBase.module(dep.getKey(), binary.getValue()));
        }
        return res;
      }
    }
    catch (Exception pE)
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
    public Map<String, _Type> dependencies;
    public Map<String, String> scripts;
    public Map<String, String> bin;
  }

}
