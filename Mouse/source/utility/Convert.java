//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009, 2010 by Roman R. Redziejowski (www.romanredz.se).
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
//    090405 Removed \b and \f from conversions.
//           Changed limits for Unicode representation.
//           Removed main.
//    090701 License changed by the author to Apache v.2.
//    090807 More pragmatic version.
//   Version 1.3
//    101202 Implement new character representation policy.
//
//=========================================================================

package mouse.utility;

//=======================================================================
/**
*  Container for conversion utilities.
*  They support the following policy for character representation.
*  1. Input file for the generator (the PEG) uses default encoding.
*  2. Input for the generated parser wrapped in SourceFile uses
*     default encoding. The user may modify SourceFile or provide
*     own wrapper to use another encoding.
*  3. The source of the generated parser uses ASCII (codes 32-126)
*     with LF control character for line termination.
*     The non-ASCII characters may appear in PEG within terminal
*     definitions and DiagNames. They are converted to Java escapes
*     when generated as Java literals.
*  4. Within the generated parser, all characters of terminal
*     definitions and DiagNames are represented by themselves.
*  5. The exception to above rule are comments that may contain
*     characters in the range 32-255. All other characters are
*     represented by Java escapes. The sequence '/u' that does not
*     start a Unicode escape is replaced by '/ u'. (This is needed
*     because Java does Unicode preprocessing even within comments.)
*  6. Printed messages use default encoding. They may contain characters
*     in the range 32-255 plus LF to start new line.
*     All other characters are represented by Java escapes.
*/
//=======================================================================

public class Convert
{
  //-------------------------------------------------------------------
  //  toCharLit
  //-------------------------------------------------------------------
  /**
  *  Converts character to (unquoted) Java character literal
  *  representing that character in ASCII encoding.
  *  A character outside ASCII is converted to a Java escape.
  *  In addition, a character literal must not be " or \.
  *  These characters are also escaped.
  *
  *  @param  c the character.
  *  @return literal representaton of c.
  */
  public static String toCharLit(char c)
    {
      switch(c)
      {
        case '\\': return("\\\\");
        case '\'': return("\\\'");
        default  : return(toRange(c,32,126));
      }
    }

  //-------------------------------------------------------------------
  //  toStringLit
  //-------------------------------------------------------------------
  /**
  *  Converts character string to (unquoted) Java string literal
  *  representing that string in ASCII encoding.
  *  Characters outside ASCII are converted to Java escapes.
  *  In addition, a string literal must not contain " or \.
  *  These characters are also escaped.
  *
  *  @param  s the string.
  *  @return literal representaton of s.
  */
  public static String toStringLit(final String s)
    {
      StringBuilder sb = new StringBuilder();
      for (int i=0;i<s.length();i++)
      {
        char c = s.charAt(i);

        switch(c)
        {
          case '"' : sb.append("\\\""); continue;
          case '\\': sb.append("\\\\"); continue;
          default  : sb.append(toRange(c,32,126)); continue;
        }
      }
      return sb.toString();
    }

  //-------------------------------------------------------------------
  //  toPrint
  //-------------------------------------------------------------------
  public static String toPrint(final String s)
  /**
  *  Converts string to a printable / readable form.
  *  Characters outside the range 32-255 are replaced by Java escapes.
  *
  *  @param  s the string.
  *  @return printable representaton of s.
  */
    {
      StringBuilder sb = new StringBuilder();

      for (int i=0;i<s.length();i++)
        sb.append(toRange(s.charAt(i),32,255));

      return sb.toString();
    }

  //-------------------------------------------------------------------
  //  toComment
  //-------------------------------------------------------------------
  public static String toComment(final String s)
  /**
  *  Converts string to a form that can be generated as comment.
  *  Java processes unicodes before recognizing comments.
  *  A 'backslash u' in comment not followed by hex digits
  *  is signaled as error. It is replaced by '\ u'
  *  and then the result converted to printable.
  *
  *  @param  s the string.
  *  @return comment representaton of s.
  */
    {
      String temp = s;
      return toPrint(temp.replace("\\u","\\ u"));
    }

  //-------------------------------------------------------------------
  //  toRange
  //-------------------------------------------------------------------
  private static String toRange(char c,int low,int high)
  /**
  *  If 'c' is outside the range 'low' through 'high' (inclusive),
  *  return its representation as Java escape.
  *  Otherwise return 'c' as a one-character string.
  *
  *  @param  c the character.
  *  @return Representaton of c within the range.
  */
    {
      switch(c)
      {
        case '\b': return("\\b");
        case '\f': return("\\f");
        case '\n': return("\\n");
        case '\r': return("\\r");
        case '\t': return("\\t");
        default:
          if (c<low || c>high)
          {
            String u = "000" + Integer.toHexString(c);
            return("\\u" + u.substring(u.length()-4,u.length()));
          }
          else return Character.toString(c);
      }
    }

 }