//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009, 2010, 2012, 2013
//  by Roman R. Redziejowski (www.romanredz.se).
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
//-------------------------------------------------------------------------
//
//  Change log
//    090701 License changed by the author to Apache v.2.
//    090803 Modified fror Mouse 1.1.
//   Version 1.2
//    100411 Specified invocation in comment to the class.
//    100411 Added general catch for all exceptions.
//   Version 1.3
//    101203 Convert Cache name to printable in statistics.
//    101206 Removed general catch. Added catch for exceptions that can
//           indicate user error. Declared other exceptions.
//    101208 Removed undocumented possibility to comment out files
//           from file list supplied with '-F'.
//   Version 1.5.1
//    120102 (Steve Owens) Removed unused import.
//           Removed unused local variable 'errors'.
//   Version 1.6
//    130312 Added option -C.
//           Restructured the code to make TestParser class static.
//           Changed output style: file name before parser output.
//           Made options -f and -F mutually exclusive,
//           and allowed only one occurrence of each.
//           Changed range of values of -m to 1-9.
//    130416 Added option -t.
//   Version 1.6.1
//    140512 Class TestParser made public.
//
//=========================================================================

package mouse;

import mouse.runtime.ParserTest.Cache;
import mouse.runtime.Source;
import mouse.runtime.SourceFile;
import mouse.runtime.SourceString;
import mouse.utility.CommandArgs;
import mouse.utility.Convert;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Vector;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  TestParser
//
//-------------------------------------------------------------------------
//
//  Run the instrumented parser (generated with option -T),
//  and print information about its operation.
//
//  Invocation
//
//    java mouse.TestParser <arguments>
//
//  The <arguments> are specified as options according to POSIX syntax:
//
//    -P <parser>
//       Identifies the parser. Mandatory.
//       <parser> is the class name, fully qualified with package name,
//       if applicable. The class must reside in a directory corresponding
//       to the package.
//
//    -f <file>
//       Apply the parser to file <file>. Optional.
//       The <file> should include any extension.
//       Need not be a complete path, just enough to identify the file
//       in the current environment.
//
//    -F <list>
//       Apply the parser separately to each file in a list of files. Optional.
//       The <list> identifies a text file containing one fully qualified
//       file name per line.
//       The <list> itself need not be a complete path, just enough
//       to identify the file in the current environment.
//
//    -m <n>
//       Amount of memoization. Optional.
//       <n> is a digit from 1 through 9 specifying the number of results
//       to be cached. Default is no memoization.
//
//    -T <string>
//       Tracing switches. Optional.
//       The <string> is assigned to the 'trace' field in your semantics
//       object, where it can be used it to activate any trace
//       programmed there.
//       In addition, presence of certain letters in <string>
//       activates traces in the parser:
//       r - trace execution of parsing procedures for rules.
//       i - trace execution of parsing procedures for inner expressions.
//       e - trace error information.
//
//    -d Show detailed statistics for backtracking - rescan - reuse. Optional.
//
//    -D Show detailed statistics for all invoked procedures. Optional.
//
//    -C <file>
//       Write all statistics as comma separated values (CSV) to file <file>,
//       rather than to System.out. Optional; can only be specified with -F.
//       The <file> should include any extension.
//       Need not be a complete path, just enough to identify the file
//       in the current environment.
//
//    -t Show timing for -f and -F.
//
//  If you do not specify -f or -F,  the parser is executed interactively,
//  prompting for input by printing '>'.
//  It is invoked separately for each input line after you press 'Enter'.
//  You terminate the session by pressing 'Enter' directly at the prompt.
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

public class TestParser
{
  //=====================================================================
  //
  //  Data
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  Command arguments.
  //-------------------------------------------------------------------
  static CommandArgs cmd;

  //-------------------------------------------------------------------
  //  Parser class under test.
  //-------------------------------------------------------------------
  static Class<?> parserClass;
  static Method setmemo;     // Set amount of memo
  static Method settrace;    // Set trace
  static Method parse;       // Run parser
  static Method caches;      // Get list of Cache objects

  //-------------------------------------------------------------------
  //  Instantiated paser.
  //-------------------------------------------------------------------
  static Object parser;
  static Cache cacheList[];

  //-------------------------------------------------------------------
  //  Statistics switches.
  //-------------------------------------------------------------------
  static boolean details;    // -d or -D specified
  static boolean allDetails; // -D specified
  static boolean csv;        // -C specified
  static boolean timing;     // -t specified

  //-------------------------------------------------------------------
  //  CSV file.
  //-------------------------------------------------------------------
  static PrintStream csvFile;

  //-------------------------------------------------------------
  //  Computed totals.
  //-------------------------------------------------------------
  static int calls;
  static int succ;
  static int fail;
  static int back;
  static int reuse;
  static int rescan;
  static int totback;
  static int maxback;

  //-------------------------------------------------------------
  //  Execution time.
  //-------------------------------------------------------------
  static long time;

  //-------------------------------------------------------------
  //  Locale for number representation.
  //-------------------------------------------------------------
  static Locale loc = new Locale("US");


  //=====================================================================
  //
  //  Invocation
  //
  //=====================================================================

  public static void main(String argv[])
    throws IOException,IllegalAccessException,InvocationTargetException,
           InstantiationException,ClassNotFoundException,
           NoSuchMethodException
    {
      //=================================================================
      //  Get and check command arguments.
      //=================================================================
      cmd = new CommandArgs
             (argv,      // arguments to parse
              "Ddt",     // options
              "PFfmTC",  // options with argument
               0,0);     // no positional arguments
      if (cmd.nErrors()>0) return;

      //---------------------------------------------------------------
      //  Parser name.
      //---------------------------------------------------------------
      String parsName = cmd.optArg('P');

      if (parsName==null)
      {
        System.out.println("Specify -P parser name.");
        return;
      }

      //---------------------------------------------------------------
      //  The -m option.
      //---------------------------------------------------------------
      int m = 0;
      if (cmd.opt('m'))
      {
        String memo = cmd.optArg('m');
        if (memo.length()!=1) m = -1;
        else m = 1 + "123456789".indexOf(memo.charAt(0));
        if (m<1)
        {
          System.out.println("-m is outside the range 1-9.");
          return;
        }
      }

      //---------------------------------------------------------------
      //  The -T option.
      //---------------------------------------------------------------
      String trace = cmd.optArg('T');
      if (trace==null) trace = "";

      //---------------------------------------------------------------
      //  Set statistics switches.
      //---------------------------------------------------------------
      if (cmd.opt('F') & cmd.opt('f'))
      {
        System.out.println("-f and -F are mutually exclusive.");
        return;
      }

      if (cmd.opt('C') & !cmd.opt('F'))
      {
        System.out.println("-C can only be specified together with -F.");
        return;
      }

      if (cmd.opt('D') & cmd.opt('d'))
      {
        System.out.println("-d and -D are mutually exclusive.");
        return;
      }

      csv = cmd.opt('C');
      details = cmd.opt('d') | cmd.opt('D');
      allDetails = cmd.opt('D');
      timing = cmd.opt('t');

      //=================================================================
      //  Set up the parser.
      //=================================================================
      //---------------------------------------------------------------
      //  Find the parser.
      //---------------------------------------------------------------
      try{parserClass = Class.forName(parsName);}
      catch (ClassNotFoundException e)
      {
        System.out.println("Parser '" + parsName + "' not found.");
        return;
      }

      //---------------------------------------------------------------
      //  Find the 'parse' and 'setTrace' methods.
      //---------------------------------------------------------------
      parse = parserClass.getMethod("parse",Class.forName("mouse.runtime.Source"));
      settrace = parserClass.getMethod("setTrace",Class.forName("java.lang.String"));

      //---------------------------------------------------------------
      //  Find the 'setMemo' and 'caches' methods.
      //  They are present only in test version of the parser.
      //---------------------------------------------------------------
      try
      {
        setmemo = parserClass.getMethod("setMemo",int.class);
        caches  = parserClass.getMethod("caches");
      }
      catch (NoSuchMethodException e)
      {
          System.out.println(parsName + " is not a test version");
          return;
      }

      //---------------------------------------------------------------
      //  Instantiate the parser, set trace and memo, get cache list.
      //---------------------------------------------------------------
      parser = parserClass.newInstance();
      settrace.invoke(parser,trace);
      setmemo.invoke(parser,m);
      cacheList = (Cache[])caches.invoke(parser);

      //=================================================================
      //  If no input files given, run parser interactively.
      //=================================================================
      if (!cmd.opt('f') && !cmd.opt('F'))
      {
        interact();
        return;
      }

      //=================================================================
      //  If -f specified, process the file.
      //=================================================================
      if (cmd.opt('f'))
      {
        test(cmd.optArg('f'));
        return;
      }

      //=================================================================
      //  If -F specified, process files from the list.
      //=================================================================
      //---------------------------------------------------------------
      //  Get file name(s).
      //---------------------------------------------------------------
      String listName = cmd.optArg('F');
      Vector<String> files = new Vector<String>();

      BufferedReader reader;
      try {reader = new BufferedReader(new FileReader(listName));}
      catch (FileNotFoundException e)
      {
        System.out.println("File '" + listName + "' was not found");
        return;
      }

      String line = reader.readLine();
      while (line!=null)
      {
        files.add(line);
        line = reader.readLine();
      }

      if (files.size()==0)
      {
         System.out.println("No files to test.");
         return;
      }

      //---------------------------------------------------------------
      //  If -C specified, open the CSV file and write header.
      //---------------------------------------------------------------
      if (csv)
      {
        csvFile = new PrintStream(cmd.optArg('C'));
        if (timing)
          csvFile.printf("%s%n","name,size,time,calls,ok,fail,back,resc,reuse,totbk,maxbk");
        else
          csvFile.printf("%s%n","name,size,calls,ok,fail,back,resc,reuse,totbk,maxbk");
      }

      //---------------------------------------------------------------
      //  Process the files.
      //---------------------------------------------------------------
      int failed = 0;
      long t0 = System.currentTimeMillis();

      for (String name: files)
        if (!test(name))
          failed++;

      long t1 = System.currentTimeMillis();

      //---------------------------------------------------------------
      //  Write number of processed / failed files.
      //---------------------------------------------------------------
      System.out.println("\nTried " + files.size() + " files.");
      if (failed==0)
        System.out.println("All successfully parsed.");
      else
        System.out.println(failed + " failed.");

      //---------------------------------------------------------------
      //  Write total time if requested.
      //---------------------------------------------------------------
      if (timing)
        System.out.println("Total time " + (t1-t0) + " ms.");

      //---------------------------------------------------------------
      //  Close the CSV file.
      //---------------------------------------------------------------
      if (csv)
        csvFile.close();
    }


  //=====================================================================
  //
  //  Run parser on file 'name'
  //
  //=====================================================================

  static boolean test(final String name)
    throws IllegalAccessException,InvocationTargetException
    {
      Source src = new SourceFile(name);
      if (!src.created())
        return false;

      int size = src.end();
      System.out.printf("%n%s: %d bytes.%n",name,size);

      long t0 = System.currentTimeMillis();

      boolean parsed = (Boolean)(parse.invoke(parser,src));

      long t1 = System.currentTimeMillis();

      if (parsed)
      {
        compTotals();
        time = t1-t0;
        if (csv) csvTotals(name,size);
        else writeTotals();
        if (details)
          if (csv) csvDetails(allDetails);
          else writeDetails(src,allDetails);
      }
      else
      {
        System.out.println("--- failed.");
        return false;
      }

      return true;
   }


  //=====================================================================
  //
  //  Run test interactively
  //
  //=====================================================================

  static void interact()
    throws IllegalAccessException,InvocationTargetException
    {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String input;
      while (true)
      {
        System.out.print("> ");
        try
        { input = in.readLine(); }
        catch (IOException e)
        {
          System.out.println(e.toString());
          return;
        }
        if (input.length()==0) return;

        SourceString src = new SourceString(input);

        boolean parsed = (Boolean)(parse.invoke(parser,src));

        if (parsed)
        {
          compTotals();
          System.out.println("");
          writeTotals();
          if (details) writeDetails(src,allDetails);
        }
        else
          System.out.println("--- failed.");

        System.out.println("");
      }
    }


  //=====================================================================
  //
  //  Compute totals
  //
  //=====================================================================

  static void compTotals()
    {
      calls   = 0;
      succ    = 0;
      fail    = 0;
      back    = 0;
      reuse   = 0;
      rescan  = 0;
      totback = 0;
      maxback = 0;

      for (Cache s: cacheList)
      {
        calls   += s.calls;
        succ    += s.succ;
        fail    += s.fail;
        back    += s.back;
        reuse   += s.reuse;
        rescan  += s.rescan;
        totback += s.totback;
        if (s.maxback>maxback) maxback = s.maxback;
      }
    }


  //=====================================================================
  //
  //  Write totals to System.out
  //
  //=====================================================================

  static void writeTotals()
  {
    if (timing)
      System.out.printf
        ("time %d ms. %d calls: %d ok, %d failed, %d backtracked.%n",
         time,calls, succ, fail, back);
    else
      System.out.printf
        ("%d calls: %d ok, %d failed, %d backtracked.%n",
         calls, succ, fail, back);
    System.out.printf("%d rescanned", rescan);
    if (reuse==0)
      System.out.print(".\n");
    else
      System.out.printf(", %d reused.%n",reuse);
    if (back>0)
      System.out.printf
        (loc,"backtrack length: max %d, average %.1f.%n",
         maxback, (float)totback/back);
  }


  //=====================================================================
  //
  //  Write totals to CSV file
  //
  //=====================================================================

  static void csvTotals(String name, int size)
  {
    if (timing)
      csvFile.printf("\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
        name,size,time,calls,succ,fail,back,rescan,reuse,totback,maxback);
    else
      csvFile.printf("\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
        name,size,calls,succ,fail,back,rescan,reuse,totback,maxback);
  }


  //=====================================================================
  //
  //  Write details to System.out
  //
  //=====================================================================

  static void writeDetails(Source src, boolean all)
    {
      if (!all) System.out.println("\nBacktracking, rescan, reuse:");
      System.out.printf
        ("%n%-13s %5s %5s %5s %5s %5s %5s %5s %-15s%n",
         "procedure", "ok", "fail", "back", "resc", "reuse", "totbk", "maxbk", "at");
      System.out.printf
        ("%-13s %5s %5s %5s %5s %5s %5s %5s %-15s%n",
         "-------------", "-----", "-----", "-----", "-----", "-----", "-----", "-----", "--");
      for (Cache s: cacheList)
      {
        if (all || s.back!=0 || s.reuse!=0 || s.rescan!=0)
        {
          String desc = Convert.toPrint(s.name);
          if (desc.length()>13)
            desc = desc.substring(0,11) + "..";
          System.out.printf
            ("%-13s %5d %5d %5d %5d %5d",
             desc, s.succ, s.fail, s.back, s.rescan, s.reuse);
          if (s.back==0)
            System.out.printf
              (" %5d %5d%n",0,0);
          else
            System.out.printf
              (" %5d %5d %-15s%n",s.totback, s.maxback, src.where(s.maxbpos));
        }
      }
    }


  //=====================================================================
  //
  //  Write details to CSV file
  //
  //=====================================================================

  static void csvDetails(boolean all)
    {
      for (Cache s: cacheList)
      {
        if (all || s.back!=0 || s.reuse!=0 || s.rescan!=0)
        {
          String desc = Convert.toPrint(s.name).replace("\"","\"\"");
          if (timing)
            csvFile.printf("\"%s\",\"\",\"\",%d,%d,%d,%d,%d,%d,%d,%d%n",
              desc,s.calls,s.succ,s.fail,s.back,s.rescan,s.reuse,s.totback,s.maxback);
          else
            csvFile.printf("\"%s\",\"\",%d,%d,%d,%d,%d,%d,%d,%d%n",
              desc,s.calls,s.succ,s.fail,s.back,s.rescan,s.reuse,s.totback,s.maxback);
        }
      }
    }
}