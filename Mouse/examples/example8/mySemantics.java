
// Example 8

import java.util.Hashtable;

class mySemantics extends mouse.runtime.SemanticsBase
{
  //-------------------------------------------------------------------
  //  Dictionary
  //-------------------------------------------------------------------
  private Hashtable<String,Double> dictionary = new Hashtable<String,Double>();

  //-------------------------------------------------------------------
  //  Store = Name Space Equ Sum
  //            0    1    2   3
  //-------------------------------------------------------------------
  void store()
    { dictionary.put(rhs(0).text(),(Double)rhs(3).get()); }

  //-------------------------------------------------------------------
  //  Print = Sum
  //           0
  //-------------------------------------------------------------------
  void print()
    { System.out.println((Double)rhs(0).get()); }

  //-------------------------------------------------------------------
  //  Sum = Sign Product AddOp Product ... AddOp Product
  //          0     1      2      3         n-2    n-1
  //-------------------------------------------------------------------
  void sum()
    {
      int n = rhsSize();
      double s = (Double)rhs(1).get();
      if (!rhs(0).isEmpty()) s = -s;
      for (int i=3;i<n;i+=2)
      {
        if (rhs(i-1).charAt(0)=='+')
          s += (Double)rhs(i).get();
        else
          s -= (Double)rhs(i).get();
      }
       lhs().put(new Double(s));
    }

  //-------------------------------------------------------------------
  //  Product = Factor MultOp Factor ... MultOp Factor
  //               0     1      2         n-2     n-1
  //-------------------------------------------------------------------
  void product()
    {
      int n = rhsSize();
      double s = (Double)rhs(0).get();
      for (int i=2;i<n;i+=2)
      {
        if (!rhs(i-1).isEmpty() && rhs(i-1).charAt(0)=='/')
          s /= (Double)rhs(i).get();
        else
          s *= (Double)rhs(i).get();
      }
      lhs().put(new Double(s));
    }

  //-------------------------------------------------------------------
  //  Factor = Digits? "."  Digits Space
  //              0    1(0)  2(1)   3(2)
  //-------------------------------------------------------------------
  void fraction()
    { lhs().put(Double.valueOf(rhsText(0,rhsSize()-1))); }

  //-------------------------------------------------------------------
  //  Factor = Digits Space
  //              0     1
  //-------------------------------------------------------------------
  void integer()
    { lhs().put(Double.valueOf(rhs(0).text())); }

  //-------------------------------------------------------------------
  //  Factor = Lparen Sum Rparen
  //              0    1    2
  //-------------------------------------------------------------------
  void unwrap()
    { lhs().put(rhs(1).get()); }

  //-------------------------------------------------------------------
  //  Factor = Name Space
  //             0    1
  //-------------------------------------------------------------------
  void retrieve()
    {
      String name = rhs(0).text();
      Double d = dictionary.get(name);
      if (d==null)
      {
        d = (Double)Double.NaN;
        System.out.println
          (rhs(0).where(0) + ": '" + name + "' is not defined");
      }
      lhs().put(d);
    }

  //-------------------------------------------------------------------
  //  Space = " "*
  //-------------------------------------------------------------------
  void space()
    { lhs().errClear(); }
}
