/* LanguageTool, a natural language style checker 
 * Copyright (C) 2006 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package de.danielnaber.languagetool.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.danielnaber.languagetool.JLanguageTool;
import de.danielnaber.languagetool.rules.RuleMatch;
import de.danielnaber.languagetool.rules.patterns.PatternRule;

public final class Tools {

  private static final int DEFAULT_CONTEXT_SIZE = 25;
  
  private Tools() {
    // cannot construct, static methods only
  }

  public static int checkText(final String contents, JLanguageTool lt) throws IOException {
    return checkText(contents, lt, false, -1);
  }

  public static int checkText(final String contents, JLanguageTool lt, boolean apiFormat) throws IOException {
    return checkText(contents, lt, apiFormat, -1);
  }

  /**
   * Check the given text and print results to System.out.
   * @param contents a text to check (may be more than one sentence)
   * @param lt
   * @param apiFormat whether to print the result in a simple XML format
   * @param contextSize error text context size: -1 for default
   * @throws IOException
   */
  public static int checkText(final String contents, JLanguageTool lt, boolean apiFormat,
      int contextSize) throws IOException {
    if (contextSize == -1) {
      contextSize = DEFAULT_CONTEXT_SIZE;
    }
    long startTime = System.currentTimeMillis();
    List<RuleMatch> ruleMatches = lt.check(contents);
    if (apiFormat) {
      String xml = StringTools.ruleMatchesToXML(ruleMatches, contents, contextSize);
      System.out.print(xml);
    } else {
      int i = 1;
      for (Iterator<RuleMatch> iter = ruleMatches.iterator(); iter.hasNext();) {
        RuleMatch match = (RuleMatch) iter.next();
        String output = i + ".) Line " + (match.getLine()+1) + ", column " + match.getColumn() +
          ", Rule ID: " + match.getRule().getId();
        if (match.getRule() instanceof PatternRule) {
          PatternRule pRule = (PatternRule) match.getRule();
          output +=  "[" + pRule.getSubId() + "]";
        }
        System.out.println(output);
        String msg = match.getMessage();
        msg = msg.replaceAll("<suggestion>", "'");
        msg = msg.replaceAll("</suggestion>", "'");
        System.out.println("Message: " + msg);
        List<String> repl = match.getSuggestedReplacements();
        if (!repl.isEmpty())
          System.out.println("Suggestion: " + StringTools.listToString(repl, "; "));
        System.out.println(StringTools.getContext(match.getFromPos(), match.getToPos(), 
            contents, contextSize));
        if (iter.hasNext())
          System.out.println();
        i++;
      }
    }
    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;
    float timeInSeconds = (float)time/1000.0f;
    float sentencesPerSecond = (float)lt.getSentenceCount() / (float)timeInSeconds;
    if (apiFormat) {
      System.out.println("<!--");
    }
    System.out.printf(Locale.ENGLISH,
        "Time: %dms for %d sentences (%.1f sentences/sec)",
        time, lt.getSentenceCount(), sentencesPerSecond);
    System.out.println();
    if (apiFormat) {
      System.out.println("-->");
    }
    return ruleMatches.size();
  }

  public static InputStream getInputStream(final String resourcePath) throws IOException {
    try {
      // try the URL first:
      URL url = new URL(resourcePath);
      // success, load the resource.
      InputStream is = url.openStream();
      return is;
    } catch (MalformedURLException e) {
      // no luck. Fallback to class loader paths.
    }
    // try file path:
    File f = new File(resourcePath);
    if (f.exists() && f.isFile() && f.canRead()) {
      return new FileInputStream(f);
    } else
      throw new IOException("Could not open input stream from URL/resource/file: " + f.getAbsolutePath());
  }

  /**
   * Get a stacktrace as a string.
   */
  public static String getFullStackTrace(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }
  
  /**
   * Load a file form the classpath using getResourceAsStream().
   * @param filename
   * @return the stream of the file
   * @throws IOException if the file cannot be loaded
   */
  public static InputStream getStream(String filename) throws IOException {
    // the other ways to load the stream like "Tools.class.getClass().getResourceAsStream(filename)"
    // don't work in a web context (using Grails):
    InputStream is = Tools.class.getResourceAsStream(filename);
    if (is == null) {
      throw new IOException("Could not load file from classpath : " + filename);
    }
    return is;
  }
  
}
