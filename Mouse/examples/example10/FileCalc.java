
import mouse.runtime.SourceFile;

class FileCalc
{
  public static void main(String argv[])
    {
      myParser parser = new myParser();          // Instantiate Parser+Semantics
      SourceFile src = new SourceFile(argv[0]);  // Wrap the file
      if (!src.created()) return;                // Return if no file
      boolean ok = parser.parse(src);            // Apply parser to it
      System.out.println(ok? "ok":"failed");     // Tell if succeeded
    }
}
