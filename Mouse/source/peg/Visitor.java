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
//    090701 License changed by the author to Apache v.2.
//   Version 1.4
//    110920 Added subclasses 'PlusPlus' and 'StarPlus'.
//
//=========================================================================

package mouse.peg;

//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Visitor
//
//-------------------------------------------------------------------------
//
//  Base class for visitors processing the grammar built by PEG parser.
//  The reason for using base class rather than interface is that
//  many visitor methods are empty. These methods thus need not be defined
//  in concrete visitors.
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

public class Visitor
{
  public void visit(Expr.Rule expr) {}
  public void visit(Expr.Choice expr) {}
  public void visit(Expr.Sequence expr) {}
  public void visit(Expr.And expr) {}
  public void visit(Expr.Not expr) {}
  public void visit(Expr.Plus expr) {}
  public void visit(Expr.Star expr) {}
  public void visit(Expr.Query expr) {}
  public void visit(Expr.PlusPlus expr) {}
  public void visit(Expr.StarPlus expr) {}
  public void visit(Expr.Ref expr) {}
  public void visit(Expr.StringLit expr) {}
  public void visit(Expr.CharClass expr) {}
  public void visit(Expr.Range expr) {}
  public void visit(Expr.Any expr) {}
}

