//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009, 2010 by Roman R. Redziejowski (www.romanredz.se).
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
//    100405 Removed printing of iteration count.
//    100411 Specified invocation in comment to the class.
//   Version 1.3
//    101109 Use new version of PEG class.
//   Version 1.6.1
//    140512 Class TestPEG made public.
//
//=========================================================================

package mouse;

import mouse.runtime.SourceFile;
import mouse.utility.CommandArgs;
import mouse.peg.PEG;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  TestPEG
//
//-------------------------------------------------------------------------
//
//  Check the grammar without generating parser.
//
//  Invocation
//
//    java mouse.TestPEG <arguments>
//
//  The <arguments> are specified as options according to POSIX syntax:
//
//    -G <filename>
//       Identifies the file containing the grammar. Mandatory.
//       The <filename> need not be a complete path, just enough to identify
//       the file in current environment. Should include file extension,if any.
//
//    -D Display the grammar. Optional.
//       Shows the rules and subexpressions together with their attributes
//       according to Ford.
//
//    -C Display the grammar in compact form: without duplicate subexpressions.
//       Optional.
//
//    -R Display only the rules. Optional.
//
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

public class TestPEG
{
  //=====================================================================
  //
  //  Invocation
  //
  //=====================================================================

  public static void main(String argv[])
    {
      //---------------------------------------------------------------
      //  Parse arguments.
      //---------------------------------------------------------------
      CommandArgs cmd = new CommandArgs
             (argv,      // arguments to parse
              "CDR",     // options without argument
              "G",       // options with argument
               0,0);     // no positional arguments
      if (cmd.nErrors()>0) return;

      String gramName = cmd.optArg('G');
      if (gramName==null)
      {
        System.err.println("Specify -G grammar file.");
        return;
      }

      SourceFile src = new SourceFile(gramName);
      if (!src.created()) return;

      //---------------------------------------------------------------
      //  Create PEG object from source file.
      //---------------------------------------------------------------
      PEG peg = new PEG(src);
      if (peg.errors>0) return;

      // System.out.println(peg.iterAt + " iterations for attributes.");
      // System.out.println(peg.iterWF + " iterations for well-formed.");

      if (peg.notWF==0)
        System.out.println("The grammar is well-formed.");

      //---------------------------------------------------------------
      //  Display as requested.
      //---------------------------------------------------------------
      if (cmd.opt('C'))
      {
        peg.compact();
        peg.showAll();
      }
      else if (cmd.opt('D')) peg.showAll();
      else if (cmd.opt('R')) peg.showRules();
    }
}