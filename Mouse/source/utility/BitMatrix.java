//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2007, 2010 by Roman R. Redziejowski (www.romanredz.se).
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
//    100512 Adapted for version 1.3.
//
//=========================================================================

package mouse.utility;


import java.util.BitSet;

//=======================================================================
/**
*  A square matrix with boolean elements.
*  <br>
*  A matrix of size <code>n</code> has <code>n</code> rows
*  and <code>n</code> columns.
*  The rows and columns are numbered from <code>0</code> through <code>n-1</code>.
*  The element in row <code>i</code> and column <code>j</code> is referred to
*  as the <code>(i,j)</code>-th element.
*  <br>
*  For convenience, the values of elements are in this documentation
*  denoted by <code>0</code> (meaning <code>false</code>) and
*  <code>1</code> (meaning <code>true</code>).
*/
//=======================================================================

public class BitMatrix
{
  //--------------------------------------------------------------------
  //  BitMatrix is implemented as an array 'm' of BitSets,
  //  each BitSet representing one row.
  //--------------------------------------------------------------------
  private final int n; // Size
  private BitSet m[];  // The matrix

  //--------------------------------------------------------------------
  //  Construct incomplete n by n matrix.
  //--------------------------------------------------------------------
  private BitMatrix(int n)
    {
      this.n = n;
      m = new BitSet[n];
    }

  //--------------------------------------------------------------------
  //  empty
  //--------------------------------------------------------------------
  /**
  *  Constructs empty matrix.
  *
  *  @param  n Size of the matrix.
  *  @return An <code>n</code> by <code>n</code> matrix
  *          with all elements <code>0</code>.
  */
  public static BitMatrix empty(int n)
    {
      BitMatrix R = new BitMatrix(n);
      for (int i=0;i<n;i++) R.m[i] = new BitSet();
      return R;
    }

  //--------------------------------------------------------------------
  //  unit
  //--------------------------------------------------------------------
  /**
  *  Constructs unit matrix.
  *
  *  @param  n Size of the matrix.
  *  @return An <code>n</code> by <code>n</code> matrix
  *          with all diagonal elements <code>1</code>
  *          and remaining elements <code>0</code>.
  */
  public static BitMatrix unit(int n)
    {
      BitMatrix R = empty(n);
      for (int i=0;i<n;i++) R.m[i].set(i);
      return R;
    }

  //--------------------------------------------------------------------
  //  size
  //--------------------------------------------------------------------
  /**
  *  Obtains size of this matrix.
  *
  *  @return Number of rows / columns.
  */
  public int size()
    { return n; }

  //--------------------------------------------------------------------
  //  weight
  //--------------------------------------------------------------------
  /**
  *  Obtains number of ones in this matrix.
  *
  *  @return Number of ones.
  */
  public int weight()
    {
      int w = 0;
      for (int i=0;i<n;i++)
        w += m[i].cardinality();
      return w;
    }

  //--------------------------------------------------------------------
  //  at
  //--------------------------------------------------------------------
  /**
  *  Obtains the value of <code>(i,j)</code>-th element.
  *
  *  @param  i Row number.
  *  @param  j Column number.
  *  @return Value of the <code>(i,j)</code>-th element.
  */
  public boolean at(int i, int j)
    { return m[i].get(j); }

  //--------------------------------------------------------------------
  //  set
  //--------------------------------------------------------------------
  /**
  *  Sets the <code>(i,j)</code>-th element to <code>b</code>.
  *
  *  @param  i Row number.
  *  @param  j Column number.
  *  @param  b The value to be set.
  */
  public void set(int i, int j, boolean b)
    { m[i].set(j,b); }

  //--------------------------------------------------------------------
  //  set
  //--------------------------------------------------------------------
  /**
  *  Sets the <code>(i,j)</code>-th element to <code>1</code>.
  *
  *  @param  i Row number.
  *  @param  j Column number.
  */
  public void set(int i, int j)
    { m[i].set(j); }

  //--------------------------------------------------------------------
  //  row
  //--------------------------------------------------------------------
  /**
  *  Obtains the contents of row <code>r</code> as a BitSet.
  *
  *  @param  r Row number.
  *  @return The contents of row <code>r</code> as a BitSet.
  */
  public BitSet row(int r)
    { return (BitSet)(m[r].clone()); }

  //--------------------------------------------------------------------
  //  column
  //--------------------------------------------------------------------
  /**
  *  Obtains the contents of column <code>c</code> as a BitSet.
  *
  *  @param  c Column number.
  *  @return The contents of column <code>c</code> as a BitSet.
  */
  public BitSet column(int c)
    {
      BitSet col = new BitSet(n);
      for (int i=0;i<n;i++)
        col.set(i,m[i].get(c));
      return col;
    }

  //--------------------------------------------------------------------
  //  copy
  //--------------------------------------------------------------------
  /**
  *  Constructs a copy of this matrix.
  *
  *  @return New matrix, identical to this matrix.
  */
  public BitMatrix copy()
    {
      BitMatrix R = new BitMatrix(n);
      for (int i=0;i<n;i++) R.m[i] = (BitSet)(m[i].clone());
      return R;
    }

  //--------------------------------------------------------------------
  //  transpose
  //--------------------------------------------------------------------
  /**
  *  Constructs transpose of this matrix.
  *
  *  @return New matrix that is the transpose of this matrix.
  */
  public BitMatrix transpose()
    {
      BitMatrix R = empty(n);
      for (int i=0;i<n;i++)
        for (int j=0;j<n;j++)
          if (at(i,j)) R.set(j,i);
      return R;
    }

  //--------------------------------------------------------------------
  //  closure
  //--------------------------------------------------------------------
  /**
  *  Computes transitve closure of this matrix.
  *  The matrix is considered to represent a relation <code>R</code>
  *  within a set of <code>n</code> objects, where <code>n</code>
  *  is the size of the matrix.
  *  The resulting matrix represents the transitive closure of <code>R</code>.
  *  <br>
  *  The result is computed using Warshall's algorithm.
  *  See J-P.Tremblay and P.G.Sorenson, The Theory and Practice of Compiler Writing,
  *  page 25.
  *
  *  @return New matrix that is the transitive closure of this matrix.
  */
  public BitMatrix closure()
    {
      BitMatrix M = copy();
      for (int k=0;k<n;k++)
        for (int i=0;i<n;i++)
          if (M.at(i,k)) M.m[i].or(M.m[k]);
      return M;
    }

  //--------------------------------------------------------------------
  //  star
  //--------------------------------------------------------------------
  /**
  *  Computes transitve and reflexive closure of this matrix.
  *  (Such closure of M is often denoted by M*.)
  *
  *  @return New matrix that is the transitive and reflexive closure of this matrix.
  */
  public BitMatrix star()
    { return closure().or(unit(n)); }

  //--------------------------------------------------------------------
  //  orInto
  //--------------------------------------------------------------------
  /**
  *  Modifies a specified matrix by performing
  *  the element-by-element 'or' with this matrix.
  *
  *  @param  M A bit matrix of the same size as this.
  */
  public void orInto(final BitMatrix M)
    {
      if (M.n!=n) throw new Error("size mismatch " + M.n + "!=" + n);
      for (int i=0;i<n;i++) M.m[i].or(m[i]);
    }

  //--------------------------------------------------------------------
  //  andInto
  //--------------------------------------------------------------------
  /**
  *  Modifies a specified matrix by performing
  *  the element-by-element 'and' with this matrix.
  *
  *  @param  M A bit matrix of the same size as this.
  */
  public void andInto(final BitMatrix M)
    {
      if (M.n!=n) throw new Error("size mismatch " + M.n + "!=" + n);
      for (int i=0;i<n;i++) M.m[i].and(m[i]);
    }

  //--------------------------------------------------------------------
  //  or
  //--------------------------------------------------------------------
  /**
  *  Computes element-by-element 'or' of this matrix and the specified matrix.
  *
  *  @param  M A bit matrix of the same size as this.
  *  @return New matrix that is the element-by-element 'or'
  *          of this matrix and <code>M</code>.
  */
  public BitMatrix or(final BitMatrix M)
    {
      if (M.n!=n) throw new Error("size mismatch " + M.n + "!=" + n);
      BitMatrix R = copy();
      M.orInto(R);
      return R;
    }

  //--------------------------------------------------------------------
  //  and
  //--------------------------------------------------------------------
  /**
  *  Computes element-by-element 'and' of this matrix and the specified matrix.
  *
  *  @param  M A bit matrix of the same size as this.
  *  @return New matrix that is the element-by-element 'and'
  *          of this matrix and <code>M</code>.
  */
  public BitMatrix and(final BitMatrix M)
    {
      if (M.n!=n) throw new Error("size mismatch " + M.n + "!=" + n);
      BitMatrix R = copy();
      M.andInto(R);
      return R;
    }

  //--------------------------------------------------------------------
  //  times
  //--------------------------------------------------------------------
  /**
  *  Computes product of this matrix and the specified matrix.
  *  The product is defined as for numeric matrices, with logical 'or'
  *  instead of addition and logical 'and' instead of multiplication.
  *
  *  @param  M A bit matrix of the same size as this.
  *  @return New matrix that is the product
  *          of this matrix and <code>M</code>.
  */
  public BitMatrix times(final BitMatrix M)
    {
      if (M.n!=n) throw new Error("size mismatch " + M.n + "!=" + n);
      BitMatrix R = empty(n);
      BitMatrix T = M.transpose();
      for (int i=0;i<n;i++)
        for (int j=0;j<n;j++)
          if (m[i].intersects(T.m[j])) R.set(i,j);
      return R;
    }

  //--------------------------------------------------------------------
  //  times
  //--------------------------------------------------------------------
  /**
  *  Computes product of this matrix and the specified vector.
  *  The product is defined as for numeric matrices, with logical 'or'
  *  instead of addition and logical 'and' instead of multiplication.
  *
  *  @param  V A bit vector of the same size as this.
  *  @return New matrix that is the product
  *          of this matrix and <code>V</code>.
  */
  public BitSet times(final BitSet V)
    {
      BitSet R = new BitSet(n);
      for (int i=0;i<n;i++)
        if (m[i].intersects(V)) R.set(i);
      return R;
    }

  //--------------------------------------------------------------------
  //  product
  //--------------------------------------------------------------------
  /**
  *  Computes n by n matrix as the Crtesian product of two vectors.
  *
  *  @param  V1 A bit vector.
  *  @param  V2 A bit vector.
  *  @param  n  Dimension of the result.
  *  @return New matrix that is the product of <code>V1</code>
  *          and <code>V2</code>.
  */
  public static BitMatrix product(final BitSet V1, final BitSet V2, int n)
    {
      BitMatrix M = new BitMatrix(n);
      for (int i=0;i<n;i++)
        if (V1.get(i)) M.m[i] = (BitSet)(V2.clone());
        else M.m[i] = new BitSet(n);
      return M;
    }

  //--------------------------------------------------------------------
  //  insert
  //--------------------------------------------------------------------
  /**
  *  Replaces a square area of this matrix by the contents of
  *  another matrix.
  *
  *  @param  M The matrix to be inserted.
  *  @param  i starting row of the area to be replaced.
  *  @param  j starting column of the area to be replaced.
  *  @return This matrix with modified contents.
  */
  public BitMatrix insert(BitMatrix M, int i, int j)
    {
      if (i+M.n>n || j+M.n>n)
        throw new Error("Insertion overflow");
      for (int r=0;r<M.n;r++)
      {
        BitSet src = M.m[r];
        BitSet trg = m[i+r];
        for (int c=0;c<M.n;c++)
          trg.set(c+j,src.get(c));
      }
      return this;
    }

  //--------------------------------------------------------------------
  //  cut
  //--------------------------------------------------------------------
  /**
  *  Returns a square matrix cut out from this matrix.
  *
  *  @param  s Size of the resulting matrix.
  *  @param  i starting row of the area to be cut.
  *  @param  j starting column of the area to be cut.
  *  @return New <code>n</code> by <code>n</code> matrix.
  */
  public BitMatrix cut(int s, int i, int j)
    {
      if (s<=0 || s>n)
        throw new Error("s = " + s);
      if (i+s>n || j+s>n)
        throw new Error("Cut overflow");
      BitMatrix M = empty(s);
      for (int r=0;r<s;r++)
      {
        BitSet src = m[i+r];
        BitSet trg = M.m[r];
        for (int c=0;c<s;c++)
          trg.set(c,src.get(c+j));
      }
      return M;
    }

  //--------------------------------------------------------------------
  //  show
  //--------------------------------------------------------------------
  /**
  *  Writes this matrix to <code>System.out</code>.
  */
  public void show()
    {
      for (int i=0;i<n;i++)
      {
        StringBuffer sb = new StringBuffer();
        for (int j=0;j<n;j++)
          sb.append(this.at(i,j)? 1:0).append(" ");
        System.out.println(sb);
      }
    }

  //--------------------------------------------------------------------
  //  Test
  //--------------------------------------------------------------------

  public static void main(String argv[])
    {
      BitMatrix P = empty(4);

      P.set(0,1);
      P.set(1,2);
      P.set(2,3);

      BitMatrix Q = unit(6);
      Q.insert(P,1,2);
      Q.show();

      Q.cut(4,1,2).show();

      System.out.println("\nP:");
      P.show();
      System.out.println("\nweight of P = " + P.weight());

      System.out.println("\ncopy of P:");
      P.copy().show();

      System.out.println("\nP times P:");
      P.times(P).show();

      System.out.println("\nclosure of P:");
      P.closure().show();

      System.out.println("\nstar of P:");
      P.star().show();

      System.out.println("\ntranspose of P:");
      BitMatrix R = P.transpose();
      R.show();

      System.out.println("\nP and closure of P:");
      P.and(P.closure()).show();

      System.out.println("\nP or transpose of P:");
      P.or(R).show();

      System.out.println("\nunit(3):");
      unit(3).show();

      BitSet V = new BitSet();
      V.set(1);
      V.set(2);

      BitSet W = new BitSet();
      W.set(0);
      W.set(3);

      System.out.println("\nV:");
      System.out.println(V);

      System.out.println("\nW:");
      System.out.println(W);

      System.out.println("\nP times V:");
      System.out.println(P.times(V));

      System.out.println("\nV product W:");
      product(V,W,4).show();
    }
 }