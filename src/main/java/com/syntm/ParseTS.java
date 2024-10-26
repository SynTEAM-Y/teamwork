package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.syntm.analysis.Partitioning;
import com.syntm.lts.*;

public class ParseTS {
	private TS mainTS = new TS("MainTS");

	public static void main(final String[] args) throws IOException {
		ParseTS parseTS = new ParseTS();
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Enter a TS for decomposition: ");
		String fileP = stdin.readLine();
		fileP = parseTS.checkFileName(fileP);
		parseTS.readInput(fileP);
		// parseTS.writeOutput("TStext");

	}

	public void readInput(final String fileP) {
		try {
			this.mainTS.parse(fileP);
			TS mTs=this.mainTS.reduce();
			//this.mainTS.setName("MainTS");
			//mTs.toDot();

			Set<TS> sTS = new HashSet<TS>();

			for (TS pa : mTs.getParameters()) {
				Partitioning lp = new Partitioning(pa, mTs.getAgentById(pa.getName()));
				sTS.add(lp.computeCompressedTS());
			}
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
