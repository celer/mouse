
    import mouse.runtime.SourceString;
    import java.io.BufferedReader;
    import java.io.InputStreamReader;

    class Calc
    {
      public static void main(String argv[])
        throws Exception
        {
          BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
          myParser parser = new myParser();            // Instantiate Parser+Semantics
          while (true)
          {
            System.out.print("> ");                    // Print prompt
            String line = in.readLine();               // Read line
            if (line.length()==0) return;              // Quit on empty line
            SourceString src = new SourceString(line); // Wrap up the line
            parser.parse(src);                         // Apply Parser to it
          }
        }
    }
