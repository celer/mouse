//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009, 2010, 2011, 2012
//  by Roman R. Redziejowski (www.romanredz.se).
//
//	Modifications by David S. Tyree (github.com/celer/)
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
//   Version 1.6.1
//    141201 Copied from Java 1.6.1 Generator and made a golang version. (dtyree)
//
//=========================================================================

package mouse;

import mouse.peg.PEG;
import mouse.peg.Expr;
import mouse.peg.Action;
import mouse.runtime.SourceFile;
import mouse.utility.CommandArgs;
import mouse.utility.Convert;
import mouse.utility.LineWriter;
import java.io.File;
import java.util.Date;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;
import java.text.SimpleDateFormat;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Generate
//
//-------------------------------------------------------------------------
//
//  Generate parser from Parsing Expression Grammar.
//  Optionally, generate skeleton for the corresponding semantics class.
//
//  Invocation
//
//    java mouse.Generate <arguments>
//
//  The <arguments> are specified as options according to POSIX syntax:
//
//    -G <filename>
//       Identifies the file containing the grammar. Mandatory.
//       The <filename> need not be a complete path, just enough to identify
//       the file in current environment. Should include file extension,if any.
//
//    -D <directory>
//       Identifies target directory to receive the generated file(s).
//       Optional. If omitted, files are generated in current work directory.
//       The <directory> need not be a complete path, just enough to identify
//       the directory in current environment. The directory must exist.
//
//    -P <parser>
//       Specifies name of the parser to be generated. Mandatory.
//       Must be an unqualified class name.
//       The tool generates a file named "<parser>.java" in target directory.
//       The file contains definition of Java class <parser>.
//       If target directory already contains a file "<parser<.java",
//       the file is replaced without a warning,
//
//    -S <semantics>
//       Indicates that semantic actions are methods in the Java class <semantics>.
//       Mandatory if the grammar specifies semantic actions.
//       Must be an unqualified class name.
//
//    -p <package>
//       Generate parser as member of package <package>.
//       The semantics class, if specified, is assumed to belong to the same package.
//       Optional. If not specified, both classes belong to unnamed package.
//       The specified package need not correspond to the target directory.
//
//    -r <import-package>
//       Specify the package to import which will be named .mouse
//
//    -s Generate skeleton of semantics class. Optional.
//       The skeleton is generated as file "<semantics>.java" in target directory,
//       where <semantics> is the name specified by -S  option.
//       The option is ignored if -S is not specified.
//       If target directory already contains a file "<semantics>.java",
//       the tool is not executed.
//
//    -M Generate memoizing version of the parser.
//
//    -T Generate instrumented ('test') version of the parser.
//
//       (Options -M and -T are mutually exclusive.)
//
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

public class GoGenerate
{
  //=====================================================================
  //
  //  Data
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  Input
  //-------------------------------------------------------------------
  String gramName;   // Grammar file name
  String gramPath;   // Full path to grammar file
  String parsName;   // Parser name
  String semName;    // Semantics name
  String dirName;    // Output directory
  String packName;   // Package name
  String runName;    // Runtime package name
  boolean memo;      // Generate memo version?
  boolean test;      // Generate test version?
  boolean skel;      // Generate semantics skeleton?

  //-------------------------------------------------------------------
  //  Output.
  //-------------------------------------------------------------------
  LineWriter out;

  //-------------------------------------------------------------------
  //  Parsed grammar.
  //-------------------------------------------------------------------
  PEG peg;

  //-------------------------------------------------------------------
  //  Date stamp.
  //-------------------------------------------------------------------
  String date;

  //-------------------------------------------------------------------
  //  Cache name (or nothing) to be generated.
  //-------------------------------------------------------------------
  String cache = "";

  //-------------------------------------------------------------------
  //  Visitors.
  //-------------------------------------------------------------------
  ProcVisitor procVisitor = new ProcVisitor();
  RefVisitor  refVisitor  = new RefVisitor();
  InliVisitor inliVisitor = new InliVisitor();
  TermVisitor termVisitor = new TermVisitor();

  //-------------------------------------------------------------------
  //  The subexpressions that will have procedures.
  //  They have names consisting of the name of the containing Rule,
  //  followed by underscore and number within the Rule.
  //  Their procedures are created immediately after that for the Rule
  //  by procedure 'createSubs'; 'done' counts the elements of 'subs'
  //  that already have procedures created.
  //-------------------------------------------------------------------
  Vector<Expr> subs = new Vector<Expr>();
  String exprName;   // Name of containing Rule
  String procName;   // Name of procedure being generated
  int exprNum;       // Number within containing Rule
  int done = 0;      // Count of created procedures


  //=====================================================================
  //
  //  Invocation
  //
  //=====================================================================

  public static void main(String argv[])
    {
      GoGenerate gen = new GoGenerate();
      gen.run(argv);
    }


  //=====================================================================
  //
  //  Do the job
  //
  //=====================================================================

  void run(String argv[])
    {
      boolean errors = false;
      //---------------------------------------------------------------
      //  Parse arguments.
      //---------------------------------------------------------------
      CommandArgs cmd = new CommandArgs
             (argv,         // arguments to parse
              "MTs",        // options without argument
              "GPSDpr",     // options with argument
               0,0);        // no positional arguments
      if (cmd.nErrors()>0) return;

      //---------------------------------------------------------------
      //  Get options.
      //---------------------------------------------------------------
      gramName = cmd.optArg('G');
      parsName = cmd.optArg('P');
      semName  = cmd.optArg('S');
      dirName  = cmd.optArg('D');
      packName = cmd.optArg('p');
      runName  = cmd.optArg('r');
      test = cmd.opt('T');
      memo = cmd.opt('M');
      skel = cmd.opt('s');

      if (gramName==null)
      {
        System.err.println("Specify -G grammar name.");
        errors = true;
      }

      if (parsName==null)
      {
        System.err.println("Specify -P parser class name.");
        errors = true;
      }

     if (runName==null)
        runName = "github.com/celer/gomouse";

     if (semName==null)
      {
        semName = "mouse.SemanticsBase";
        if (skel)
        {
          skel = false;
          System.err.println("Option -s ignored because -S not specified.");
        }
      }

      if (dirName==null)
        dirName = "";
      else
        dirName = dirName + File.separator;

      if (skel)
      {
        File f = new File(dirName + semName + ".go");
        if (f.exists())
        {
          System.err.println("File '" + dirName + semName + ".go' already exists.");
          System.err.println("Remove the file or the -s option.");
          errors = true;
        }
        f = null;
      }

      if (memo & test)
      {
        System.err.println("Options -M and -T are mutually exclusive.");
        errors = true;
      }

      if (errors) return;

      //---------------------------------------------------------------
      //  Parse the grammar and eliminate duplicate expressions.
      //---------------------------------------------------------------
      SourceFile src = new SourceFile(gramName);
      if (!src.created()) return;
      peg = new PEG(src);
      if (peg.errors>0) return;
      if (peg.notWF>0) return;
      peg.compact();

      //---------------------------------------------------------------
      //  Get full path to grammar file, ready to include in comment.
      //---------------------------------------------------------------
      gramPath = Convert.toComment(src.file().getAbsolutePath());

      //---------------------------------------------------------------
      //  Get date stamp.
      //---------------------------------------------------------------
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      df.setTimeZone(TimeZone.getTimeZone("GMT"));
      date = df.format(new Date());

      //---------------------------------------------------------------
      //  Generate parser.
      //---------------------------------------------------------------
      generate();

      //---------------------------------------------------------------
      //  If requested, generate semantics skeleton.
      //---------------------------------------------------------------
      if (skel) genSkel();
    }


  //=====================================================================
  //
  //  Generate the parser
  //
  //=====================================================================

  void generate()
    {
      //---------------------------------------------------------------
      //  Set up output.
      //---------------------------------------------------------------
      out = new LineWriter(dirName + parsName + ".go");
			out.setIndentString("\t");

      //---------------------------------------------------------------
      //  Assign names to terminals.
      //---------------------------------------------------------------
      for (int i=0;i<peg.terms.length;i++)
        peg.terms[i].name = "$Term" + i;


      //---------------------------------------------------------------
      //  Create header.
      //---------------------------------------------------------------
      String    basePars = "mouse.ParserBase";
      if (memo) basePars = "mouse.ParserMemo";
      if (test) basePars = "mouse.ParserTest";
			

      out.BOX("This file was generated by Mouse 1.6.1 at " +
               date + " GMT\nfrom grammar '" + gramPath + "'.");
      out.line("");

      if ( packName!=null)
      {
        out.line("package " + packName + ";");
        out.line("");
      } else {
				out.line("package main;");
			}
			out.line("");

			out.line("import mouse \""+runName+"\";");


      out.line("");
			out.line("type "+parsName+" struct {");
			out.indent();
			out.line("sem *"+semName+";");
			out.line(basePars);
			out.undent();
			out.line("}");	

      out.BOX("Initialization");

      out.box("Constructor");
      out.line("func New" + parsName + "() *"+parsName+" {");
			out.indent();
			out.line("this:=" + parsName + "{};");
      out.line("this.sem = New" + semName + "();");
      out.line("this.sem.CurrentRule = &this;");
      //out.line("    this.super.sem = sem;");
      if (memo | test)
        out.line("this.Caches = cacheList;");
			out.line("return &this;");
			out.undent();
      out.line("}");
      out.line("");

      out.box("Run the parser");
      out.line("func (this *"+parsName+") Parse(src mouse.Source) (bool,error) {");
			out.indent();
      out.line("this.Init(src);");
      out.line("this.sem.Init();");
      out.line("if (this." + peg.rules[0].name + "()){ return true,nil; }");
      out.line("return this.Failure();");
			out.undent();
      out.line("}");
      out.line("");

      out.box("Get semantics");
      out.line("func (this *"+parsName+") Semantics() *"+semName+" {");
			out.indent();
      out.line("return this.sem;");
			out.undent();
			out.line("}");
      out.line("");

      out.BOX("Parsing procedures");

      //---------------------------------------------------------------
      //  Create parsing procedures for Rules.
      //---------------------------------------------------------------
      for (Expr.Rule rule: peg.rules)
      {
        exprName = rule.name;
        procName = rule.name;
        exprNum = 0;

        out.Box(Convert.toComment(rule.asString()));
        out.line("func (this *"+parsName+") " + rule.name + "() bool {");
        out.indent();
        if (memo | test)
        {
          out.line("if (this.Saved(" + rule.name + ")){ return this.Reuse(); }");
          if (test) cache = rule.name;
        }
        else if (rule.diagName==null)
          out.line("this.Begin(\"" + rule.name + "\");");
        else
          out.line("this.BeginWithDiag(\"" + rule.name + "\",\""
                   + Convert.toStringLit(rule.diagName) + "\");");

        //-------------------------------------------------------------
        //  Special case: single expression on right-hand side
        //  and no 'onFail' action.
        //-------------------------------------------------------------
        if ( rule.rhs.length==1 && rule.onFail[0]==null)
        {
          Expr e = rule.rhs[0];
          Action act = rule.onSucc[0];
          inline(e,"this.Reject(" + cache + ")");
          if (act==null)
            out.line("return this.Accept(" + cache + ");");
          else if (act.and)
          {
            out.line("if (this.sem." + act.name + "()){ return this.Accept(" + cache + "); }");
            out.line("this.BoolReject();");
            out.line("return this.Reject(" + cache + ");");
          }
          else
          {
            out.line("this.sem." + act.name + "();");
            out.line("return this.Accept(" + cache + ");");
          }
        }

        //-------------------------------------------------------------
        //  General case.
        //-------------------------------------------------------------
        else
        {
          for (int i=0;i<rule.rhs.length;i++)
          {
            Action succ = rule.onSucc[i];
            Action fail = rule.onFail[i];

            if (succ==null)
              out.line("if (" + ref(rule.rhs[i]) + "){ return this.Accept(" + cache + "); ");
            else if (succ.and)
            {
              out.line("if (" + ref(rule.rhs[i]) + " && "
                       + "(this.sem." + succ.name + "()?true:this.BoolReject())){ return this.Accept(" + cache + "); ");
            }
            else
            {
              out.line("if (" + ref(rule.rhs[i]) + "){ this.sem." + succ.name + "(); return this.Accept(" + cache + "); ");
            }

            if (fail!=null)
              out.line("} else { this.sem." + fail.name + "(); }");
						else
							out.line("}");

          }

          out.line("return this.Reject(" + cache + ");");

        }

        out.undent();
        out.line("}");
        out.line("");

        createSubs();
      }

      //---------------------------------------------------------------
      //  If memo or test version:
      //  create Cache objects for rules and inner.
      //---------------------------------------------------------------
      if (memo | test)
      {
        out.BOX("Cache objects");

        out.line("");

        for (Expr.Rule rule: peg.rules)
          out.line("var "+rule.name+" *mouse.Cache  = mouse.NewCache(\""
                    + rule.name + "\",\""
                    + Convert.toStringLit(diagName(rule)) + "\");") ;

        out.line("");

        for (Expr expr: subs)
          if (isPred(expr))
          {
            out.line("var "+expr.name+" *mouse.Cache  = mouse.NewCache(\""
                      + expr.name + "\",\""
                      + Convert.toStringLit(diagPred(expr)) + "\"); // "
                      + Convert.toComment(expr.asString()) );
          }
          else
          {
            out.line("var "+expr.name+" *mouse.Cache = mouse.NewCache(\""
                      + Convert.toStringLit(expr.name) + "\",\"\"); // "
                      + Convert.toComment(expr.asString()) );

          }
      }

      //---------------------------------------------------------------
      //  If test version:
      //  create Cache objects for terminals.
      //---------------------------------------------------------------
      if (test)
      {
        out.line("");

        for (Expr expr: peg.terms)
          out.line("var "+expr.name+" *mouse.Cache = mouse.NewCache(\""
                    + Convert.toStringLit(expr.asString()) + "\",\"\");") ;
      }

      //---------------------------------------------------------------
      //  Create Expression array for memo / test version.
      //---------------------------------------------------------------
      if (memo | test)
      {
        Vector<Expr> temp = new Vector<Expr>();
        for (Expr.Rule r: peg.rules)
          temp.add(r);
        temp.addAll(subs);

        if (test)
          for (Expr t: peg.terms)
            temp.add(t);

        out.line("");
        out.box("List of Cache objects");
        out.line("");

        StringBuilder sb = new StringBuilder();
        sb.append("var cacheList []*mouse.Cache=");
				sb.append("[]*mouse.Cache{");
        for (Expr expr: temp)
        {
          String name = expr.name;
          sb.append(name + ",");
        }
        sb.deleteCharAt(sb.length()-1);
      	sb.append("}");
			  out.line(sb.toString());
      }

      //---------------------------------------------------------------
      //  Terminate the parser and close output.
      //---------------------------------------------------------------
      out.undent();
      out.close();

      System.out.println(peg.rules.length + " rules");
      System.out.println(subs.size()  + " unnamed");
      System.out.println(peg.terms.length + " terminals");

    }


  //=====================================================================
  //
  //  Generate semantics skeleton
  //
  //=====================================================================

  void genSkel()
    {
      //---------------------------------------------------------------
      //  Set up output.
      //---------------------------------------------------------------
      out = new LineWriter(dirName + semName + ".go");


      //---------------------------------------------------------------
      //  Create header.
      //---------------------------------------------------------------
      out.BOX("This skeleton was generated by Mouse 1.6.1 at " + date + " GMT\n" +
               "from grammar '" + gramPath + "'.");
      out.line("");

      if ( packName!=null)
      {
        out.line("package " + packName + ";");
        out.line("");
      } else {
        out.line("package main;");
        out.line("");
			}

			out.line("");
			out.line("import mouse \""+runName+"\";");
			out.line("");

			out.line("type "+semName+" struct {");
			out.indent();
			out.line("mouse.SemanticsBase");
			out.undent();
			out.line("}");
      //out.line("class " + semName + " extends " + runName + ".SemanticsBase");
      //out.line("{");

			out.line("func New"+semName+"() *"+semName+"{");
			out.indent();
			out.line("s:="+semName+"{}");
			out.line("return &s;");
			out.undent();
			out.line("}");
      //---------------------------------------------------------------
      //  Collect Actions specified in the grammar.
      //---------------------------------------------------------------
      Vector<Action> actions  = new Vector<Action>();
      Hashtable<String,String> comments = new Hashtable<String,String>();

      for (Expr.Rule rule: peg.rules)
      {
        for (int i=0;i<rule.rhs.length;i++)
        {
          if (rule.onSucc[i]!=null)
          {
            Action act = rule.onSucc[i];
            String comment = rule.name + " = " + Convert.toComment(rule.rhs[i].asString());
            String found = comments.get(act.name);
            if (found==null)
            {
              actions.add(act);
              comments.put(act.name,comment);
            }

            else
              comments.put(act.name,found + "\n" + comment);
          }

          if (rule.onFail[i]!=null)
          {
            Action act = rule.onFail[i];
            String comment = "failed " + rule.name + " = " + Convert.toComment(rule.rhs[i].asString());
            String found = comments.get(act.name);
            if (found==null)
            {
              actions.add(act);
              comments.put(act.name,comment);
            }

            else
              comments.put(act.name,found + "\n" + comment);
          }
        }
      }

      //---------------------------------------------------------------
      //  Create semantic procedures.
      //---------------------------------------------------------------
      for (int i=0;i<actions.size();i++)
      {
        Action act = actions.elementAt(i);
        out.box(comments.get(act.name));
        out.line("func (this *"+semName+") "+act.name+(act.and? "() bool " : "() ") + "{");
        out.line((act.and? " return true; ":""));
				out.line("}");
        out.line("");
      }

      //---------------------------------------------------------------
      //  Terminate the class and close output.
      //---------------------------------------------------------------
      out.close();

      System.out.println(actions.size() + " semantic procedures");
    }




  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  RefVisitor
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  //-------------------------------------------------------------------
  //  This procedure returns the string to be generated as invocation
  //  of 'expr'. Note that 'expr' is never an Expr.Rule, as Rules are
  //  always referenced via Expr.Ref objects.
  //  The invocation string is obtained by using RefVisitor to visit
  //  'expr'. The Visitor keeps track of visited objects in 'subs' and
  //  'temps', and generates for them names that are stored in 'names'.
  //-------------------------------------------------------------------
  String ref(Expr expr)
    {
      expr.accept(refVisitor);
      return refVisitor.result;
    }

  class RefVisitor extends mouse.peg.Visitor
  {
    //-----------------------------------------------------------------
    //  Result from Visitor
    //  Note that the Visitor is never called recursively.
    //-----------------------------------------------------------------
    String result;

    public void visit(Expr.Rule expr)
      {}

    public void visit(Expr.Choice expr)
      { doExpr(expr); }

    public void visit(Expr.Sequence expr)
      { doExpr(expr); }

    public void visit(Expr.And expr)
      { doExpr(expr); }

    public void visit(Expr.Not expr)
      { doExpr(expr); }

    public void visit(Expr.Plus expr)
      { doExpr(expr); }

    public void visit(Expr.Star expr)
      { doExpr(expr); }

    public void visit(Expr.Query expr)
      { doExpr(expr); }

    public void visit(Expr.PlusPlus expr)
      { doExpr(expr); }

    public void visit(Expr.StarPlus expr)
      { doExpr(expr); }

    public void visit(Expr.Ref expr)
      { result = "this."+expr.name + "()"; }

    public void visit(Expr.StringLit expr)
      { doTerm(expr); }

    public void visit(Expr.CharClass expr)
      { doTerm(expr); }

    public void visit(Expr.Range expr)
      { doTerm(expr); }

    public void visit(Expr.Any expr)
      { doTerm(expr); }

    private void doExpr(Expr expr)
      {
        String name = expr.name;

        if (name==null)
        {
          name = exprName + "_" + exprNum;
          exprNum++;
          expr.name = name;
          subs.add(expr);
        }

        result = "this."+name + "()";
      }

    private void doTerm(Expr expr)
      { result = "this.Next" + termCall(expr); }

  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  ProcVisitor - visitor to generate body of parsing procedure
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  class ProcVisitor extends mouse.peg.Visitor
  {
    public void visit(Expr.Rule expr)
      {throw new Error("SNOC" + expr.name); }

    public void visit(Expr.Choice expr)
      {
        for (Expr e: expr.expr)
          out.line("if (" + ref(e) + ") { return this.AcceptInner(" + cache + "); }");
        out.line("return this.RejectInner(" + cache + ");");
      }

    public void visit(Expr.Sequence expr)
      {
        for (Expr e: expr.expr)
          inline(e,"this.RejectInner(" + cache + ")");
        out.line("return this.AcceptInner(" + cache + ");");
      }

    public void visit(Expr.And expr)
      {
        out.line("if (!" + ref(expr.expr) + "){ return this.RejectAnd(" + cache + "); }");
        out.line("return this.AcceptAnd(" + cache + ");");
      }

    public void visit(Expr.Not expr)
      {
        out.line("if (" + ref(expr.expr) + "){ return this.RejectNot(" + cache + ");}");
        out.line("return this.AcceptNot(" + cache + ");");
      }

    public void visit(Expr.Plus expr)
      {
        out.line("if (!" + ref(expr.expr) + "){ return this.RejectInner(" + cache + "); }");
        out.line("for " + ref(expr.expr) + " {};");
        out.line("return this.AcceptInner(" + cache + ");");
      }

    public void visit(Expr.Star expr)
      {
        out.line("for " + ref(expr.expr) + " {};");
        out.line("return this.AcceptInner(" + cache + ");");
      }

    public void visit(Expr.Query expr)
      {
        out.line(ref(expr.expr) + ";");
        out.line("return this.AcceptInner(" + cache + ");");
      }

    public void visit(Expr.PlusPlus expr)
      {
        out.line("if (" + ref(expr.expr2) + "){ return this.RejectInner(" + cache + "); }");
        out.line("if (!" + ref(expr.expr1) + "){ return this.RejectInner(" + cache + "); }");
        out.line("for !" + ref(expr.expr2) + " {");
				out.indent();
        out.line("if (!" + ref(expr.expr1) + "){ return this.RejectInner(" + cache + "); }");
				out.undent();
				out.line("}");
        out.line("return this.AcceptInner(" + cache + ");");
      }

    public void visit(Expr.StarPlus expr)
      {
        out.line("for !" + ref(expr.expr2) + " {");
				out.indent();
        out.line("if (!" + ref(expr.expr1) + "){ return this.RejectInner(" + cache + "); }");
				out.undent();
				out.line("}");
        out.line("return this.AcceptInner(" + cache + ");");
      }

    public void visit(Expr.Ref expr)
      {
        if (!expr.fal)
          out.line("this."+expr.name + "();");
        else
          out.line("if (!" + expr.name + "()) { return this.RejectInner(" + cache + "); }");
        out.line("return this.AcceptInner(" + cache + ");");
      }

    public void visit(Expr.StringLit expr)
      { doTerm(expr); }

    public void visit(Expr.CharClass expr)
      { doTerm(expr); }

    public void visit(Expr.Range expr)
      { doTerm(expr); }

    public void visit(Expr.Any expr)
      { doTerm(expr); }

    private void doTerm(Expr expr)
      {
        out.line("if (!" + ref(expr)+ "){ return this.RejectInner(" + cache + "); }");
        out.line("return this.AcceptInner(" + cache + ");");
      }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  InliVisitor - visitor to generate inline procedure
  //
  //  (Inline procedure falls through on success
  //   or returns reject() on failure.)
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  void inline(Expr expr, String rej)
    {
      reject = rej;
      expr.accept(inliVisitor);
    }

  String reject;

  class InliVisitor extends mouse.peg.Visitor
  {
    public void visit(Expr.Rule expr)
      {throw new Error("SNOC" + expr.name); }

    public void visit(Expr.Choice expr)
      {
        Expr e = expr.expr[0];
				StringBuffer sb = new StringBuffer();
        sb.append("if (!" + ref(e));
        for (int i=1;i<expr.expr.length;i++)
        {
          e = expr.expr[i];
          sb.append(" && !" + ref(e));
        }
        sb.append("   ){ return " + reject + "; }");
				out.line(sb.toString());
      }

    public void visit(Expr.Sequence expr)
      {
        for (Expr e: expr.expr)
          e.accept(inliVisitor);
      }

    public void visit(Expr.And expr)
      {
        Expr e = expr.expr;
        if (isTerm(e))
          out.line("if (!this.Ahead" + termCall(e) + "){ return " + reject + "; }");
        else
          out.line("if (!" + ref(expr) + "){ return " + reject + "; }");
      }

    public void visit(Expr.Not expr)
      {
        Expr e = expr.expr;
        if (isTerm(e))
          out.line("if (!this.AheadNot" + termCall(e) + "){ return " + reject + "; }");
        else
          out.line("if (!" + ref(expr) + "){ return " + reject + "; }");
      }

    public void visit(Expr.Plus expr)
      {
        out.line("if (!" + ref(expr.expr) + "){ return " + reject + "; }");
        out.line("for " + ref(expr.expr) + " {};");
      }

    public void visit(Expr.Star expr)
      { out.line("for " + ref(expr.expr) + " {};"); }

    public void visit(Expr.Query expr)
      { out.line(ref(expr.expr) + ";"); }

    public void visit(Expr.PlusPlus expr)
      {
        out.line("if (" + ref(expr.expr2) + ") { return " + reject + "; }");
        out.line("if (!" + ref(expr.expr1) + "){ return " + reject + "; }");
        out.line("for !" + ref(expr.expr2) + " {");
        out.indent();
				out.line("if (!" + ref(expr.expr1) + "){ return " + reject + "; }");
				out.undent();
				out.line("}");
      }

    public void visit(Expr.StarPlus expr)
      {
        out.line("for !" + ref(expr.expr2) + " {");
				out.indent();
        out.line("if (!" + ref(expr.expr1) + "){ return " + reject + "; }");
				out.undent();
				out.line("}");
      }

    public void visit(Expr.Ref expr)
      {
        if (!expr.fal)
          out.line("this."+expr.name + "();");
        else
          out.line("if (!this." + expr.name + "()) { return " + reject + "; }");
      }

    public void visit(Expr.StringLit expr)
      { doTerm(expr); }

    public void visit(Expr.CharClass expr)
      { doTerm(expr); }

    public void visit(Expr.Range expr)
      { doTerm(expr); }

    public void visit(Expr.Any expr)
      { doTerm(expr); }

    private void doTerm(Expr expr)
      { out.line("if (!" + ref(expr)+ "){ return " + reject + "; }"); }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  TermVisitor
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  //-------------------------------------------------------------------
  //  This procedure returns kernel of a call to terminal processing.
  //-------------------------------------------------------------------
  String termCall(Expr expr)
    {
      termVisitor.cash = test? (expr.name) : "";
      termVisitor.ccash = test? ("," + expr.name) : "";
      expr.accept(termVisitor);
      return termVisitor.result;
    }

  class TermVisitor extends mouse.peg.Visitor
  {
    //-----------------------------------------------------------------
    //  Result from Visitor
    //-----------------------------------------------------------------
    String result;

    //-----------------------------------------------------------------
    //  Input to Visitor: references to cash
    //-----------------------------------------------------------------
    String cash;
    String ccash;

    public void visit(Expr.StringLit expr)
      {
        String cLit = Convert.toCharLit(expr.s.charAt(0));
        String sLit = Convert.toStringLit(expr.s);
        if (expr.s.length()==1)
          result = "Rune(rune('" + cLit + "')" + ccash + ")";
        else
          result = "Str(\"" + sLit + "\"" + ccash + ")";
      }

    public void visit(Expr.CharClass expr)
      {
        String cLit = Convert.toCharLit(expr.s.charAt(0));
        String sLit = Convert.toStringLit(expr.s);
        if (expr.s.length()==1)
        {
          if (expr.hat)
            result = "Not(rune(\'" + cLit + "\')" + ccash + ")";
          else
           result = "Rune(rune(\'" + cLit + "\')" + ccash + ")";
        }
        else
        {
          if (expr.hat)
            result = "NotInStr(\"" + sLit + "\"" + ccash + ")";
          else
            result = "InStr(\"" + sLit + "\"" + ccash + ")";
        }
      }

    public void visit(Expr.Range expr)
      {
        String aLit = Convert.toCharLit(expr.a);
        String zLit = Convert.toCharLit(expr.z);
        result = "In(rune('"+ aLit + "'),rune('" + zLit + "')" + ccash + ")";
      }

    public void visit(Expr.Any expr)
      { result = "(" + cash + ")"; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Auxiliary methods
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //---------------------------------------------------------------
  //  Create parsing procedures for subexpressions.
  //---------------------------------------------------------------
  void createSubs()
    {
      int toDo = subs.size();

      while (done<toDo)
      {
        for (int i=done;i<toDo;i++)
        {
          Expr expr = subs.elementAt(i);
          procName = expr.name;
          out.box(procName + " = " + Convert.toComment(expr.asString()));
          out.line("func (this *"+parsName+") "+ procName + "() bool {");
          out.indent();

          if (memo | test)
          {
            out.line("if (this.SavedInner(" + procName + ")){ return "
                      + (isPred(expr)? "this.ReusePred(); }" : "this.ReuseInner(); }"));
            if (test) cache = procName;
          }

          else if (isPred(expr))
            out.line("this.BeginWithDiag(\"\",\"" + Convert.toStringLit(diagPred(expr)) + "\");");

          else
            out.line("this.Begin(\"\");");

          expr.accept(procVisitor);
          out.undent();
          out.line("}");
          //out.undent();
          out.line("");
        }
        done = toDo;
        toDo = subs.size(); // We probably added subexprs of subexprs!
      }
    }

  //-------------------------------------------------------------------
  //  isPred
  //-------------------------------------------------------------------
  boolean isPred(Expr expr)
    {
      return
          expr instanceof Expr.And ||
          expr instanceof Expr.Not ;
    }

  //-------------------------------------------------------------------
  //  isTerm
  //-------------------------------------------------------------------
  boolean isTerm(Expr expr)
    {
      return
          expr instanceof Expr.StringLit ||
          expr instanceof Expr.CharClass ||
          expr instanceof Expr.Range ||
          expr instanceof Expr.Any ;

    }

  //-------------------------------------------------------------------
  //  Get diagnostic name of a Rule
  //-------------------------------------------------------------------
  String diagName(Expr.Rule rule)
    {
      if (rule.diagName==null) return rule.name;
      else return Convert.toStringLit(rule.diagName);
    }

  //-------------------------------------------------------------------
  //  Get diagnostic string for a Predicate
  //-------------------------------------------------------------------
  String diagPred(Expr expr)
  {
    if (expr instanceof Expr.And)
    {
      Expr arg = ((Expr.And)expr).expr;
      if (arg instanceof Expr.Ref)
      {
        Expr.Rule rule = ((Expr.Ref)arg).rule;
        return diagName(rule);
      }
      else
        return arg.asString();
    }

    else if (expr instanceof Expr.Not)
    {
      Expr arg = ((Expr.Not)expr).expr;
      if (arg instanceof Expr.Ref)
      {
        Expr.Rule rule = ((Expr.Ref)arg).rule;
        return "not " + diagName(rule);
      }
      else if (arg instanceof Expr.Any)
        return "end of text";
      else
        return "not " + arg.asString();
    }

    else throw new Error("SNOC");
  }


}
