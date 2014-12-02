
// Example 3

class mySemantics extends mouse.runtime.SemanticsBase
{
  //-------------------------------------------------------------------
  //  Sum = Space Sign Number AddOp Number ... AddOp Number
  //          0     1    2      3     4         n-2   n-1
  //-------------------------------------------------------------------
  void sum()
    {
      int n = rhsSize();
      int s = (Integer)rhs(2).get();
      if (!rhs(1).isEmpty()) s = -s;
      for (int i=4;i<n;i+=2)
      {
        if (rhs(i-1).charAt(0)=='+')
          s += (Integer)rhs(i).get();
        else
          s -= (Integer)rhs(i).get();
      }
      System.out.println(s);
    }

  //-------------------------------------------------------------------
  //  Number = Digits Space
  //             0      1
  //-------------------------------------------------------------------
  void number()
    { lhs().put(Integer.valueOf(rhs(0).text())); }

}
