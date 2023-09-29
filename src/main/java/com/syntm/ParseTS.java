package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import com.syntm.lts.*;

public class ParseTS {
	private TS mainTS;

	public static void main(final String[] args) throws IOException {
		ParseTS parseTS = new ParseTS();
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Enter a TS for decomposition: ");
		String fileP = stdin.readLine();
		fileP = parseTS.checkFileName(fileP);
		parseTS.readInput(fileP);
		parseTS.writeOutput("TStext");

	}

	public void readInput(final String fileP) {
		try {
			this.mainTS = TS.parse(fileP);

			mainTS.toDot(mainTS, "Main TS");

			for (TS p : this.mainTS.getParameters()) {
				p.toDot(p, p.getName());
			}

			for (TS a : this.mainTS.getAgents()) {
				a.toDot(a, a.getName());
			}
			 for (TS p : this.mainTS.getParameters()) {
				p.openParallelCompTS(p, mainTS.getAgentById(p.getName()));
				p.closedParallelCompTS(p, mainTS.getAgentById(p.getName()));
			//	if (mainTS.getAgentById(p.getName()).getName().equals("Proc")) {
					Partitioning lp = new Partitioning(p, mainTS.getAgentById(p.getName()));
					TS pPrime = lp.computeCompressedTS();
					p.openParallelCompTS(p, pPrime);
					p.closedParallelCompTS(p, pPrime);
			//	}

			}
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
