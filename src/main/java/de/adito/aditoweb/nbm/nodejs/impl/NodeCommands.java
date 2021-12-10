package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Contains a set of NodeJS / NPM commands
 *
 * @author w.glanzer, 25.10.2021
 */
public class NodeCommands
{

  /**
   * Executes the "npm install" command and installs all given dependencies
   *
   * @param pExecutor     NodeJS Executor to execute commands
   * @param pEnvironment  Environment of NodeJS
   * @param pPath         Path to the NodeJS project (used as prefix)
   * @param pDependencies Dependencies that should be installed
   */
  public static void install(@NotNull INodeJSExecutor pExecutor, @NotNull INodeJSEnvironment pEnvironment, @NotNull String pPath, @NotNull String... pDependencies)
      throws IOException, InterruptedException, TimeoutException
  {
    List<String> arguments = new ArrayList<>(Arrays.asList(pDependencies));
    arguments.addAll(0, List.of("install", "--prefix", pPath));
    pExecutor.executeSync(pEnvironment, INodeJSExecBase.packageManager(), 30000, arguments.toArray(new String[0]));
  }

  /**
   * Executes the "npm update" command and updates all given dependencies
   *
   * @param pExecutor     NodeJS Executor to execute commands
   * @param pEnvironment  Environment of NodeJS
   * @param pPath         Path to the NodeJS project (used as prefix)
   * @param pDependencies Dependencies that should be updated
   */
  public static void update(@NotNull INodeJSExecutor pExecutor, @NotNull INodeJSEnvironment pEnvironment, @NotNull String pPath, @NotNull String... pDependencies)
      throws IOException, InterruptedException, TimeoutException
  {
    List<String> arguments = new ArrayList<>(Arrays.asList(pDependencies));
    arguments.addAll(0, List.of("update", "--prefix", pPath));
    pExecutor.executeSync(pEnvironment, INodeJSExecBase.packageManager(), 30000, arguments.toArray(new String[0]));
  }

  /**
   * Executes the "npm outdated" command and determines, if one of the given dependencies is outdated.
   *
   * @param pExecutor     NodeJS Executor to execute commands
   * @param pEnvironment  Environment of NodeJS
   * @param pPath         Path to the NodeJS project (used as prefix)
   * @param pDependencies Dependencies that should be checked
   * @return true, if one dependency is outdated
   */
  public static boolean outdated(@NotNull INodeJSExecutor pExecutor, @NotNull INodeJSEnvironment pEnvironment, @NotNull String pPath, @NotNull String... pDependencies)
      throws IOException, InterruptedException, TimeoutException
  {
    List<String> arguments = new ArrayList<>(Arrays.asList(pDependencies));
    arguments.addAll(0, List.of("outdated", "--prefix", pPath));
    String result = pExecutor.executeSync(pEnvironment, INodeJSExecBase.packageManager(), 30000, arguments.toArray(new String[0]));
    return result.trim().split("\n").length > 1; // something is outdated, if we get more than one line (the first line is the command line, that should be skipped)
  }

  /**
   * Executes the "npm list" command and determines, if all of the given dependencies are installed.
   * It does not handle outdated dependencies - it just verifies, if it is installed
   *
   * @param pExecutor     NodeJS Executor to execute commands
   * @param pEnvironment  Environment of NodeJS
   * @param pPath         Path to the NodeJS project (used as prefix)
   * @param pDependencies Dependencies that should be checked
   * @return true, if all of the dependencies are installed
   */
  public static boolean list(@NotNull INodeJSExecutor pExecutor, @NotNull INodeJSEnvironment pEnvironment, @NotNull String pPath, @NotNull String... pDependencies)
      throws IOException, InterruptedException, TimeoutException
  {
    List<String> arguments = new ArrayList<>(Arrays.asList(pDependencies));
    arguments.addAll(0, List.of("list", "--prefix", pPath));
    String result = pExecutor.executeSync(pEnvironment, INodeJSExecBase.packageManager(), 30000, arguments.toArray(new String[0]));
    return Arrays.stream(result.split("\n"))
        .skip(1) //the first line is the command line, that should be skipped
        .filter(pLine -> !pLine.trim().isEmpty())
        .noneMatch(pLine -> pLine.trim().toLowerCase(Locale.ROOT).endsWith("(empty)"));
  }

}
