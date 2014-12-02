
//===========================================================================
//
//  Semantics to handle typedefs in PEG for C
//  compatible with Mouse 1.1 - 1.4.
//
//---------------------------------------------------------------------------
//
//  Copyright (C) 2007, 2009, 2010 by Roman R Redziejowski (www.romanredz.se).
//
//  The author gives unlimited permission to copy and distribute
//  this file, with or without modifications, as long as this notice
//  is preserved, and any changes are properly documented.
//
//  This file is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//---------------------------------------------------------------------------
//
//  See comment in 'C.peg' for overview of typedef handling.
//
//---------------------------------------------------------------------------
//
//  Change log
//    2009-07-13 Posted on Internet.
//    2009-08-16 Updated for Mouse 1.1.
//
//===========================================================================

import java.util.Vector;
import java.util.HashSet;

class C extends mouse.runtime.SemanticsBase
{
  //=======================================================================
  //
  //  Typedef table
  //
  //=======================================================================

  HashSet<String> typedefs;


  //-------------------------------------------------------------------
  //  Initialization.
  //  This method is called before the processing of each input file.
  //-------------------------------------------------------------------
  public void init()
    { typedefs = new HashSet<String>(); }


  //=======================================================================
  //
  //  Semantic actions
  //
  //=======================================================================
  //-------------------------------------------------------------------
  //  Declaration = DeclarationSpecifiers InitDeclaratorList? SEMI
  //                         0                    1           1/2
  //-------------------------------------------------------------------
  void Declaration()
    {
      // If InitDeclaratorList is present and DeclarationSpecifiers
      // contain "typedef", copy all Identifiers delivered
      // by InitDeclaratorList into typedefs table.

      if (rhsSize()==1 || rhs(0).get()==null) return;
      Vector<String> iList = (Vector<String>)(rhs(1).get());
        for (String s: iList)
          typedefs.add(s);
    }


  //-------------------------------------------------------------------
  //  DeclarationSpecifiers = (StorageClassSpecifier / TypeQualifier /
  //    FunctionSpecifier)* TypedefName (StorageClassSpecifier /
  //    TypeQualifier / FunctionSpecifier)*
  //  DeclarationSpecifiers = (StorageClassSpecifier / TypeSpecifier /
  //    TypeQualifier / FunctionSpecifier)+
  //-------------------------------------------------------------------
  void DeclarationSpecifiers()
    {
      // This semantic action is called by both alternatives
      // of DeclarationSpecifiers.
      // Scan all Specifiers and return semantic value "typedef"
      // if any of them is "typedef".

      lhs().put(null);
      for (int i=0;i<rhsSize();i++)
        if (rhs(i).text().length()>=7
            && rhs(i).text().substring(0,7).equals("typedef"))
        {
          lhs().put("typedef");
          return;
        }
    }


  //-------------------------------------------------------------------
  //  InitDeclaratorList = InitDeclarator (COMMA InitDeclarator)*
  //                              0        1,3,..     2,4,..
  //-------------------------------------------------------------------
  void InitDeclaratorList()
    {
      // Build Vector of Identifiers delivered by InitDeclarators
      // and return it as semantic value.

      Vector<String> iList = new Vector<String>();
      for (int i=0;i<rhsSize();i+=2)
        iList.add((String)rhs(i).get());
      lhs().put(iList);
    }


  //-------------------------------------------------------------------
  //  InitDeclarator = Declarator (EQU Initializer)?
  //                        0       1       2
  //-------------------------------------------------------------------
  void InitDeclarator()
    {
      // Return as semantic value the Identifier delivered by Declarator.

      lhs().put(rhs(0).get());
    }


  //-------------------------------------------------------------------
  //  Declarator = Pointer? DirectDeclarator
  //                  0          n-1
  //-------------------------------------------------------------------
  void Declarator()
    {
      // Return as semantic value the Identifier delivered
      // by DirectDeclarator.

      lhs().put(rhs(rhsSize()-1).get());
    }


  //-------------------------------------------------------------------
  //  DirectDeclarator = (Identifier / LPAR Declarator RPAR) ... etc.
  //                          0          0       1       2
  //-------------------------------------------------------------------
  void DirectDeclarator()
    {
      // Return as semantic value either the Identifier appearing on the rhs,
      // or the Identifier delivered by DirectDeclarator.

      if (rhs(0).isA("LPAR")) lhs().put(rhs(1).get());
      else lhs().put(rhs(0).get());
    }


  //-------------------------------------------------------------------
  //  TypedefName = Identifier
  //                     0
  //-------------------------------------------------------------------
  boolean TypedefName()
    {
      // Return true if the Identifier appears in the typedefs table;
      // otherwise return false.

      return typedefs.contains((String)rhs(0).get());
    }


  //-------------------------------------------------------------------
  //  Identifier = !Keyword IdNondigit IdChar* Spacing?
  //                            0       1,2,..   n-1
  //-------------------------------------------------------------------
  void Identifier()
    {
      // Return as semantic value the String specified as Identifier.

      lhs().put(rhsText(0,rhsSize()-1));
    }

}
