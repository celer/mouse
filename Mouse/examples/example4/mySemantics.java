
// Example 4

class mySemantics extends mouse.runtime.SemanticsBase
{
  //-------------------------------------------------------------------
  //  Sum = Space Sign Number AddOp Number ... AddOp Number
  //          0     1    2      3     4         n-2   n-1
  //-------------------------------------------------------------------
  void sum()
    {
      int n = rhsSize();
      double s = (Double)rhs(2).get();
      if (!rhs(1).isEmpty()) s = -s;
      for (int i=4;i<n;i+=2)
      {
        if (rhs(i-1).charAt(0)=='+')
          s += (Double)rhs(i).get();
        else
          s -= (Double)rhs(i).get();
      }
      System.out.println(s);
    }

  //-------------------------------------------------------------------
  //  Number = Digits? "."  Digits Space
  //              0    1(0)  2(1)   3(2)
  //-------------------------------------------------------------------
  void fraction()
    { lhs().put(Double.valueOf(rhsText(0,rhsSize()-1))); }

  //-------------------------------------------------------------------
  //  Number = Digits Space
  //              0     1
  //-------------------------------------------------------------------
  void integer()
    { lhs().put(Double.valueOf(rhs(0).text())); }

}
