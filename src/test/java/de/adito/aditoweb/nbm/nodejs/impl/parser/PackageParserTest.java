package de.adito.aditoweb.nbm.nodejs.impl.parser;

import com.google.common.collect.Sets;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.INodeJSExecBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author w.glanzer, 12.05.2021
 * @see PackageParser
 */
class PackageParserTest
{

  @Test
  void shouldParsePackageJsonWithScripts()
  {
    Assertions.assertEquals(Map.of("myScript1", "dummyscript run something"),
                            PackageParser.parseScripts(new StringReader("{ scripts: { \"myScript1\": \"dummyscript run something\" } }")));
  }

  @Test
  void shouldParsePackageJsonWithoutScripts()
  {
    Assertions.assertEquals(Map.of(), PackageParser.parseScripts(new StringReader("{ }")));
    Assertions.assertEquals(Map.of(), PackageParser.parseScripts(new StringReader("{ name: \"test\" }")));
  }

  @Test
  void shouldParseEmptyPackageJsonScripts()
  {
    Assertions.assertEquals(Map.of(), PackageParser.parseScripts(new StringReader("{ scripts: {  } }")));
  }

  @Test
  void shouldParseInvalidPackageJsonScripts()
  {
    Assertions.assertEquals(Map.of(), PackageParser.parseScripts(new StringReader("{ scripts: {  äö / \"")));
  }


  /**
   * Tests the method {@link PackageParser#parseScripts(File)}.
   */
  @Nested
  class ParseScripts
  {
    private final Map.Entry<String, String> resetData = Map.entry("reset:data", "run reset.sh");
    private final Map.Entry<String, String> publishModule = Map.entry("publishModule", "npm publish");
    private final Map.Entry<String, String> startMariaDb = Map.entry("startMariaDB", "node ./scripts/mariaDB.js");


    /**
     * Tests the method call with a normal package.json.
     *
     * @throws URISyntaxException if the filename cannot be converted to URI
     */
    @Test
    void shouldParseScriptsBasic() throws URISyntaxException
    {
      baseParseScripts(Set.of(resetData, startMariaDb), "package_basic.json");
    }

    /**
     * Tests the method call with a modularized project package.json.
     *
     * @throws URISyntaxException if the filename cannot be converted to URI
     */
    @Test
    void shouldParseScriptsModularizedProject() throws URISyntaxException
    {
      baseParseScripts(Set.of(publishModule, resetData), "package_project.json");
    }

    /**
     * Tests the method call with a modularized module package.json.
     *
     * @throws URISyntaxException if the filename cannot be converted to URI
     */
    @Test
    void shouldParseScriptsModularizedModule() throws URISyntaxException
    {
      baseParseScripts(Set.of(publishModule, startMariaDb), "package_module.json");

    }

    /**
     * Basic method for calling {@link PackageParser#parseScripts(File)}. This method reads the file from the resources folder and passes it as file to the method.
     *
     * @param expectedValues the expected map entries that should be returned by the method
     * @param filename       The name of the file in the resources folder
     * @throws URISyntaxException If the read url of the filename can not be converted into an {@link URI}
     */
    private void baseParseScripts(@NotNull Set<Map.Entry<String, String>> expectedValues, @NotNull String filename) throws URISyntaxException
    {
      // builds the expected map out of the set
      Map<String, String> expected = expectedValues.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // loads the given .json file and checks that it exists
      URL url = PackageParserTest.class.getClassLoader().getResource(PackageParserTest.class.getPackageName().replace(".", "/") + "/" + filename);
      assertNotNull(url);

      // transform the url to file
      File file = new File(url.toURI());

      Map<String, String> actual = PackageParser.parseScripts(file);

      assertEquals(expected, actual);
    }
  }


  /**
   * Tests the method {@link PackageParser#parseBinaries(String)}
   */
  @Nested
  class ParseBinaries
  {
    /**
     * Tests that the binaries are found from an expample json
     */
    @Test
    void shouldParseBinaries()
    {

      String jsonData = "\n" +
          "{\n" +
          "  \"version\": \"1.0.0\",\n" +
          "  \"name\": \"project\",\n" +
          "  \"private\": true,\n" +
          "  \"type\": \"project\",\n" +
          "  \"scripts\": {\n" +
          "    \"list\": \"npm list -j -l\"\n" +
          "  },\n" +
          "  \"_id\": \"project@1.0.0\",\n" +
          "  \"extraneous\": false,\n" +
          "  \"path\": \"C:\\\\dev\\\\projects\\\\modularization\\\\project\",\n" +
          "  \"_dependencies\": {\n" +
          "    \"module1\": \"../module1\"\n" +
          "  },\n" +
          "  \"devDependencies\": {},\n" +
          "  \"peerDependencies\": {},\n" +
          "  \"dependencies\": {\n" +
          "    \"module1\": {\n" +
          "      \"version\": \"1.0.0\",\n" +
          "      \"resolved\": \"file:../../module1\",\n" +
          "      \"overridden\": false,\n" +
          "      \"name\": \"module1-1\",\n" +
          "      \"_id\": \"module1-1@1.0.0\",\n" +
          "      \"extraneous\": false,\n" +
          "      \"path\": \"C:\\\\dev\\\\projects\\\\modularization\\\\project\\\\node_modules\\\\module1\",\n" +
          "      \"_dependencies\": {},\n" +
          "      \"devDependencies\": {},\n" +
          "      \"peerDependencies\": {},\n" +
          "      \"bin\": {\"a\": \"b\", \"c\": \"d\"}\n" +
          "    }\n" +
          "  }\n" +
          "}";

      Map<String, INodeJSExecBase> result = PackageParser.parseBinaries(jsonData);

      assertAll(
          () -> assertEquals(2, result.size(), "length should be 2"),
          () -> assertEquals(Sets.newHashSet("a", "c"), result.keySet(), "keys should be a and c"),
          () -> assertEquals("node_modules/module1/b", result.get("a").getBasePath(), "base path of a"),
          () -> assertEquals("node_modules/module1/d", result.get("c").getBasePath(), "base path of c"));
    }
  }

}

