//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009, 2011 by Roman R. Redziejowski (www.romanredz.se).
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
//    090405 Bug fix in Unicode escape.
//    090701 License changed by the author to Apache v.2.
//    090710 Removed printout left after bug fix.
//    090816 Made Grammar() non-boolean.
//   Version 1.4
//    110919 Modified 'CharClass' to handle enlarged syntax.
//    110920 Modified 'Suffixed' to create 'PlusPlus' and 'StarPlus' objects.
//
//=========================================================================

package mouse.peg;

import mouse.utility.Convert;

class Semantics extends mouse.runtime.SemanticsBase
{
  //=====================================================================
  //
  //  Data
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  Results: array of Rules and number of errors.
  //-------------------------------------------------------------------
  public Expr.Rule[] rules = null;
  public int errcount = 0;


  //=====================================================================
  //
  //  Some shorthands
  //
  //=====================================================================

  Expr exprValue(int i)
    { return (Expr)rhs(i).get(); }

  Expr.Rule ruleValue(int i)
    { return (Expr.Rule)rhs(i).get(); }

  String stringValue(int i)
    { return (String)rhs(i).get(); }

  Action actionValue(int i)
    { return (Action)rhs(i).get(); }

  char charValue(int i)
    { return (Character)rhs(i).get(); }


  //=======================================================================
  //
  //  Semantic procedures
  //
  //=======================================================================
  //-------------------------------------------------------------------
  //  Grammar = Space? (&_ (Rule / Skip))* EOT
  //              0         1,2,..,-2    -1
  //-------------------------------------------------------------------
  void Grammar()
    {
      int n = rhsSize()-2; // Number of Rules, correct or not.
      if (n<=0)
      {
        System.out.println("input file empty");
        errcount++;
        return;
      }

      if (errcount>0) return;

      // All Rules were correctly parsed. Construct array of Rules.
      rules = new Expr.Rule[n];
      for (int i=0;i<n;i++)
        rules[i] = ruleValue(i+1);

      // Print trace if requested.
      if (trace.indexOf('G')<0) return;
      for (Expr.Rule r: rules)
        System.out.println(Convert.toPrint(r.asString()));
    }

  //-------------------------------------------------------------------
  //  Rule = Name EQUAL RuleRhs DiagName? SEMI
  //           0    1      2        3     4(3)
  //-------------------------------------------------------------------
  void Rule()
    {
      String ruleName = stringValue(0);

      String diagName = null;
      if (rhsSize()==5)
        diagName = stringValue(3);

      // RuleRhs returns Expr.Rule object without name and diag name
      Expr.Rule temp = ruleValue(2);

      // Fill default action names
      if (temp.rhs.length==1)
      {
        if (temp.onSucc[0]!=null && temp.onSucc[0].name.isEmpty())
          temp.onSucc[0].name = ruleName;
        if (temp.onFail[0]!=null && temp.onFail[0].name.isEmpty())
          temp.onFail[0].name = ruleName + "_fail";
      }
      else
      {
        for (int i=0;i<temp.rhs.length;i++)
        {
          if (temp.onSucc[i]!=null && temp.onSucc[i].name.isEmpty())
            temp.onSucc[i].name = ruleName + "_" + i;
          if (temp.onFail[i]!=null && temp.onFail[i].name.isEmpty())
            temp.onFail[i].name = ruleName + "_" + i + "_fail";
        }
      }

      // Make new object because components should be final
      lhs().put(new Expr.Rule(ruleName,diagName,temp.rhs,temp.onSucc,temp.onFail));
    }

  //-------------------------------------------------------------------
  //  Rule not recognized
  //-------------------------------------------------------------------
  void Error()
    {
      System.out.println(lhs().errMsg());
      lhs().errClear();
      errcount++;
    }

  //-------------------------------------------------------------------
  //  RuleRhs = Sequence Actions (SLASH Sequence Actions)*
  //                0       1     2,5,.. 3,6,..   4,7,..
  //-------------------------------------------------------------------
  void RuleRhs()
    {
      // Returns a temporary Rule object with 'name' and 'diagName' null.
      int n = (rhsSize()+1)/3; // Number of 'Sequence's

      Expr[]   seq  = new Expr[n];
      Action[] succ = new Action[n];
      Action[] fail = new Action[n];

      for (int i=0;i<n;i++)
      {
        seq[i] = exprValue(3*i);

        Action[] actions = (Action[])(rhs(3*i+1).get());
        succ[i] = actions[0];
        fail[i] = actions[1];
      }

      lhs().put(new Expr.Rule(null,null,seq,succ,fail));
    }

  //-------------------------------------------------------------------
  //  Choice = Sequence (SLASH Sequence)*
  //               0     1,3,..  2,4,..
  //-------------------------------------------------------------------
  void Choice()
    {
      int n = rhsSize();
      if (n==1)
      {
        lhs().put(rhs(0).get());
        return;
      }
      Expr[] seq = new Expr[(n+1)/2];
      for (int i=0;i<seq.length;i++)
        seq[i] = exprValue(2*i);

      lhs().put(new Expr.Choice(seq));
    }

  //-------------------------------------------------------------------
  //  Sequence = Prefixed+
  //               0,1,..
  //-------------------------------------------------------------------
  void Sequence()
    {
      int n = rhsSize();

      if (n==1)
      {
        lhs().put(rhs(0).get());
        return;
      }

      Expr[] pref = new Expr[n];
      for (int i=0;i<n;i++)
        pref[i] = exprValue(i);

      lhs().put(new Expr.Sequence(pref));
    }

  //-------------------------------------------------------------------
  //  Prefixed = PREFIX? Suffixed
  //                0     1(0)
  //-------------------------------------------------------------------
  void Prefixed()
    {
      if (rhsSize()==1)
      {
        lhs().put(rhs(0).get());
        return;
      }

      Expr arg = exprValue(1);
      boolean and = rhs(0).charAt(0)=='&';

      // If nested predicate: reduce to single one
      if (arg instanceof Expr.And)
      {
        if (and)
          lhs().put(arg);
        else
          lhs().put(new Expr.Not(((Expr.And)arg).expr));
      }
      else if (arg instanceof Expr.Not)
      {
        if (and)
          lhs().put(arg);
        else
          lhs().put(new Expr.And(((Expr.Not)arg).expr));
      }

      // Argument is not a predicate
      else
      {
        if (and)
          lhs().put(new Expr.And(arg));
        else
          lhs().put(new Expr.Not(arg));
      }
    }

  //-------------------------------------------------------------------
  //  Suffixed  = Primary (UNTIL Primary / SUFFIX)?
  //                 0       1      2         1
  //-------------------------------------------------------------------
  void Suffixed()
    {
      if (rhsSize()==1)           // Primary only
        lhs().put(rhs(0).get());

      else if (rhsSize()==2)      // Primary SUFFIX
      {
        if (rhs(1).charAt(0)=='?')
          lhs().put(new Expr.Query(exprValue(0)));
        else if (rhs(1).charAt(0)=='*')
          lhs().put(new Expr.Star(exprValue(0)));
        else
          lhs().put(new Expr.Plus(exprValue(0)));
      }

      else                        // Primary UNTIL Primary
      {
        if (rhs(1).charAt(0)=='*')
          lhs().put(new Expr.StarPlus(exprValue(0),exprValue(2)));
        else
          lhs().put(new Expr.PlusPlus(exprValue(0),exprValue(2)));
      }
    }

  //-------------------------------------------------------------------
  //  Primary = Name
  //             0
  //-------------------------------------------------------------------
  void Resolve()
    {
      Expr.Ref ref = new Expr.Ref(stringValue(0));
      lhs().put(ref);
    }

  //-------------------------------------------------------------------
  //  Primary = LPAREN Choice RPAREN
  //               0      1      2
  //-------------------------------------------------------------------
  void Pass2()
    { lhs().put(rhs(1).get()); }

  //-------------------------------------------------------------------
  //  Primary = ANY
  //-------------------------------------------------------------------
  void Any()
    { lhs().put(new Expr.Any()); }

  //-------------------------------------------------------------------
  //  Primary = StringLit
  //  Primary = Range
  //  Primary = CharClass
  //  Char = Escape
  //-------------------------------------------------------------------
  void Pass()
    { lhs().put(rhs(0).get()); }

  //-------------------------------------------------------------------
  //  Actions = OnSucc OnFail
  //               0     1
  //-------------------------------------------------------------------
  void Actions()
    { lhs().put(new Action[]{actionValue(0),actionValue(1)}); }

  //-------------------------------------------------------------------
  //  OnSucc = (LWING AND? Name? RWING)?
  //              0    1    -2    -1
  //-------------------------------------------------------------------
  void OnSucc()
    {
      int n = rhsSize();

      if (n==0)
        lhs().put(null);

      else
      {
        String name = rhs(n-2).isA("Name")? stringValue(n-2) : "";

        if (rhs(1).isA("AND"))
          lhs().put(new Action(name,true));
        else
          lhs().put(new Action(name,false));
      }
    }

  //-------------------------------------------------------------------
  //  OnFail = (TILDA LWING Name? RWING)?
  //              0     1    -2    -1
  //-------------------------------------------------------------------
  void OnFail()
    {
      int n = rhsSize();

      if (n==0)
        lhs().put(null);

      else
      {
        String name = rhs(n-2).isA("Name")? stringValue(n-2) : "";
        lhs().put(new Action(name,false));
      }
    }

  //-------------------------------------------------------------------
  //  Name = Letter (Letter / Digit)* Space
  //            0        1 ... -2       -1
  //-------------------------------------------------------------------
  void Name()
    { lhs().put(rhsText(0,rhsSize()-1)); }

  //-------------------------------------------------------------------
  //  DiagName = "(" (!")" Char)+ ")" Space
  //              0    1,2..,-3    -2   -1
  //-------------------------------------------------------------------
  void DiagName()
    {
      StringBuffer sb = new StringBuffer();
      for (int i=1;i<rhsSize()-2;i++)
        sb.append(charValue(i));
      lhs().put(sb.toString());
    }

  //-------------------------------------------------------------------
  //  StringLit = ["] (!["] Char)+ ["] Space
  //               0    1,2..,-3    -2   -1
  //-------------------------------------------------------------------
  void StringLit()
    {
      StringBuffer sb = new StringBuffer();
      for (int i=1;i<rhsSize()-2;i++)
        sb.append(charValue(i));
      lhs().put(new Expr.StringLit(sb.toString()));
    }

  //-------------------------------------------------------------------
  //  CharClass = ("[" / "^[") (!"]" Char)+ "]" Space
  //                0      0    1,2..,-3    -2   -1
  //-------------------------------------------------------------------
  void CharClass()
    {
      StringBuffer sb = new StringBuffer();
      for (int i=1;i<rhsSize()-2;i++)
        sb.append(charValue(i));
      lhs().put(new Expr.CharClass(sb.toString(),rhs(0).charAt(0)=='^'));
    }

  //-------------------------------------------------------------------
  //  Range = "[" Char "-" Char "]" Space
  //           0    1   2    3   4    5
  //-------------------------------------------------------------------
  void Range()
    {
      char a = (charValue(1));
      char z = (charValue(3));
      lhs().put(new Expr.Range(a,z));
    }

  //-------------------------------------------------------------------
  //  Char = ![\r\n]_
  //-------------------------------------------------------------------
  void Char()
    { lhs().put(rhs(0).charAt(0)); }

  //-------------------------------------------------------------------
  //  Escape = "\\u" HexDigit HexDigit HexDigit HexDigit
  //              0       1       2        3        4
  //-------------------------------------------------------------------
  void Unicode()
    {
        String s = rhsText(1,5);
        lhs().put((char)Integer.parseInt(s,16));
    }

  //-------------------------------------------------------------------
  //  Escape = "\n"
  //             0
  //-------------------------------------------------------------------
  void Newline()
    { lhs().put('\n'); }

  //-------------------------------------------------------------------
  //  Escape = "\r"
  //             0
  //-------------------------------------------------------------------
  void CarRet()
    { lhs().put('\r'); }

  //-------------------------------------------------------------------
  //  Escape = "\t"
  //             0
  //-------------------------------------------------------------------
  void Tab()
    { lhs().put('\t'); }

  //-------------------------------------------------------------------
  //  Escape = "\" _
  //            0  1
  //-------------------------------------------------------------------
  void Escape()
    { lhs().put(rhs(1).charAt(0)); }

  //-------------------------------------------------------------------
  //  Space = ([ \r\n\t] / Comment)*
  //-------------------------------------------------------------------
  void Space()
    {  lhs().errClear(); }

}
