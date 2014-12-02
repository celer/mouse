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
//    090805 'source' renamed to 'asString' and modified to contain
//           reconstructed source in 'true' form.
//   Version 1.3
//    100510 Attributes made public (for access from Generate).
//    101111 Removed 'position' (unused).
//   Version 1.4
//    110919 Added 'hat' field to 'CharClass'.
//    110920 Added subclasses 'PlusPlus' and 'StarPlus'.
//   Version 1.5.1
//    120102 (Steve Owens) Removed unused import.
//
//
//=========================================================================

package mouse.peg;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class Expr
//
//  Objects of class Expr represent parsing expressions.
//  Expr is an abstract class, with concrete subclasses representing
//  different kinds of expressions:
//
//  - Expr.Rule - name = expression
//  - Expr.Choice - two or more expressions separated by '/'.
//  - Expr.Sequence - sequence of two or more expressions.
//  - Expr.And - expression preceded by '&'.
//  - Expr.Not - expression preceded by '!'.
//  - Expr.Plus - expression followed by '+'.
//  - Expr.Star - expression followed by '*'.
//  - Expr.Query - expression followed by '?'.
//  - Expr.PlusPlus - two expressions separated by by '++'.
//  - Expr.StarPlus - two expressions separated by by '*+'.
//  - Expr.Ref - reference to another expression.
//  - Expr.StringLit - string literal.
//  - Expr.CharClass - character class.
//  - Expr.Range - character from range.
//  - Expr.Any - any character.
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH


public abstract class Expr
{
  //=====================================================================
  //
  //  Common data.
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  Name.
  //-------------------------------------------------------------------
  public String name;

  //-------------------------------------------------------------------
  //  Index in vectors and matrices.
  //-------------------------------------------------------------------
  public int index;

  //-------------------------------------------------------------------
  //  Reconstructed source text in 'true' form:
  //  with all literals converted to charaters they represent.
  //-------------------------------------------------------------------
  public String asString;

  //-------------------------------------------------------------------
  //  Ford's attributes.
  //-------------------------------------------------------------------
  public boolean nul = false; // May consume null string
  public boolean adv = false; // May consume non-null string
  public boolean fal = false; // May fail
  public boolean WF  = false; // Is well-formed


  //=====================================================================
  //
  //  Common methods.
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  Accept visitor.
  //-------------------------------------------------------------------
  public abstract void accept(Visitor v);

  //-------------------------------------------------------------------
  //  Return source text in true form.
  //-------------------------------------------------------------------
  public String asString() { return asString; }

  //-------------------------------------------------------------------
  //  Binding strength.
  //-------------------------------------------------------------------
  int bind() { return 5; }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Rule
  //
  //  Represents rule of the form name = expression.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Rule extends Expr
  {
    //-----------------------------------------------------------------
    //  Data.
    //  An absent action is represented by null (NOT empty String).
    //-----------------------------------------------------------------
    public String diagName; // Diagnostic name (null if none).
    public Expr[] rhs;      // Expressions on the right-hand side.
    public Action[] onSucc; // Actions for components of Expr.
    public Action[] onFail;


    //-----------------------------------------------------------------
    //  Create the object with specified components.
    //-----------------------------------------------------------------
    public Rule
      ( final String name, final String diagName,
        final Expr[] rhs, final Action[] onSucc, final Action[] onFail)
      {
        this.name     = name;
        this.diagName = diagName;
        this.rhs      = rhs;
        this.onSucc   = onSucc;
        this.onFail   = onFail;
      }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Choice
  //
  //  Represents expression 'expr-1 / expr-2 / ... / expr-n' where n>1.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Choice extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public final Expr[] expr;  // The expr's

    //-----------------------------------------------------------------
    //  Create object with specified expr's.
    //-----------------------------------------------------------------
    public Choice(final Expr[] expr)
      { this.expr = expr; }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 0; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Sequence
  //
  //  Represents expression "expr-1 expr-2  ... expr-n" where n>1.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Sequence extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public Expr[] expr;  // The 'expr's

    //-----------------------------------------------------------------
    //  Create object with specified 'expr's.
    //-----------------------------------------------------------------
    public Sequence(final Expr[] expr)
      { this.expr = expr; }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 1; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.And
  //
  //  Represents expression '&expr'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class And extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public Expr expr;

    //-----------------------------------------------------------------
    //  Create object with specified 'expr'.
    //-----------------------------------------------------------------
    public And(final Expr expr)
      { this.expr = expr; }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 3; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Not
  //
  //  Represents expression '!expr'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Not extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public Expr expr;

    //-----------------------------------------------------------------
    //  Create object with specified 'expr'.
    //-----------------------------------------------------------------
    public Not(final Expr expr)
      { this.expr = expr; }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 3; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Plus
  //
  //  Represents expression 'expr+'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Plus extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public Expr expr;

    //-----------------------------------------------------------------
    //  Create object with specified 'expr'.
    //-----------------------------------------------------------------
    public Plus(final Expr expr)
      { this.expr = expr; }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 4; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Star
  //
  //  Represents expression 'expr*'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Star extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public Expr expr;

    //-----------------------------------------------------------------
    //  Create object with specified 'expr'.
    //-----------------------------------------------------------------
    public Star(final Expr expr)
      { this.expr = expr; }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 4; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Query
  //
  //  Represents expression 'expr?'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Query extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public Expr expr;

    //-----------------------------------------------------------------
    //  Create object with specified 'expr'.
    //-----------------------------------------------------------------
    public Query(final Expr expr)
      { this.expr = expr; }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 4; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.PlusPlus
  //
  //  Represents expression 'expr1++expr2'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class PlusPlus extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public Expr expr1;
    public Expr expr2;

    //-----------------------------------------------------------------
    //  Create object with specified 'expr1' and 'expr2'.
    //-----------------------------------------------------------------
    public PlusPlus(final Expr expr1, final Expr expr2)
      {
        this.expr1 = expr1;
        this.expr2 = expr2;
      }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 4; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.StarPlus
  //
  //  Represents expression 'expr1*+expr2'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class StarPlus extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public Expr expr1;
    public Expr expr2;

    //-----------------------------------------------------------------
    //  Create object with specified 'expr1' and 'expr2'.
    //-----------------------------------------------------------------
    public StarPlus(final Expr expr1, final Expr expr2)
      {
        this.expr1 = expr1;
        this.expr2 = expr2;
      }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }

    //-------------------------------------------------------------------
    //  Binding strength.
    //-------------------------------------------------------------------
    int bind() { return 4; }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Ref
  //
  //  Represents reference to the Rule identified by 'name'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Ref extends Expr
  {
    public Rule rule;

    //-----------------------------------------------------------------
    //  Create the object with specified name.
    //-----------------------------------------------------------------
    public Ref(final String name)
      {
        this.name = name;
        asString = name;
      }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.StringLit
  //
  //  Represents string literal.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class StringLit extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public final String s; // The string in true form.

    //-----------------------------------------------------------------
    //  Create the object with specified string.
    //-----------------------------------------------------------------
    public StringLit(final String s)
      {
        this.s = s;
        adv = true;
        fal = true;
        WF  = true;
        asString = "\"" + s + "\"";
      }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Range
  //
  //  Represents range [a-z].
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Range extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public char a;      // Range limits in true form.
    public char z;

    //-----------------------------------------------------------------
    //  Create the object with limits a-z.
    //-----------------------------------------------------------------
    public Range(char a, char z)
      {
        this.a = a;
        this.z = z;
        adv = true;
        fal = true;
        WF  = true;
        asString = "[" + a + "-" + z + "]";
      }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.CharClass
  //
  //  Represents character class [s] or ^[s].
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class CharClass extends Expr
  {
    //-----------------------------------------------------------------
    //  Data
    //-----------------------------------------------------------------
    public final String s; // The string in true form.
    public boolean hat;    // '^' present?

    //-----------------------------------------------------------------
    //  Create object with specified string and 'not'.
    //-----------------------------------------------------------------
    public CharClass(final String s, boolean hat)
      {
        this.s = s;
        this.hat = hat;
        adv = true;
        fal = true;
        WF  = true;
        asString = (hat?"^[":"[") + s + "]";
      }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }
  }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class Expr.Any
  //
  //  Represents 'any character'.
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

  public static class Any extends Expr
  {
    //-----------------------------------------------------------------
    //  Create.
    //-----------------------------------------------------------------
    public Any()
      {
        adv = true;
        fal = true;
        WF  = true;
        asString = "_";
      }

    //-----------------------------------------------------------------
    //  Accept visitor.
    //-----------------------------------------------------------------
    public void accept(Visitor v)
      { v.visit(this); }
  }
}


