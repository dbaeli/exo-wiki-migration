package org.exoplatform.wiki;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

public class ExoWikiExtractorTest {


    public Set<String> extractMacro(String content) {
        Map<String, Integer> result = ExoWikiExtractor.extractMacro(null,content);
        return  result.keySet();      
    }

    @Test
    public void testNoneMacro() throws Exception {
        Set<String> test = extractMacro("aaa xxx bbb");
        TestCase.assertEquals(0, test.size());
    }

    @Test
    public void testExtractMacroEmpty() throws Exception {
        Set<String> test = extractMacro("test");
        TestCase.assertEquals(0, test.size());
    }

    @Test
    public void testNoneMacroWithDash() throws Exception {
        Set<String> test = extractMacro("Test\n|  instance type|[Large Instance|http://aws.amazon.com/ec2/] (m1.large)");
        TestCase.assertEquals(0, test.size());
    }

    @Test
    public void testExtractMacroSimple() throws Exception {
        Set<String> test = extractMacro("{toc}");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("toc"));
    }

    @Test
    public void testExtractMacroDouble1() throws Exception {
        Set<String> test = extractMacro("something {macro1} something {macro2} something");
        TestCase.assertEquals(2, test.size());
        TestCase.assertTrue(test.contains("macro1"));
        TestCase.assertTrue(test.contains("macro2"));
    }

    @Test
    public void testExtractMacroDouble2() throws Exception {
        Set<String> test = extractMacro("{macro1} something {macro2} something");
        TestCase.assertEquals(2, test.size());
        TestCase.assertTrue(test.contains("macro1"));
        TestCase.assertTrue(test.contains("macro2"));
    }

    @Test
    public void testExtractMacroDouble3() throws Exception {
        Set<String> test = extractMacro("something {macro1} something {macro2}");
        TestCase.assertEquals(2, test.size());
        TestCase.assertTrue(test.contains("macro1"));
        TestCase.assertTrue(test.contains("macro2"));
    }

    @Test
    public void testExtractMacroMultiple3() throws Exception {
        Set<String> test = extractMacro("something {macro1} something {macro2} something {macro3} something {macro4} something {macro5}");
        TestCase.assertEquals(5, test.size());
        TestCase.assertTrue(test.contains("macro1"));
        TestCase.assertTrue(test.contains("macro2"));
        TestCase.assertTrue(test.contains("macro3"));
        TestCase.assertTrue(test.contains("macro4"));
        TestCase.assertTrue(test.contains("macro5"));
    }

    @Test
    public void testExtractMacroWithValueCase1() throws Exception {
        Set<String> test = extractMacro("something {macro1:test}");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("macro1"));
    }

    @Test
    public void testExtractMacroWithValueCase2() throws Exception {
        Set<String> test = extractMacro("something {macro1:test|value=1}");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("macro1"));
    }

    @Test
    public void testExtractMacroWithSpace() throws Exception {
        Set<String> test = extractMacro("something {macro1 }");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("macro1"));
    }

    @Test
    public void testExtractMacroWithCode() throws Exception {
        Set<String> test = extractMacro("something {code} content {test} {code}");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("code"));
    }

    @Test
    public void testExtractMacroWithCodeJava() throws Exception {
        Set<String> test = extractMacro("something {code:java} content {test} {code}");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("code"));
    }

    @Test
    public void testExtractMacroWithCodeTwice() throws Exception {
        Set<String> test = extractMacro("something {code} content {test} {code} something else {macro1} something {code} content {test} {code}");
        TestCase.assertEquals(2, test.size());
        TestCase.assertTrue(test.contains("macro1"));
        TestCase.assertTrue(test.contains("code"));
    }

    @Test
    public void testCodeRemoval() throws Exception {
        String result = ExoWikiExtractor.removeBlocks("code", "aaa{code} xxx {code}bbb");
        TestCase.assertEquals("aaa{code}bbb", result);
    }

    @Test
    public void testStyleRemoval() throws Exception {
        String result = ExoWikiExtractor.removeBlocks("style", "aaa{style} xxx {style}bbb");
        TestCase.assertEquals("aaa{style}bbb", result);
    }

    @Test
    public void testCodeTwiceRemoval() throws Exception {
        String result = ExoWikiExtractor.removeBlocks("code", "aaa{code} xxx {code}bbb{code} xxx {code}ccc");
        TestCase.assertEquals("aaa{code}bbb{code}ccc", result);
    }

    @Test
    public void testCodeTwiceRemovalBraquetInside() throws Exception {
        String result = "aaa{code} xxx {nonmacro} {code}bbb{code} xxx {nonmacro} {code}ccc";
        String content = ExoWikiExtractor.removeBlocks("code", result);
        TestCase.assertEquals("aaa{code}bbb{code}ccc", content);
    }

    @Test
    public void testCodeTwiceRemovalExtendedMacro() throws Exception {
        String result = "aaa{code} xxx {code}bbb{code:java} xxx {code}ccc";
        String content = ExoWikiExtractor.removeBlocks("code", result);
        TestCase.assertEquals("aaa{code}bbb{code}ccc", content);
    }


    @Test
    public void testCodeTwiceRemovalLimits() throws Exception {
        String result = "{code} xxx {code}bbb{code} xxx {code}";
        String content = ExoWikiExtractor.removeBlocks("code", result);
        TestCase.assertEquals("{code}bbb{code}", content);
        result = "{code} xxx {code}bbb{code:java} xxx {code}";
        content = ExoWikiExtractor.removeBlocks("code", result);
        TestCase.assertEquals("{code}bbb{code}", content);
        result = "{code} xxx {macro} {code}bbb{code:java} xxx {code}";
        content = ExoWikiExtractor.removeBlocks("code", result);
        TestCase.assertEquals("{code}bbb{code}", content);
    }

    @Test
    public void testEscapedMacro() throws Exception {
        Set<String> test = extractMacro("aaa\\{escaped}xxx{macro}bbb");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("macro"));
    }

    @Test
    public void testGliffy() throws Exception {

        Set<String> test = extractMacro("{gliffy:name=GateIn OAuth diagram|align=left|size=L|version=5}");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("gliffy"));
    }

    @Test
    public void testEscapedMacro2() throws Exception {
        Set<String> test = extractMacro("aaa\\{escaped}xxx{macro}bbb\\{escaped2}");
        TestCase.assertEquals(1, test.size());
        TestCase.assertTrue(test.contains("macro"));
    }

    @Test
    public void testCountMacros() throws Exception {
        Map<String, Integer> result = ExoWikiExtractor.extractMacro(null,"{macro1} {macro2}bbb{macro1:java} xxx {macro3}");
        TestCase.assertEquals(3, result.keySet().size());
        TestCase.assertEquals(2, (int) result.get("macro1"));
        TestCase.assertEquals(1, (int) result.get("macro2"));
        TestCase.assertEquals(1, (int) result.get("macro3"));
    }



}