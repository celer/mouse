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
//    090714 Write ok/failed message to System.out instead of err.
//    090810 Modified for Mouse 1.1.
//   Version 1.2
//    100411 Specified invocation in comment to the class.
//    100411 Added general catch for all exceptions.
//    100411 Changed structure to the same as TestParser.
//   Version 1.3
//    101206 Removed general catch. Added catch for exceptions that can
//           indicate user error. Declared other exceptions.
//    101208 Removed undocumented possibility to comment out files
//           from file list supplied with '-F'.
//   Version 1.5.1
//    120102 (Steve Owens) Removed unused import.
//   Version 1.6
//    130313 Restructured the code to make TryParser class static.
//           Changed output style: file name before parser output.
//           Made options -f and -F mutually exclusive,
//           and allowed only one occurrence of each.
//           Changed default of -m to 0.
//    130415 Added option '-t'.
//   Version 1.6.1
//    140512 Class TryParser made public.
//
//=========================================================================


package mouse;

import mouse.runtime.Source;
import mouse.runtime.SourceFile;
import mouse.runtime.SourceString;
import mouse.utility.CommandArgs;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Vector;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  TryParser
//
//-------------------------------------------------------------------------
//
//  Run the generated parser.
//
//  Invocation
//
//    java mouse.TryParser <arguments>
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
//       Applicable only to a parser generated with option -M or -T.
//       <n> is a digit from 1 through 9 specifying the number of results
//       to be cached. Default is no memoization.
//
//    -T <string>
//       Tracing switches. Optional.
//       The <string> is assigned to the 'trace' field in your semantics
//       object, where it can be used it to activate any trace
//       programmed there.
//
//    -t Show timing for -f and -F.
//
//  If you do not specify -f or -F, the parser is executed interactively,
//  prompting for input by printing '>'.
//  It is invoked separately for each input line after you press 'Enter'.
//  You terminate the session by pressing 'Enter' directly at the prompt.
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

public class TryParser
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
  static Method settrace; // Set trace switches
  static Method setmemo;  // Set amount of memo
  static Method parse;    // Run parser

  //-------------------------------------------------------------------
  //  Instantiated paser.
  //-------------------------------------------------------------------
  static Object parser;

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
              "t",       // options without argument
              "PFfmT",   // options with argument
              0,0);      // no positional arguments
      if (cmd.nErrors()>0) return;

      //---------------------------------------------------------------
      //  Get parser name.
      //---------------------------------------------------------------
      String parsName = cmd.optArg('P');

      if (parsName==null)
      {
        System.err.println("Specify -P parser name.");
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
      //  The -F and -f options.
      //---------------------------------------------------------------
      if (cmd.opt('F') & cmd.opt('f'))
      {
        System.out.println("-f and -F are mutually exclusive.");
        return;
      }

      //=================================================================
      //  Set up the parser.
      //=================================================================
      //---------------------------------------------------------------
      //  Find the parser.
      //---------------------------------------------------------------
      try{parserClass = Class.forName(parsName);}
      catch (ClassNotFoundException e)
      {
        System.err.println("Parser '" + parsName + "' not found.");
        return;
      }

      //---------------------------------------------------------------
      //  Find the 'parse' and 'setTrace' methods.
      //---------------------------------------------------------------
      parse = parserClass.getMethod("parse",Class.forName("mouse.runtime.Source"));
      settrace = parserClass.getMethod("setTrace",Class.forName("java.lang.String"));

      //---------------------------------------------------------------
      //  Find the 'setMemo' method.
      //  Set 'setmemo' to null if this is not a memoizing parser.
      //---------------------------------------------------------------
      setmemo = null;
      try {setmemo = parserClass.getMethod("setMemo",int.class);}
      catch (NoSuchMethodException e) {}

      if (m!=0 && setmemo==null)
      {
        System.out.println(parsName + " is not a memoizing parser.");
        return;
      }

      //---------------------------------------------------------------
      //  Instantiate the parser, set trace and (optionally) memo.
      //---------------------------------------------------------------
      parser = parserClass.newInstance();
      settrace.invoke(parser,trace);
      if (setmemo!=null) setmemo.invoke(parser,m);

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
        if (test(cmd.optArg('f')))
          System.out.println("--- ok.");
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
      if (cmd.opt('t'))
        System.out.println("Total time " + (t1-t0) + " ms.");
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

      System.out.println("\n" + name);

      long t0 = System.currentTimeMillis();

      boolean parsed = (Boolean)(parse.invoke(parser,src));

      long t1 = System.currentTimeMillis();

      if (!parsed)
      {
        System.out.println("--- failed.");
        return false;
      }

      if (cmd.opt('t'))
        System.out.println("--- " + (t1-t0) + " ms.");

      return true;
    }


  //=====================================================================
  //
  //  Run parser interactively
  //
  //=====================================================================

  static void interact()
    throws IOException,IllegalAccessException,InvocationTargetException
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
      }
    }
}