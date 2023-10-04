package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashSet;
import java.util.Set;

import com.syntm.analysis.Partitioning;
import com.syntm.lts.*;

public class RunEngine {
	private TS mainTS;

	public static void main(final String[] args) throws IOException, InterruptedException {
		RunEngine parseTS = new RunEngine();
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

		ProcessBuilder p = new ProcessBuilder("docker", "run", "lazkany/strix", "-f",
				"(X G(!request_0 | !request_1) &\n" + 
						"X G(request_0 | request_1) &\n" + 
						"!request_0 &\n" + 
						"!request_1) ->  (X G ((grant_0 & G !request_0) -> (F !grant_0)) &\n" + 
						"X G ((grant_1 & G !request_1) -> (F !grant_1)) &\n" + 
						"X G ((grant_0 & X (!request_0 & !grant_0)) -> X (request_0 R !grant_0)) &\n" + 
						"X G ((grant_1 & X (!request_1 & !grant_1)) -> X (request_1 R !grant_1)) &\n" + 
						"X G (!grant_0 | !grant_1) &\n" + 
						"X (request_0 R !grant_0) &\n" + 
						"X (request_1 R !grant_1) &\n" + 
						"X G (request_0 -> F grant_0) &\n" + 
						"X G (request_1 -> F grant_1) &\n" + 
						"!grant_0 &\n" + 
						"!grant_1)",
				"--ins=request_0,request_1", "--outs=grant_0,grant_1", "--k");

	
		File Strategy = new File("Mealy");
		p.redirectErrorStream(true);
		p.redirectOutput(Redirect.appendTo(Strategy));
		p.start();
		
		Mealy m= new Mealy("");
		m.kissToMealy("Mealy");
		//Strategy.delete();

		System.out.println("Enter a TS for decomposition: ");
		String fileP = stdin.readLine();
		fileP = parseTS.checkFileName(fileP);
		parseTS.readInput(fileP);
		parseTS.writeOutput("Ex/TStext");

	}

	public void readInput(final String fileP) {
		try {
			this.mainTS = TS.parse(fileP);

			mainTS.toDot(mainTS, "Main TS");
			for (TS a : this.mainTS.getAgents()) {
				a.toDot(a, a.getName());
			}

			Set<TS> sTS = new HashSet<TS>();

			for (TS p : this.mainTS.getParameters()) {
				Partitioning lp = new Partitioning(p, mainTS.getAgentById(p.getName()));
				sTS.add(lp.computeCompressedTS());
			}
			// Create Dummy TS for compostion (The Id of ||)
			TS t = new TS("");
			State s = new State("in");
			t.addState(s);
			t.setInitState("in");

			for (TS ts : sTS) {

				t = t.openParallelCompTS(t, ts);
			}
			t.toDot(t, t.getName());
			t = t.prunedTS(t);
			t.toDot(t, t.getName());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String checkFileName(String fileName) {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			if (fileName != null && new File(fileName).exists()) {
				break;
			}

			System.out.println("File \"" + fileName + "\" not found, try again: ");

			try {
				fileName = stdin.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return fileName;
	}

	public void writeOutput(final String filename) {
		try {
			FileWriter out = new FileWriter(new File(filename));

			out.write(this.mainTS.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
