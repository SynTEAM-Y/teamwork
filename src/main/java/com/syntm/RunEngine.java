package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.syntm.analysis.Partitioning;
import com.syntm.lts.Int;
import com.syntm.lts.Mealy;
import com.syntm.lts.Spec;
import com.syntm.lts.State;
import com.syntm.lts.TS;

public class RunEngine {
	private TS mainTS;

	public static void main(final String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		RunEngine parse = new RunEngine();
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		Spec spec = new Spec();
		System.out.println("Enter SPEC: ");
		String specfile = stdin.readLine();
		specfile = parse.checkFileName(specfile);
		spec.specReader(specfile);
		String command = spec.toString();
		System.out.println(command);
		ProcessBuilder p = new ProcessBuilder("docker", "run", "lazkany/strix", "-f", command, spec.inParam(),
				spec.outParam(), "--k");

		File Strategy = new File("Mealy");
		if (Strategy.exists()) {
			FileOutputStream fos = new FileOutputStream(Strategy, false);
		}
		p.redirectErrorStream(true);
		p.redirectOutput(Redirect.to(Strategy));
		Process proc = p.start();
		proc.waitFor();

		Mealy m = new Mealy("");
		m.kissToMealy("Mealy", spec.getcCode(), spec.getoCode());		
		
		m.toDot(m, m.getName());
		if (m.getStatus().equals("REALIZABLE")) {
			TS ts = m.toTS(m, m.getName());
			if (!ts.getStatus().equals("ND")) {
				System.out.println(
                            "\n\n Specification is REALIZABLE for a multi-agent\n\n You will get a distributed Implementation :)\n\n");
				parse.processInput(ts, spec);
			}
			else
			{
				System.out.println("Despite the realizablity of the specification on a single machine\n\n it cannot be realized in our distributed model\n\n The translation to TS resulted in a non-determinstic TS.");
			}
			
		}
		//Strategy.delete();

	}

	public void processInput(TS ts, Spec spec) {
		int i=0;
		for (Int aInt : spec.getaInterfaces()) {
			ts.initialDecomposition("A" + i, aInt.getChannels(), aInt.getOutput());
			i++;
		}

		Set<TS> sTS = new HashSet<TS>();
		for (TS a : ts.getAgents()) {
			a.toDot();
		}
		TS chk = new TS("");
		State ss = new State("");
		chk.addState(ss);
		chk.setInitState("");

		for (TS pa : ts.getParameters()) {
			chk= chk.openParallelCompTS(ts.getAgentById(pa.getName()));
			Partitioning lp = new Partitioning(pa, ts.getAgentById(pa.getName()));
			sTS.add(lp.computeCompressedTS());
		}
		chk.toDot();
		// Create Dummy TS for compostion (The Id of ||)
		TS t = new TS("");
		State s = new State("");
		t.addState(s);
		t.setInitState("");

		for (TS tss : sTS) {

			t = t.openParallelCompTS(tss);
		}
		t.toDot();
		t = t.prunedTS(t);
		t.toDot();
	}

	private String checkFileName(String fileName) {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			if (fileName != null && new File(fileName).exists()) {
				break;
			}

			System.out.println("File \"" + new File(fileName).getAbsolutePath()+ "\" not found, try again: ");

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
