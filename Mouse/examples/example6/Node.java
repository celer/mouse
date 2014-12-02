
abstract class Node
{
  abstract String show();

  static class Num extends Node
  {
    final String number;

    Num(final String v)
      { number = v; }

    String show()
      { return number; }
  }

  static class Op extends Node
  {
    final Node leftArg;
    final char operator;
    final Node rightArg;

    Op(final Node left, char op, final Node right)
      {
        leftArg = left;
        operator = op;
        rightArg = right;
      }

    String show()
      { return "[" + leftArg.show() + operator + rightArg.show() + "]" ; }
  }
}