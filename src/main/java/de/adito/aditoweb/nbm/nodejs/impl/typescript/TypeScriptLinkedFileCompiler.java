package de.adito.aditoweb.nbm.nodejs.impl.typescript;

import com.google.gson.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.transpiler.ILinkedFileCompiler;
import de.adito.aditoweb.nbm.nodejs.impl.*;
import org.jetbrains.annotations.NotNull;
import org.openide.util.io.*;
import org.openide.util.lookup.ServiceProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ILinkedFileCompiler} for TypeScript support
 *
 * @author p.neub, 26.04.2023
 */
@ServiceProvider(service = ILinkedFileCompiler.class)
public final class TypeScriptLinkedFileCompiler implements ILinkedFileCompiler
{
  @Override
  public String getName()
  {
    return "TypeScript";
  }

  @NotNull
  @Override
  public String getSourceExtension()
  {
    return "ts";
  }

  @NotNull
  @Override
  public String getTargetExtension()
  {
    return "js";
  }

  @NotNull
  @Override
  public CompletableFuture<Void> compileFiles(@NotNull Path pRoot, @NotNull List<Path> pFiles)
  {
    INodeJSExecutor executor = NodeJSInstallation.getCurrent().getExecutor();
    INodeJSEnvironment bundledEnvironment = NodeJSInstallation.getCurrent().getEnvironment();
    INodeJSExecBase tscBinary = new NPMCommandExecutor(executor, bundledEnvironment, true, null).binaries().get("tsc");
    String tscBinaryPath = bundledEnvironment.resolveExecBase(tscBinary).getAbsolutePath();

    try
    {
      TsConfigJson tsConfigJson = new TsConfigJson();
      tsConfigJson.files = pFiles.stream().map(Path::toString).collect(Collectors.toList());
      tsConfigJson.include = List.of();
      tsConfigJson.exclude = List.of("**/*");
      tsConfigJson.compilerOptions = new TsConfigJson.CompilerOptions();
      tsConfigJson.compilerOptions.noEmit = false;
      tsConfigJson.compilerOptions.checkJs = false;
      tsConfigJson.compilerOptions.sourceMap = true;

      Path extendsTsConfig = pRoot.resolve("tsconfig.json");
      if (Files.exists(extendsTsConfig))
        tsConfigJson.extendsPath = extendsTsConfig.toString();

      Path tsconfig = Files.createTempFile("tsconfig.jdito", ".json");
      try (final FileWriter writer = new FileWriter(tsconfig.toFile()))
      {
        new Gson().toJson(tsConfigJson, TsConfigJson.class, writer);
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      return executor.executeAsync(bundledEnvironment, INodeJSExecBase.node(), baos, baos, new NullInputStream(), tscBinaryPath, "--build", tsconfig.toString())
          .thenApply(pExitCode -> {
            // Don't delete the temporary tsconfig, if an error occurred. This is intentional.
            if (pExitCode != 0)
              throw new CompilationException(baos.toString(StandardCharsets.UTF_8));

            try
            {
              Files.delete(tsconfig);
            }
            catch (IOException pE)
            {
              throw new RuntimeException(pE);
            }
            return null;
          });
    }
    catch (IOException pE)
    {
      throw new RuntimeException(pE);
    }
  }
}
