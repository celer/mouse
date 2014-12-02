//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009 by Roman R. Redziejowski (www.romanredz.se).
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
//   Version 1.2
//    091103 Corrected bug: check that options argument is not empty
//           before looking at its first charater.
//
//=========================================================================

package mouse.utility;

import java.util.Vector;

//=======================================================================
/**
*  An object-oriented counterpart of C procedure 'getopt'.
*  <br>
*  An object of class CommandArgs represents the argument list supplied
*  as the 'argv' parameter to 'main'. It is constructed by parsing
*  the argument list according to instructions supplied as arguments
*  to the constructor. The options and arguments can be then obtained
*  by invoking methods on the object thus constructed.
*  The argument list is supposed to follow POSIX conventions
*  (IEEE Standard 1003.1, 2004 Edition, Chapter 12).
*/
//=======================================================================

public class CommandArgs
{
  //-------------------------------------------------------------------
  //  Option letters in order of appearance.
  //  Note that options with argument may have multiple occurrences.
  //-------------------------------------------------------------------
  private String letters;

  //-------------------------------------------------------------------
  //  Arguments specified with letters.
  //  Null for argument-less options.
  //-------------------------------------------------------------------
  private Vector<String> optArgs = new Vector<String>();

  //-------------------------------------------------------------------
  //  Positional arguments.
  //-------------------------------------------------------------------
  private Vector<String> args = new Vector<String>();

  //-------------------------------------------------------------------
  //  Error count.
  //-------------------------------------------------------------------
  private int errors = 0;

  //-------------------------------------------------------------------
  /** Construct CommandArgs object from an argument list 'argv'.
  *   <br>
  *   @param  argv Argument list, as passed to the program.
  *   @param  options String consisting of option letters for options without argument.
  *   @param  optionsWithArg String consisting of option letters for options with argument.
  *   @param  minargs Minimum number of arguments.
  *   @param  maxargs Maximum number of arguments.
  */
  //-------------------------------------------------------------------
  public CommandArgs
    ( final String[] argv,
      final String options,
      final String optionsWithArg,
      int minargs,
      int maxargs)
    {
      int i = 0;
      StringBuffer opts = new StringBuffer();

      //---------------------------------------------------------------
      //  Examine elements of argv as long as they specify options.
      //---------------------------------------------------------------
      while(i<argv.length)
      {
        String elem = argv[i];

        //-------------------------------------------------------------
        //  Any element that does not start with '-' terminates options
        //-------------------------------------------------------------
        if (elem.isEmpty() || elem.charAt(0)!='-')
          break;

        //-------------------------------------------------------------
        //  A single '-' is a positional argument and terminates options.
        //-------------------------------------------------------------
        if (elem.equals("-"))
        {
          args.addElement("-");
          i++;
          break;
        }

        //-------------------------------------------------------------
        //  A  '--' is not an argument and terminates options.
        //-------------------------------------------------------------
        if (elem.equals("--"))
        {
          i++;
          break;
        }

        //-------------------------------------------------------------
        //  An option found - get option letter.
        //-------------------------------------------------------------
        String c = elem.substring(1,2);

        if (optionsWithArg.indexOf(c)>=0)
        {
          //-----------------------------------------------------------
          //  Option with argument
          //-----------------------------------------------------------
          opts.append(c);
          if (elem.length()>2)
          {
            // option's argument in the same element
            optArgs.addElement(elem.substring(2,elem.length()));
            i++;
          }

          else
          {
            // option's argument in next element
            i++;
            if (i<argv.length && (argv[i].length()==0 || argv[i].charAt(0)!='-'))
            {
              optArgs.addElement(argv[i]);
              i++;
            }
            else
            {
              System.err.println("Missing argument of option -" + c + ".");
              optArgs.addElement(null);
              errors++;
            }
          }
        }

        else
        {
          //-----------------------------------------------------------
          //  Option without argument or invalid.
          //  The element may specify more options.
          //-----------------------------------------------------------
          for (int n=1;n<elem.length();n++)
          {
            c = elem.substring(n,n+1);
            if (options.indexOf(c)>=0)
            {
              opts.append(c);
              optArgs.addElement(null);
            }
            else
            {
              System.err.println("Unrecognized option -" + c + ".");
              errors++;
              break;
            }
          }
          i++;
        }
      }

      letters = opts.toString();

      //---------------------------------------------------------------
      //  The remaining elements of argv are positional arguments.
      //---------------------------------------------------------------
      while(i<argv.length)
      {
        args.addElement(argv[i]);
        i++;
      }

      if (nArgs()<minargs)
      {
        System.err.println("Missing argument(s).");
        errors++;
      }

      if (nArgs()>maxargs)
      {
        System.err.println("Too many arguments.");
        errors++;
      }
    }

  //-------------------------------------------------------------------
  //  Access to options
  //-------------------------------------------------------------------
  /**
  *  Checks if a given option was specified.
  *
  *  @param  c Option letter.
  *  @return true if the option is specified, false otherwise.
  */
  public boolean opt(char c)
    { return letters.indexOf(c)>=0; }

  /**
  *  Gets argument of a given option.
  *  Returns null if the option is not specified or does not have argument.
  *  If option was specified several times, returns the first occurrence.
  *
  *  @param  c Option letter.
  *  @return value of the i-th option.
  */
  public String optArg(char c)
    {
      int i = letters.indexOf(c);
      return i<0? null : optArgs.elementAt(i);
    }

  /**
  *  Gets arguments of a given option.
  *  Returns a vector of arguments for an option specified repeatedly-
  *  Returns empty vector if the option is not specified or does not have argument.
  *
  *  @param  c Option letter.
  *  @return value of the i-th option.
  */
  public Vector<String> optArgs(char c)
    {
      Vector<String> result = new Vector<String>();
      for (int i=0;i<letters.length();i++)
        if (letters.charAt(i)==c)
          result.add(optArgs.elementAt(i));
      return result;
    }

  //-------------------------------------------------------------------
  //  Access to positional arguments
  //-------------------------------------------------------------------
  /**
  *  Gets the number of arguments in the argument list.
  *
  *  @return Number of arguments.
  */
  public int nArgs()
    { return args.size(); }

  /**
  *  Gets the i-th argument.
  *
  *  @param  i Argument number (<code>0&le;i&lt;nOpts()</code>).
  *  @return the i-th argument.
  */
  public String arg(int i)
    { return args.elementAt(i); }

  /**
  *  Gets the argument vector.
  *
  *  @return Vector<String> of arguments.
  */
  public Vector<String> args()
    { return args; }

  //-------------------------------------------------------------------
  //  Error count
  //-------------------------------------------------------------------
  /**
  *  Gets number of errors detected when parsing the argument list.
  *
  *  @return Number of errors.
  */
  public int nErrors()
    { return errors; }
}