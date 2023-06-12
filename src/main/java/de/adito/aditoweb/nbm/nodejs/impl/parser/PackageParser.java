package de.adito.aditoweb.nbm.nodejs.impl.parser;

import com.google.gson.Gson;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.INodeJSExecBase;
import lombok.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * @author w.glanzer, 12.05.2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PackageParser
{
  private static final Logger logger = Logger.getLogger(PackageParser.class.getName());

  /**
   * Parses the package.json and extracts all set scripts.
   * Key: ScriptName, Value: Script
   *
   * @param pPackageJson package.json
   * @return the map of scripts
   */
  @NonNull
  public static Map<String, String> parseScripts(@NonNull File pPackageJson)
  {
    try
    {
      return parseScripts(new FileReader(pPackageJson));
    }
    catch (Exception e)
    {
      // just return empty map, but log the exception
      logger.log(Level.WARNING, e.getMessage(), e);
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
  @NonNull
  public static Map<String, String> parseScripts(@NonNull Reader pPackageJsonReader)
  {
    try
    {
      _Type content = new Gson().fromJson(pPackageJsonReader, _Type.class);
      if (content != null && content.scripts != null)
        return content.scripts;
    }
    catch (Exception e)
    {
      // just return empty map, but log the exception
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    return Map.of();
  }

  /**
   * Parses the output of "npm list -j -l" and returns the binaries,
   * so that we don't have to use the ".cmd" wrapper.
   *
   * @param pJsonData output of "npm list -j -l"
   * @return a map of binaries and their INodeJSExecBase, pointing to the resolved javascript file
   */
  @NonNull
  public static Map<String, INodeJSExecBase> parseBinaries(@NonNull String pJsonData)
  {
    try
    {
      NpmListType root = new Gson().fromJson(pJsonData, NpmListType.class);
      if (root.dependencies != null)
      {
        Map<String, INodeJSExecBase> res = new HashMap<>();
        for (Map.Entry<String, NpmListType> dep : root.dependencies.entrySet())
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
      // just return empty map, but log the exception
      logger.log(Level.WARNING, pE.getMessage(), pE);
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

  /**
   * gson type of the result of {@code npm list -j -l}
   */
  private static class NpmListType extends _Type
  {
    public Map<String, NpmListType> dependencies;
    public Map<String, String> bin;
  }

}
