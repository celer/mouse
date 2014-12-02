//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2010, 2011 by Roman R. Redziejowski (www.romanredz.se).
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
//    101109 Created for version 1.3.
//   Version 1.4
//    111010 Completed DiagVisitor with 'StarPlus' and 'PlusPlus'.
//
//=========================================================================

package mouse.peg;

import java.util.HashSet;
import mouse.utility.BitMatrix;
import mouse.utility.Convert;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class Diagnose
//
//-------------------------------------------------------------------------
//
//  Contains methods to detect and write messages about:
//  - not well-formed expressions;
//  - no-fail alternatives in Choice;
//  - no-success alternatives in Sequence;
//  - superfluous '?' operators.
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

class Diagnose
{
  //=====================================================================
  //
  //  Nonterminal expressions.
  //
  //=====================================================================
  int N;        // Number of nonterminals
  int R;        // Number of rules
  Expr exprs[]; // Array of nonterminals

  //=====================================================================
  //
  //  Matrices for diagnosing left recursion.
  //  first.at(i,j) means exprs[i] directly calls exprs[j] as first.
  //  First.at(i,j) means exprs[i] (in)directly calls exprs[j] as first.
  //  For easier construction, the matrices have dimension N+1 x N+1,
  //  with row and column N used for all terminals (not listed in exprs).
  //  This row and column are not used in diagnostics.
  //
  //=====================================================================
  BitMatrix first;
  BitMatrix First;

  //=====================================================================
  //
  //  Lists of expression names to appear in diagnostics.
  //  The grammar often contains duplicate sub-expressions.
  //  To avoid duplication of messages, information is collected
  //  in hash sets.
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  Left-recursive expressions.
  //-------------------------------------------------------------------
  HashSet<String> recur = new HashSet<String>();

  //-------------------------------------------------------------------
  //  Expressions under superfluous query.
  //-------------------------------------------------------------------
  HashSet<String> query = new HashSet<String>();

  //-------------------------------------------------------------------
  //  Choice alternatives that cannot fail.
  //-------------------------------------------------------------------
  HashSet<String> choice = new HashSet<String>();

  //-------------------------------------------------------------------
  //  Expressions that always fail.
  //-------------------------------------------------------------------
  HashSet<String> fail = new HashSet<String>();

  //-------------------------------------------------------------------
  //  Nullable iterations.
  //-------------------------------------------------------------------
  HashSet<String> iter = new HashSet<String>();


  //=====================================================================
  //
  //  Detect problems and write messages.
  //
  //=====================================================================

  void applyTo(PEG peg)
    {
      //---------------------------------------------------------------
      //  Build array 'exprs' of nonterminal expressions.
      //  For each Expr object, set 'index' to its position in 'exprs'.
      //---------------------------------------------------------------
      N = peg.rules.length + peg.subs.length;
      R = peg.rules.length;
      exprs = new Expr[N];

      int i = 0;

      for (Expr e: peg.rules)
      {
        e.index = i;
        exprs[i] = e;
        i++;
      }

      for (Expr e: peg.subs)
      {
        e.index = i;
        exprs[i] = e;
        i++;
      }

      //---------------------------------------------------------------
      //  The Expr.Ref nodes are not included in 'exprs',
      //  but they obtain index of the node they refer to.
      //---------------------------------------------------------------
      for (Expr.Ref e: peg.refs)
        e.index = e.rule.index;

      //---------------------------------------------------------------
      //  The terminal nodes are not included in 'exprs',
      //  but they obtain index N.
      //---------------------------------------------------------------
      for (Expr e: peg.terms)
        e.index = N;

      //---------------------------------------------------------------
      //  Initialize 'first'.
      //---------------------------------------------------------------
      first = BitMatrix.empty(N+1);

      //---------------------------------------------------------------
      //  Scan nonterminals using DiagVisitor.
      //---------------------------------------------------------------
      DiagVisitor diagVisitor = new DiagVisitor();
      for (Expr e: exprs)
        e.accept(diagVisitor);

      //---------------------------------------------------------------
      //  Find expressions that always fail.
      //---------------------------------------------------------------
      for (Expr e: exprs)
        if (!e.nul & !e.adv)
           fail.add(diagName(e));

      //---------------------------------------------------------------
      //  Find left recursion.
      //---------------------------------------------------------------
      First = first.closure();
      for (i=0;i<R;i++)
        if (First.at(i,i))
          leftRecursion(i);

      //---------------------------------------------------------------
      //  Write out findings.
      //---------------------------------------------------------------
      if (peg.notWF>0)
      {
        System.out.println("Warning: the grammar not well-formed.");

        for (String s: iter)
          System.out.println("- " + s + " may consume empty string.");

        for (String s: recur)
          System.out.println(s + ".");

        return;
      }

      //---------------------------------------------------------------
      //  We arrive here only if the grammar is well-formed.
      //  Otherwise the fail / succeed attributes are incomplete.
      //---------------------------------------------------------------
      for (String s: fail)
        System.out.println("Warning: " + s + " always fails.");

      for (String s: choice)
        System.out.println("Warning: " + s + " never fails and hides other alternative(s).");

      for (String s: query)
        System.out.println("Info: as " + s + " never fails, the '?' in " + s + "? can be dropped.");
    }


  //=====================================================================
  //
  //  Provide left-recursion details of exprs[i].
  //
  //=====================================================================
  private void leftRecursion(int i)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("- " + diagName(exprs[i]) + " is left-recursive");
      String sep = " via ";
      for (int j=0;j<N;j++)
      {
        if (first.at(i,j) && First.at(j,i))
        {
          sb.append(sep + diagName(exprs[j]));
          sep = " and ";
        }
      }
      recur.add(sb.toString());
    }


  //=====================================================================
  //
  //  Diagnostic name for expression.
  //
  //=====================================================================
  private String diagName(Expr e)
    {
      if (e.name!=null) return e.name;
      return Convert.toPrint(e.asString());
    }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  DiagVisitor - collects diagnostic information.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  class DiagVisitor extends mouse.peg.Visitor
  {
    //-----------------------------------------------------------------
    //  Rule.
    //-----------------------------------------------------------------
    public void visit(Expr.Rule expr)
      { doChoice(expr,expr.rhs); }

    //-----------------------------------------------------------------
    //  Choice.
    //-----------------------------------------------------------------
    public void visit(Expr.Choice expr)
      { doChoice(expr,expr.expr); }

    //-----------------------------------------------------------------
    //  Sequence.
    //-----------------------------------------------------------------
    public void visit(Expr.Sequence expr)
      {
        for (int i=0; i<expr.expr.length; i++)
        {
          first.set(expr.index,expr.expr[i].index);
          if (!expr.expr[i].nul) break;
        }
      }

    //-----------------------------------------------------------------
    //  And predicate.
    //-----------------------------------------------------------------
    public void visit(Expr.And expr)
      { first.set(expr.index,expr.expr.index); }

    //-----------------------------------------------------------------
    //  Not predicate.
    //-----------------------------------------------------------------
    public void visit(Expr.Not expr)
      { first.set(expr.index,expr.expr.index); }

    //-----------------------------------------------------------------
    //  Plus.
    //-----------------------------------------------------------------
    public void visit(Expr.Plus expr)
      {
        if (expr.expr.nul) iter.add(diagName(expr.expr) + " in " + diagName(expr));
        first.set(expr.index,expr.expr.index);
     }

    //-----------------------------------------------------------------
    //  Star.
    //-----------------------------------------------------------------
    public void visit(Expr.Star expr)
      {
        if (expr.expr.nul) iter.add(diagName(expr.expr) + " in " + diagName(expr));
        first.set(expr.index,expr.expr.index);
     }

    //-----------------------------------------------------------------
    //  Query.
    //-----------------------------------------------------------------
    public void visit(Expr.Query expr)
      {
        if (expr.expr.nul) query.add(diagName(expr.expr));
        first.set(expr.index,expr.expr.index);
      }

    //-----------------------------------------------------------------
    //  StarPlus.
    //-----------------------------------------------------------------
    public void visit(Expr.StarPlus expr)
      {
        if (expr.expr1.nul) iter.add(diagName(expr.expr1) + " in " + diagName(expr));
        first.set(expr.index,expr.expr1.index);
        first.set(expr.index,expr.expr2.index);
     }

    //-----------------------------------------------------------------
    //  PlusPlus.
    //-----------------------------------------------------------------
    public void visit(Expr.PlusPlus expr)
      {
        if (expr.expr1.nul) iter.add(diagName(expr.expr1) + " in " + diagName(expr));
        first.set(expr.index,expr.expr1.index);
        first.set(expr.index,expr.expr2.index);
     }

    //-----------------------------------------------------------------
    //  Common for Rule and Choice.
    //-----------------------------------------------------------------
    private void doChoice(Expr expr, Expr[] list)
      {
        for (int i=0; i<list.length-1; i++)
          if (!list[i].fal)
            choice.add(diagName(list[i]) + " in " + diagName(expr));
        for (Expr e: list)
          first.set(expr.index,e.index);
      }
  }
}
