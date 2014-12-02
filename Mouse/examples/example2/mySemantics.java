
// Example 2

class mySemantics extends mouse.runtime.SemanticsBase
{
  //-------------------------------------------------------------------
  //  Sum = Number "+" Number ... "+" Number !_
  //           0    1     2       n-2   n-1
  //-------------------------------------------------------------------
  void sum()
    {
      int s = 0;
      for (int i=0;i<rhsSize();i+=2)
        s += (Integer)rhs(i).get();
      System.out.println(s);
    }

  //-------------------------------------------------------------------
  //  Number = [0-9] ... [0-9]
  //             0        n-1
  //-------------------------------------------------------------------
  void number()
    { lhs().put(Integer.valueOf(lhs().text())); }
}
