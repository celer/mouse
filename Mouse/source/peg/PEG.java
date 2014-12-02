//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009, 2010, 2011, 2012
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
//   Version 1.3
//    101108 Made into repository for parsed PEG and related methods.
//    101202 Corrected reconstruction of DiagName.
//   Version 1.4
//    110920 Completed all Visitors with 'StarPlus' and 'PlusPlus'.
//   Version 1.5.1
//    120102 (Steve Owens) Removed unused import.
//           Removed unused varable 'nul1' in 'visit(Expr.StarPlus)'.
//
//=========================================================================

package mouse.peg;

import java.util.Hashtable;
import java.util.HashSet;
import mouse.utility.Convert;
import mouse.runtime.Source;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class PEG
//
//-------------------------------------------------------------------------
//
//  A PEG object represents parsed grammar.
//  The parsed grammar is a structure of Expr objects in the form of
//  trees rooted in Expr.Rule objects. The Expr.Rule objects are listed
//  in the array 'rules'. For easier handling, additional arrays contain
//  lists of other objects appearing in the structure: 'subs' for
//  subexpressions, 'terms' for terminals, and 'refs' for Expr.Ref objects.
//
//  The constructor builds this structure from a file containing PEG,
//  computes Ford's attributes, and checks various aspects of the grammar.
//
//  Method 'compact' eliminates duplicate subexpressions from the
//  parsed grammar. After this operation, the parsed grammar is no longer
//  a set of trees, but an acyclic graph, as different Expr nodes may
//  point to the same subexpressions. The 'rules' array is not changed
//  (duplicate rules are not eliminated), but the other arrays are updated.
//
//  The 'show' methods print out on System.out the grammar reconstructed
//  from its parsed form, together with the computed attributes.
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

public class PEG
{
  //=====================================================================
  //
  //  Data
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  Rules, subexpressions, terminals, references.
  //-------------------------------------------------------------------
  public Expr.Rule rules[];
  public Expr subs[];
  public Expr terms[];
  public Expr.Ref refs[];

  //-------------------------------------------------------------------
  //  Counters.
  //-------------------------------------------------------------------
  public int errors;     // Errors
  public int iterAt;     // Iterations for attributes
  public int iterWF;     // Iterations for WF
  public int notWF;      // Not well-formed expressions


  //=====================================================================
  //
  //  Constructor
  //
  //=====================================================================

  public PEG(Source src)
    {
      //---------------------------------------------------------------
      //  Parse the grammar
      //---------------------------------------------------------------
      Parser parser = new Parser();
      parser.parse(src);

      Semantics sem = parser.semantics();
      rules = sem.rules;
      errors = sem.errcount;

      //---------------------------------------------------------------
      //  Quit if parsing failed.
      //---------------------------------------------------------------
      if (errors>0) return;

      //---------------------------------------------------------------
      //  Build expression lists.
      //---------------------------------------------------------------
      makeLists();

      //---------------------------------------------------------------
      //  Resolve name references and quit if error found.
      //---------------------------------------------------------------
      resolve();
      if (errors>0) return;

      //---------------------------------------------------------------
      //  Compute 'asString' for all nodes.
      //---------------------------------------------------------------
      reconstruct();

      //---------------------------------------------------------------
      //  Compute attributes and well-formedness.
      //---------------------------------------------------------------
      attributes();
      computeWF();

      //---------------------------------------------------------------
      //  Diagnose.
      //---------------------------------------------------------------
      Diagnose diag = new Diagnose();
      diag.applyTo(this);
    }


  //=====================================================================
  //
  //  Compact
  //
  //=====================================================================

  public void compact()
    {
      //---------------------------------------------------------------
      //  Use CompactVisitor to eliminate duplicate expressions
      //  from parse tree. (The result is no longer a tree.)
      //---------------------------------------------------------------
      CompactVisitor compactVisitor = new CompactVisitor();
      for (Expr r: rules)
        r.accept(compactVisitor);

      //---------------------------------------------------------------
      //  Build new expression lists.
      //---------------------------------------------------------------
      makeLists();
    }


  //=====================================================================
  //
  //  Show
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  showRules.
  //-------------------------------------------------------------------
  public void showRules()
    {
      System.out.println("\n" + rules.length + " rules");
      for (Expr.Rule r: rules)
        System.out.println("  " + Convert.toPrint(r.asString()) + "   // " + attrs(r));
    }

  //-------------------------------------------------------------------
  //  showAll.
  //-------------------------------------------------------------------
  public void showAll()
    {
      showRules();

      System.out.println("\n" + subs.length + " subexpressions");
      for (Expr e: subs)
        System.out.println("  " + Convert.toPrint(e.asString()) + "   // " + attrs(e));

      System.out.println("\n" + terms.length + " terminals");
      for (Expr e: terms)
        System.out.println("  " + Convert.toPrint(e.asString()) + "   // " + attrs(e));
    }

  //-------------------------------------------------------------------
  //  Format attributes
  //-------------------------------------------------------------------
  private String attrs(Expr e)
    { return " " + (e.nul?"0":"") + (e.adv?"1":"")
                 + (e.fal?"f":"") + (e.WF?"":" !WF"); }


  //=====================================================================
  //
  //  Make Lists
  //
  //---------------------------------------------------------------------
  //
  //  Make linear lists of expressions contained in the parse tree:
  //  inner expressions ('subs'), terminals (terms), references ('refs').
  //
  //=====================================================================

  private void makeLists()
    {
      //---------------------------------------------------------------
      //  Use ListVisitor to build the lists in its local hash sets.
      //---------------------------------------------------------------
      ListVisitor listVisitor = new ListVisitor();
      for (Expr.Rule r: rules)
         r.accept(listVisitor);

      //---------------------------------------------------------------
      //  Convert the hash sets to arrays.
      //---------------------------------------------------------------
      subs  = listVisitor.subs.toArray(new Expr[0]);
      terms = listVisitor.terms.toArray(new Expr[0]);
      refs  = listVisitor.refs.toArray(new Expr.Ref[0]);
    }


  //=====================================================================
  //
  //  Resolve references.
  //
  //=====================================================================

  private void resolve()
    {
      //---------------------------------------------------------------
      //  Mapping from names to Rules.
      //---------------------------------------------------------------
      Hashtable<String,Expr.Rule> names = new Hashtable<String,Expr.Rule>();

      //---------------------------------------------------------------
      //  Referenced names.
      //  Top rule is assumed referenced.
      //---------------------------------------------------------------
      HashSet<String> referenced = new HashSet<String>();
      referenced.add(rules[0].name);

      //---------------------------------------------------------------
      //  Dummy rule - replaces undefined to stop multiple messages.
      //---------------------------------------------------------------
      Expr.Rule dummy = new Expr.Rule(null,null,null,null,null);

      //---------------------------------------------------------------
      //  Build table of Rule names, checking for duplicates.
      //---------------------------------------------------------------
      for (Expr.Rule r: rules)
      {
        Expr.Rule prev = names.put(r.name,r);
        if (prev!=null)
        {
          System.out.println("Error: duplicate name '" + r.name + "'.");
          errors++;
        }
      }

      //---------------------------------------------------------------
      //  Resolve references.
      //---------------------------------------------------------------
      for (Expr.Ref ref: refs)
      {
        ref.rule = names.get(ref.name);
        if (ref.rule==null)
        {
          System.out.println("Error: undefined name '" + ref.name + "'.");
          errors++;
          names.put(ref.name,dummy);
        }
        else
          referenced.add(ref.name);
      }

      //---------------------------------------------------------------
      //  Detect unused rules.
      //---------------------------------------------------------------
      for (Expr.Rule r: rules)
      {
        if (!referenced.contains(r.name))
          System.out.println("Warning: rule '" + r.name + "' is not used.");
      }
    }


  //=====================================================================
  //
  //  Reconstruct Source
  //
  //---------------------------------------------------------------------
  //
  //  Reconstructs, in a standard form, the source string of each
  //  expressions and assigns it to 'asString' field of the Expr object.
  //
  //=====================================================================

  private void reconstruct()
    {
      //---------------------------------------------------------------
      //  Use SourceVisitor to reconstruct source.
      //---------------------------------------------------------------
      SourceVisitor sourceVisitor = new SourceVisitor();
      for (Expr e: rules)
        e.accept(sourceVisitor);
    }


  //=====================================================================
  //
  //  Compute Ford's attributes: nul, adv, fal for all expressions.
  //
  //---------------------------------------------------------------------
  //
  //  Computes nul, adv, and fal attributes for all expressions.
  //  For terminals the attributes are preset by the constructor.
  //  For other expressions they are computed by iteration to a fixpoint.
  //  The AttrVisitor is used for the iteration step.
  //
  //=====================================================================

  private void attributes()
    {
      int trueAttrs; // Number of true attributes after last step
      int a = 0;     // Number of true attributes before last step
      iterAt = 0;    // Number of steps

      AttrVisitor attrVisitor = new AttrVisitor();

      while(true)
      {
        //-------------------------------------------------------------
        //  Iteration step
        //-------------------------------------------------------------
        for (Expr e: refs)
          e.accept(attrVisitor);
        for (Expr e: subs)
          e.accept(attrVisitor);
        for (Expr e: rules)
          e.accept(attrVisitor);

        //-------------------------------------------------------------
        //  Count true attributes (non-terminals only)
        //-------------------------------------------------------------
        trueAttrs = 0;
        for (Expr e: rules)
          trueAttrs += (e.nul? 1:0) + (e.adv? 1:0) + (e.fal? 1:0);
        for (Expr e: subs)
          trueAttrs += (e.nul? 1:0) + (e.adv? 1:0) + (e.fal? 1:0);

        //-------------------------------------------------------------
        //  Break if fixpoint reached
        //-------------------------------------------------------------
        if (trueAttrs==a) break;

        //-------------------------------------------------------------
        //  To next step
        //-------------------------------------------------------------
        a = trueAttrs;
        iterAt++;
      }
    }

  //=====================================================================
  //
  //  Compute well-formedness.
  //
  //---------------------------------------------------------------------
  //
  //  Computes the WF attribute for all expressions.
  //  For terminals the attribute is preset by the constructor.
  //  For other expressions it is computed by iteration to a fixpoint.
  //  The FormVisitor is used for the iteration step.
  //
  //=====================================================================

  private void computeWF()
    {
      int s = -1;  // Number of not well-formed after last step
      iterWF = 0;  // Number of iterations

      FormVisitor formVisitor = new FormVisitor();

      while(true)
      {
        //-------------------------------------------------------------
        //  Iteration step
        //-------------------------------------------------------------
        for (Expr e: refs)
          e.accept(formVisitor);
        for (Expr e: subs)
          e.accept(formVisitor);
        for (Expr e: rules)
          e.accept(formVisitor);

        //-------------------------------------------------------------
        //  Count not well-formed (non-terminals only)
        //-------------------------------------------------------------
        notWF = 0;
        for (Expr e: rules)
          if (!e.WF) notWF++;
        for (Expr e: subs)
          if (!e.WF) notWF++;

        //-------------------------------------------------------------
        //  Break if fixpoint reached
        //-------------------------------------------------------------
        if (notWF==s) break;

        //-------------------------------------------------------------
        //  To next step
        //-------------------------------------------------------------
        s = notWF;
        iterWF++;
      }
    }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  ListVisitor - makes lists of expressions
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //-------------------------------------------------------------------
  //  Each visit adds the visited expression to its proper list, and
  //  then proceeeds to visit all subexpressions, if any.
  //  Note that the visitor must also work after 'compact' operation
  //  that changed the tree into an acyclic graph. As a result, the
  //  visitor may arrive to a node that was already visited.
  //  Therefore we collect data in hash sets, and do not visit
  //  subexpressions if the node is already listed.
  //-------------------------------------------------------------------

  class ListVisitor extends mouse.peg.Visitor
  {
    //-----------------------------------------------------------------
    //  Local lists
    //-----------------------------------------------------------------
    HashSet<Expr>     subs  = new HashSet<Expr>();
    HashSet<Expr>     terms = new HashSet<Expr>();
    HashSet<Expr.Ref> refs  = new HashSet<Expr.Ref>();

    public void visit(Expr.Rule expr)
      {
        for (Expr e: expr.rhs)
          e.accept(this);
      }

    public void visit(Expr.Choice expr)
      { doCompound(expr, expr.expr); }

    public void visit(Expr.Sequence expr)
      { doCompound(expr, expr.expr); }

    public void visit(Expr.And expr)
      { doUnary(expr, expr.expr); }

    public void visit(Expr.Not expr)
      { doUnary(expr, expr.expr); }

    public void visit(Expr.Plus expr)
      { doUnary(expr, expr.expr); }

    public void visit(Expr.Star expr)
      { doUnary(expr, expr.expr); }

    public void visit(Expr.Query expr)
      { doUnary(expr, expr.expr); }

    public void visit(Expr.Ref expr)
      { refs.add(expr); }

    public void visit(Expr.PlusPlus expr)
      { doBinary(expr, expr.expr1,expr.expr2); }

    public void visit(Expr.StarPlus expr)
      { doBinary(expr, expr.expr1,expr.expr2); }

    public void visit(Expr.StringLit expr)
      { terms.add(expr); }

    public void visit(Expr.Range expr)
      { terms.add(expr); }

    public void visit(Expr.CharClass expr)
      { terms.add(expr); }

    public void visit(Expr.Any expr)
      { terms.add(expr); }

    private void doCompound(Expr expr, Expr[] list)
      {
        if (subs.add(expr))   // If not visited yet
          for (Expr e: list)
            e.accept(this);
      }

    private void doBinary(Expr expr, Expr arg1, Expr arg2)
      {
        if (subs.add(expr))   // If not visited yet
          arg1.accept(this);
          arg2.accept(this);
      }

    private void doUnary(Expr expr, Expr arg)
      {
        if (subs.add(expr))   // If not visited yet
          arg.accept(this);
      }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  SourceVisitor - recostructs source strings of expressions
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //-------------------------------------------------------------------
  //  Each visit starts with visiting the subexpressions to construct
  //  their source strings. These strings are then used as building
  //  blocks to produce the final result. Procedure 'enclose'
  //  encloses the subexpression in parentheses if needed, depending
  //  on the binding strength of subexpression and containing expression.
  //-------------------------------------------------------------------

  class SourceVisitor extends mouse.peg.Visitor
  {
    public void visit(Expr.Rule r)
      {
        StringBuilder sb = new StringBuilder();
        sb.append(r.name + " ");

        sb.append("= ");

        String sep = "";
        for (int i=0;i<r.rhs.length;i++)
        {
          sb.append(sep);
          r.rhs[i].accept(this);
          sb.append(enclose(r.rhs[i],0));
          if (r.onSucc[i]!=null)
          sb.append(" " + r.onSucc[i].asString());
          if (r.onFail[i]!=null)
            sb.append(" ~" + r.onFail[i].asString());
          sep = " / ";
        }

        if (r.diagName!=null)
        sb.append(" <" + r.diagName + ">");

        sb.append(" ;");
        r.asString = sb.toString();;
      }

    public void visit(Expr.Choice expr)
      {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (Expr e: expr.expr)
        {
          sb.append(sep);
          e.accept(this);
          sb.append(enclose(e,0));
          sep = " / ";
        }
        expr.asString = sb.toString();
      }

    public void visit(Expr.Sequence expr)
      {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (Expr e: expr.expr)
        {
          sb.append(sep);
          e.accept(this);
          sb.append(enclose(e,1));
          sep = " ";
        }
        expr.asString = sb.toString();;
      }

    public void visit(Expr.And expr)
      {
        expr.expr.accept(this);
        expr.asString = "&" + enclose(expr.expr,3);
      }

    public void visit(Expr.Not expr)
      {
        expr.expr.accept(this);
        expr.asString = "!" + enclose(expr.expr,3);
      }

    public void visit(Expr.Plus expr)
      {
        expr.expr.accept(this);
        expr.asString = enclose(expr.expr,4) + "+";
      }

    public void visit(Expr.Star expr)
      {
        expr.expr.accept(this);
        expr.asString = enclose(expr.expr,4) + "*";
      }

    public void visit(Expr.Query expr)
      {
        expr.expr.accept(this);
        expr.asString = enclose(expr.expr,4) + "?";
      }

    public void visit(Expr.PlusPlus expr)
      {
        expr.expr1.accept(this);
        expr.expr2.accept(this);
        expr.asString = enclose(expr.expr1,4) + "++ " + enclose(expr.expr2,4);
      }

    public void visit(Expr.StarPlus expr)
      {
        expr.expr1.accept(this);
        expr.expr2.accept(this);
        expr.asString = enclose(expr.expr1,4) + "*+ " + enclose(expr.expr2,4);
      }



    //-----------------------------------------------------------------
    //  Parenthesizing
    //-----------------------------------------------------------------
    private String enclose(Expr e, int mybind)
      {
        boolean nest = e.bind()<=mybind;
        return (nest?"(":"") + e.asString + (nest?")":"");
      }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  CompactVisitor - eliminates duplicate expresions
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //-------------------------------------------------------------------
  //  Each visit examines subexpressions of a visited expression.
  //  If it finds the subexpression identical to a previously
  //  encountered, replaces the subexpression by the latter.
  //  Otherwise, it proceeds to visit the subexpression.
  //  Expressions are considered identical if they have the same
  //  reconstructed source.
  //-------------------------------------------------------------------
  class CompactVisitor extends mouse.peg.Visitor
  {
    //-----------------------------------------------------------------
    //  Hash table to detect identical expressions.
    //  The table maps sources to expressions.
    //-----------------------------------------------------------------
    Hashtable<String,Expr> sources = new Hashtable<String,Expr>();

    public void visit(Expr.Rule r)
      { doCompound(r, r.rhs); }

    public void visit(Expr.Choice expr)
      { doCompound(expr, expr.expr); }

    public void visit(Expr.Sequence expr)
      { doCompound(expr, expr.expr); }

    public void visit(Expr.And expr)
      {
        Expr alias = alias(expr.expr);
        if (alias!=null) expr.expr = alias;
      }

    public void visit(Expr.Not expr)
      {
        Expr alias = alias(expr.expr);
        if (alias!=null) expr.expr = alias;
      }

    public void visit(Expr.Plus expr)
      {
        Expr alias = alias(expr.expr);
        if (alias!=null) expr.expr = alias;
      }

    public void visit(Expr.Star expr)
      {
        Expr alias = alias(expr.expr);
        if (alias!=null) expr.expr = alias;
      }

    public void visit(Expr.Query expr)
      {
        Expr alias = alias(expr.expr);
        if (alias!=null) expr.expr = alias;
      }

    public void visit(Expr.PlusPlus expr)
      { doBinary(expr, expr.expr1, expr.expr2); }

    public void visit(Expr.StarPlus expr)
      { doBinary(expr, expr.expr1, expr.expr2); }


    private void doBinary(Expr expr, Expr arg1, Expr arg2)
      {
        Expr alias = alias(arg1);
        if (alias!=null) arg1 = alias;
        alias = alias(arg2);
        if (alias!=null) arg2 = alias;
      }

    private void doCompound(Expr expr, Expr[] args)
      {
        for (int i=0;i<args.length;i++)
        {
          Expr alias = alias(args[i]);
          if (alias!=null) args[i] = alias;
        }
      }

    //-----------------------------------------------------------------
    //  If the 'sources' table already contains an expression with
    //  the same source as 'expr', return that expression.
    //  Otherwise add 'expr' to the table, visit 'expr', and return null.
    //-----------------------------------------------------------------
    private Expr alias(Expr expr)
      {
        String source = expr.asString();
        Expr found = sources.get(source);
        if (found!=null) return found;
        sources.put(source,expr);
        expr.accept(this);
        return null;
      }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  AttrVisitor - computes Ford's attributes
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //-------------------------------------------------------------------
  //  Each visit computes attributes from those of subexpressions.
  //  Attributes for terminals are preset by their constructors.
  //  The visitor does not climb down the parse tree.
  //-------------------------------------------------------------------

  class AttrVisitor extends mouse.peg.Visitor
  {
    public void visit(Expr.Rule expr)
      { doChoice(expr,expr.rhs); }

    public void visit(Expr.Choice expr)
      { doChoice(expr,expr.expr); }

    public void visit(Expr.Sequence expr)
      {
        boolean allNull = true;
        boolean exAdv   = false;
        boolean allSucc = true;
        boolean exFail  = false;

        for (Expr e: expr.expr)
        {
          if (!e.nul) allNull = false;
          if (allSucc && e.fal) exFail = true;
          if (e.adv)  exAdv = true;
          if (!e.nul && !e.adv) allSucc = false;
        }

        if (allNull) expr.nul = true;
        if (allSucc && exAdv) expr.adv = true;
        if (exFail) expr.fal = true;
      }

    public void visit(Expr.And expr)
      {
        Expr e = expr.expr;
        if (e.nul || e.adv) expr.nul = true;
        if (e.fal) expr.fal = true;
      }

    public void visit(Expr.Not expr)
      {
        Expr e = expr.expr;
        if (e.nul || e.adv) expr.fal = true;
        if (e.fal) expr.nul = true;
      }

    public void visit(Expr.Plus expr)
      {
        Expr e = expr.expr;
        if (e.adv) expr.adv = true;
        if (e.fal) expr.fal = true;
      }

    public void visit(Expr.Star expr)
      {
        Expr e = expr.expr;
        if (e.adv) expr.adv = true;
        if (e.fal) expr.nul = true;
      }

    public void visit(Expr.Query expr)
      {
        Expr e = expr.expr;
        if (e.adv) expr.adv = true;
        if (e.nul || e.fal) expr.nul = true;
      }

    public void visit(Expr.PlusPlus expr)
      {
        Expr e1 = expr.expr1;
        Expr e2 = expr.expr2;

        // Computed as for (!e2 e1)(!e2 e1)* e2
        // Attributes of !e2 e1
        boolean nul1 = e2.fal && e1.nul;
        boolean adv1 = e2.fal && e1.adv;
        boolean fal1 = e2.nul || e2.adv;

        // Attributes of (!e2 e1)*
        boolean nul2 = fal1;
        boolean adv2 = adv1;
        boolean fal2 = false;

        // Attributes of (!e2 e1)* e2
        boolean nul3 = nul2 && e2.nul;
        boolean adv3 = (nul2 && e2.adv) || (adv2 && e2.adv) || (adv2 && e2.nul);
        boolean fal3 = fal2 || ((nul2 || adv2) && e2.fal);

        // Attributes of (!e2 e1)(!e2 e1)* e2
        expr.nul = nul1 && nul3;
        expr.adv = (nul1 && adv3) || (adv1 && adv3) || (adv1 && nul3);
        expr.fal = fal1 || ((nul1 || adv1) && fal3);
      }

    public void visit(Expr.StarPlus expr)
      {
        Expr e1 = expr.expr1;
        Expr e2 = expr.expr2;

        // Computed as for (!e2 e1)* e2
        // Attributes of !e2 e1
        boolean adv1 = e2.fal & e1.adv;
        boolean fal1 = e2.nul | e2.adv;

        // Attributes of (!e2 e1)*
        boolean nul2 = fal1;
        boolean adv2 = adv1;
        boolean fal2 = false;

        // Attributes of (!e2 e1)* e2
        expr.nul = nul2 & e2.nul;
        expr.adv = (nul2 & e2.adv) | (adv2 & e2.adv) | (adv2 & e2.nul);
        expr.fal = fal2 | ((nul2 | adv2) & e2.fal);
      }

    public void visit(Expr.Ref expr)
      {
        Expr e = expr.rule;
        expr.nul = e.nul;
        expr.adv = e.adv;
        expr.fal = e.fal;
      }


    private void doChoice(Expr expr, Expr[] list)
      {
        boolean n = false;
        boolean a = false;
        boolean f = true;

        for (Expr e: list)
        {
          n |= e.nul;
          a |= e.adv;
          f &= e.fal;
          if (!f) break;
        }

        if (n) expr.nul = true;
        if (a) expr.adv = true;
        if (f) expr.fal = true;
      }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  FormVisitor - computes WellFormed attribute
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //-------------------------------------------------------------------
  //  Each visit computes the attribute from those of subexpressions.
  //  Attributes for terminals are preset by their constructors.
  //  The visitor does not climb down the parse tree.
  //-------------------------------------------------------------------

  class FormVisitor extends mouse.peg.Visitor
  {

    public void visit(Expr.Rule expr)
      {
        for (Expr e: expr.rhs)
          if (!e.WF) return;
        expr.WF = true;
      }

    public void visit(Expr.Choice expr)
      {
        for (Expr e: expr.expr)
          if (!e.WF) return;
        expr.WF = true;
      }

    public void visit(Expr.Sequence expr)
      {
        for (Expr e: expr.expr)
        {
          if (!e.WF) return;
          if (!e.nul) break;
        }
        expr.WF = true;
      }

    public void visit(Expr.And expr)
      {
        if (expr.expr.WF)
          expr.WF = true;
      }

    public void visit(Expr.Not expr)
      {
        if (expr.expr.WF)
          expr.WF = true;
      }

    public void visit(Expr.Plus expr)
      {
        if (expr.expr.WF && !expr.expr.nul)
          expr.WF = true;
      }

    public void visit(Expr.Star expr)
      {
        if (expr.expr.WF && !expr.expr.nul)
          expr.WF = true;
      }

    public void visit(Expr.Query expr)
      {
        if (expr.expr.WF)
          expr.WF = true;
      }

    public void visit(Expr.PlusPlus expr)
      {
        if (expr.expr1.WF && expr.expr2.WF && !expr.expr1.nul)
          expr.WF = true;
      }

    public void visit(Expr.StarPlus expr)
      {
        if (expr.expr1.WF && expr.expr2.WF && !expr.expr1.nul)
          expr.WF = true;
      }

    public void visit(Expr.Ref expr)
      { expr.WF = expr.rule.WF; }
  }
}

