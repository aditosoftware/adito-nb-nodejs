package de.adito.aditoweb.nbm.nodejs.impl.parser;

import org.junit.jupiter.api.*;

import java.io.StringReader;
import java.util.Map;

/**
 * @author w.glanzer, 12.05.2021
 * @see PackageParser
 */
class Test_PackageParser
{

  @Test
  void test_parseScripts_filled()
  {
    Assertions.assertEquals(Map.of("myScript1", "dummyscript run something"),
                            PackageParser.parseScripts(new StringReader("{ scripts: { \"myScript1\": \"dummyscript run something\" } }")));
  }

  @Test
  void test_parseScripts_noScriptsElement()
  {
    Assertions.assertEquals(Map.of(), PackageParser.parseScripts(new StringReader("{ }")));
    Assertions.assertEquals(Map.of(), PackageParser.parseScripts(new StringReader("{ name: \"test\" }")));
  }

  @Test
  void test_parseScripts_empty()
  {
    Assertions.assertEquals(Map.of(), PackageParser.parseScripts(new StringReader("{ scripts: {  } }")));
  }

  @Test
  void test_parseScripts_invalid()
  {
    Assertions.assertEquals(Map.of(), PackageParser.parseScripts(new StringReader("{ scripts: {  äö / \"")));
  }

}