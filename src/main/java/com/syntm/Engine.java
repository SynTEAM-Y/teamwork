package com.syntm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Engine {
   public Engine() {
   }

   public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException {
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
      String exit = "";

      while(!exit.equals("x")) {
         System.out.println("");
         System.out.println("\u001b[32mSelect a Problem to solve\u001b[0m");
         Set<String> choice = new HashSet<>(Arrays.asList("1", "2", "x"));
         System.out.println("\u001b[32m[1] :  Distribute and Reconfigure TS? \u001b[0m");
         System.out.println("\u001b[32m[2] :  LTL Distributed Synthesis? \u001b[0m");
         System.out.println("\u001b[32m[x] :  Exit? \u001b[0m");
         String in = "";

         for(in = stdin.readLine(); !choice.contains(in.toString()); in = stdin.readLine()) {
            System.out.println("\u001b[31mOption does not exist!\u001b[0m");
            System.out.println("\u001b[31mTry again \u001b[0m");
         }

         switch (in.toString()) {
            case "1":
               ParseTS.main();
               break;
            case "2":
               System.out.println(Defs.ANSI_RED +"Make sure to have docker up and running. We use the Strix synthesis engine for the initial single-agent synthesis"+ Defs.ANSI_RESET);

               System.out.println(Defs.ANSI_RED +"First use, it takes time to load the docker container"+ Defs.ANSI_RESET);
               RunEngine.main();
               break;
            case "x":
               exit = "x";
         }
      }

   }
}
