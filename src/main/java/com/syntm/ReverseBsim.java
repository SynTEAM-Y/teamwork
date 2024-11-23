package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.syntm.lts.TS;

public class ReverseBsim {

    public static void main(final String[] args) throws IOException, InterruptedException {
        ParseTS parseTS = new ParseTS();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        Set<TS> sTS = new HashSet<>();
        File outputFolder = new File("./generated/output/");
        FileUtils.cleanDirectory(outputFolder);

        System.out.println(Defs.ANSI_GREEN + "Enter a TS for composition: " + Defs.ANSI_RESET);
        String exit = "";
        exit = stdin.readLine();
        TS mainTS = new TS("MainTS");
        System.err.println(exit);
        mainTS.parseDot(exit);
        mainTS.toDot();
        sTS.add(mainTS);
        while (!exit.equals("x")) {
            System.out
                    .println(Defs.ANSI_GREEN + "File \"" + exit + "\" is imported, enter a new one " + Defs.ANSI_RESET);

            try {
                exit = stdin.readLine();
                if (!exit.equals("x")) {
                    TS main = new TS("MainTS");
                    main.parseDot(exit);
                    main.toDot();
                    sTS.add(main);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        TS fin = parseTS.compose(sTS);
        fin.toDot();
    }
}
