package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

public class ReverseBsim {

    public static void main(final String[] args) throws IOException, InterruptedException {
        ParseTS parseTS = new ParseTS();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        Set<TS> sTS = new HashSet<>();
        File outputFolder = new File("./generated/");
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
        boolean nd=false;
        TS fin = parseTS.compose(sTS);
       
        for (String y : fin.getChannels()) {
            for (State st : fin.getStates()) {
                Set<Trans> det = st.getTrans().stream().filter(tr -> tr.getAction().equals(y)).collect(Collectors.toSet());
                System.err.println(det);
                if (det.size()>1) {
                   
                   nd=true;
                }
            }
            
        }
        System.err.println("nondeterminism detected"+nd); 
        fin.toDot();
    }
}
