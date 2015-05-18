/*
author: Xueting Pi
CSS 430 Program 1 - Shell.java
4/15/2015
*/
import java.util.*;
import java.util.List;
import java.util.ArrayList;

class Shell extends Thread
{
   //command line number 
   private int cmdLine;
   boolean inProcess = true;
   static final int ERROR = -1;
   private static final String LINE_BREAK = System.getProperty("line.separator");

   // constructor for shell
   public Shell( ) {
      cmdLine = 0;
   }

   // execute single command and empty the list containing that command
   public void execution(List<String> singleCommand, int count)
   {
      // output the class name
      SysLib.cout(singleCommand.get(0) + LINE_BREAK);
      // convert arraylist to an array of string
      String[] strArray = (String[]) singleCommand.toArray(new String[count]);
      if (SysLib.exec(strArray) == ERROR )
      {
         SysLib.cerr(strArray[0] + " failed in loading\n");
      }
   }

   // required run method for this Shell Thread
   public void run( ) {
      
      while (inProcess)
      {
       //  SysLib.cout(System.getProperty("line.separator"));

         cmdLine ++;      // shell number
         String cmd = ""; // commands user typed

         // read in multiple commands typed by user
         do {
            StringBuffer inputBuf = new StringBuffer();
            SysLib.cout("shell[" + cmdLine + "]% ");
            SysLib.cin(inputBuf);
            cmd = inputBuf.toString();
         } while (cmd.length() == 0);

         // store space-delimited strings into a string array
         String[] args = SysLib.stringToArgs(cmd);

         // use arrayList to store each command separated by & or ;
         List<String> singleCmd = new ArrayList<String>();
         int count = 0; // keep track of the size of each command 

         // loop over each element in the string array
         for (int i = 0; i < args.length; i++)
         {
            if (args[i].equals("exit"))
            {
               // exit out of both for loop and while loop
               SysLib.exit();
               inProcess = false;
               break;
            }

            if (args[i].equals("&") && !singleCmd.isEmpty())
            {
               // execute the one command before &
               execution(singleCmd,count);
               // empty the single command list for next command
               singleCmd = new ArrayList<String>();
               count = 0;
            }
            else if (args[i].equals(";") && !singleCmd.isEmpty())
            {
               // execute the one command before ;
               execution(singleCmd,count);
               SysLib.join();
               // empty the arraylist for next command and reset count 
               singleCmd = new ArrayList<String>();
               count = 0;
            }
            else
            {
               singleCmd.add(args[i]);
               count++;
            }
         } 

         // if the command does not end with a & or ; execute that last command 
         if (!singleCmd.isEmpty())
         {
               execution(singleCmd,count);
               SysLib.join();
         }
      }
   }
}
