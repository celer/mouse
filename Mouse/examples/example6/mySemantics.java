
// Example 6

class mySemantics extends mouse.runtime.SemanticsBase
{
  //-------------------------------------------------------------------
  //  Result
  //-------------------------------------------------------------------
  Node tree;

  //-------------------------------------------------------------------
  //  Input = Space Sum !_
  //            0    1
  //-------------------------------------------------------------------
  void result()
    { tree = (Node)rhs(1).get(); }

  //-------------------------------------------------------------------
  //  Sum = Sign Product AddOp Product ... AddOp Product
  //          0     1      2      3         n-2    n-1
  //-------------------------------------------------------------------
  void sum()
    {
      int n = rhsSize();
      Node N = (Node)rhs(1).get();
      if (!rhs(0).isEmpty())
        N = new Node.Op(new Node.Num("0"),'-',N);
      for (int i=3;i<n;i+=2)
        N = new Node.Op(N,rhs(i-1).charAt(0),(Node)rhs(i).get());
      lhs().put(N);
    }

  //-------------------------------------------------------------------
  //  Product = Factor MultOp Factor ... MultOp Factor
  //               0     1      2         n-2     n-1
  //-------------------------------------------------------------------
  void product()
    {
      int n = rhsSize();
      Node N = (Node)rhs(0).get();
      for (int i=2;i<n;i+=2)
      {
        char op = (!rhs(i-1).isEmpty() && rhs(i-1).charAt(0)=='/')? '/' : '*';
        N = new Node.Op(N,op,(Node)rhs(i).get());
      }
      lhs().put(N);
    }

  //-------------------------------------------------------------------
  //  Factor = Digits? "."  Digits Space
  //              0    1(0)  2(1)   3(2)
  //-------------------------------------------------------------------
  void fraction()
    { lhs().put(new Node.Num(rhsText(0,rhsSize()-1))); }

  //-------------------------------------------------------------------
  //  Factor = Digits Space
  //              0     1
  //-------------------------------------------------------------------
   void integer()
     { lhs().put(new Node.Num(rhs(0).text())); }

  //-------------------------------------------------------------------
  //  Factor = Lparen Sum Rparen
  //              0    1    2
  //-------------------------------------------------------------------
  void unwrap()
    { lhs().put(rhs(1).get()); }
}
