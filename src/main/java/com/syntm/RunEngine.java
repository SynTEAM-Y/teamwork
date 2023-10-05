package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.syntm.analysis.Partitioning;
import com.syntm.lts.Mealy;
import com.syntm.lts.Spec;
import com.syntm.lts.State;
import com.syntm.lts.TS;

public class RunEngine {
	private TS mainTS;

	public static void main(final String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		RunEngine parseTS = new RunEngine();
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

		Spec spec = new Spec();
		System.out.println("Enter SPEC: ");
		String specfile = stdin.readLine();
		spec.specReader(specfile);
		String command = spec.toString();
		ProcessBuilder p = new ProcessBuilder("docker", "run", "lazkany/strix", "-f", command, spec.inParam(),
				spec.outParam(), "--k");

		File Strategy = new File("Mealy");
		p.redirectErrorStream(true);
		p.redirectOutput(Redirect.appendTo(Strategy));
		Process proc = p.start();
		proc.waitFor();

		Mealy m = new Mealy("");
		m = m.kissToMealy("Mealy");
		TS TM = m.toTS(m, m.getName());
		Strategy.delete();

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
