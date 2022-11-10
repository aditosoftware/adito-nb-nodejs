package de.adito.aditoweb.nbm.nodejs.impl.parser;

import org.junit.jupiter.api.*;

import java.io.StringReader;
import java.util.Map;

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

}