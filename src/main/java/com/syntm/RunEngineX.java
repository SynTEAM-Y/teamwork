package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import com.syntm.analysis.Partitioning;
import com.syntm.lts.Mealy;
import com.syntm.lts.Spec;
import com.syntm.lts.State;
import com.syntm.lts.TS;

public class RunEngineX {

	public static void main(final String[] args)
			throws IOException, InterruptedException {

		// Parse arguments
		Map<String, String> params = RunEngineX.parse(args);

		if (!params.containsKey("tsDirectory")) {
			// Parse the spec file
			Map<String, String> specs = new HashMap<>();
			try (BufferedReader f = new BufferedReader(new FileReader(params.get("specFile")))) {
				while (true) {
					String line = f.readLine();
					if (line == null) break;
					specs.put(line.split("@")[0], line.split("@")[1]);
				}
			}

			Spec spec = new Spec();
			spec.setcCode(specs.get("sinp")); // sinp
			spec.setoCode(specs.get("sout")); // sout
			Set<String> chan = new HashSet<>(Arrays.asList(specs.get("sinp").split(","))); // sinp
			Set<String> out = new HashSet<>(Arrays.asList(specs.get("sout").split(","))); // sout
			spec.getsInterface().setChannels(chan);
			spec.getsInterface().setOutput(out);
			spec.agentBuilder(specs.get("sint")); // sint
			spec.assumptionBuilder(specs.get("aformula").split(",")); // aformula
			spec.guaranteeBuilder(specs.get("gformula").split(",")); // gformula

			String outputPath = params.get("outputPath");
			if (!params.containsKey("mealyFile")) {
				// Generate mealy file using strix
				String command = spec.toString();
				System.out.println(command);

				ProcessBuilder p = new ProcessBuilder("docker", "run", "lazkany/strix", "-f", command, spec.inParam(),
						spec.outParam(), "--k");
				p.redirectOutput(new File(outputPath));
				Process proc = p.start();
				proc.waitFor();
			} else {
				// Compress TS from a mealy machine
				Mealy m = new Mealy("");
				m.kissToMealy(params.get("mealyFile"), spec.getcCode(), spec.getoCode());
				m.toDot(m, m.getName());
				if (m.getStatus().equals("REALIZABLE")) {
					TS ts = m.toTS(m, m.getName());
					if (!ts.getStatus().equals("ND")) {
						System.out.println(
								"\n\n Specification is REALIZABLE for a multi-agent\n\n You will get a distributed Implementation :)\n\n");
						RunEngineX.processInput(ts, spec, outputPath);

					} else {
						System.out.println(
								"Despite the realizablity of the specification on a single machine\n\n it cannot be realized in our distributed model\n\n The translation to TS resulted in a non-determinstic TS.");
					}

				}
			}
		} else {
			// Read all TSs from tsDirectory
			Set<TS> sTS = new HashSet<>();
			try (Stream<Path> paths = Files.walk(Paths.get(params.get("tsDirectory")))) {
				paths
						.filter(Files::isRegularFile)
						.forEach(f -> {
							// System.out.println("Reading from: " + f.toAbsolutePath().toString());
							try (FileInputStream fs = new FileInputStream(f.toAbsolutePath().toString())) {
								try (ObjectInputStream os = new ObjectInputStream(fs)) {
									sTS.add((TS) os.readObject());
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
			}

			// Create Dummy TS for compostion (The Id of ||)
			TS t = new TS("");
			State s = new State("in");
			t.addState(s);
			t.setInitState("in");

			for (TS tss : sTS) {
				t = t.openParallelCompTS(tss);
			}
			t.toDot();
			t = t.prunedTS(t);
			t.toDot();
		}

	}

	private static Map<String, String> parse(String[] args) {
		Map<String, String> output = new HashMap<>();
		switch (args[0]) {
			case "gen":
				output.put("specFile", args[1]);
				output.put("outputPath", args[2]);
				break;
			case "reduce":
				output.put("specFile", args[1]);
				output.put("mealyFile", args[2]);
				output.put("outputPath", args[3]);
				break;
			case "combine":
				output.put("tsDirectory", args[1]);
				break;

			default:
				System.out.println("Wrong arguments");
				System.exit(0);
				break;
		}
		return output;
	}

	private static void processInput(TS ts, Spec spec, String outputPath) {
		Random rand = new Random();

		for (String agent : spec.getAgents().keySet()) {
			ts.initialDecomposition(agent, spec.getAgents().get(agent).getChannels(),
					spec.getAgents().get(agent).getOutput());
		}

		int n = 0;
		for (TS pa : ts.getParameters()) {
			Partitioning lp = new Partitioning(pa, ts.getAgentById(pa.getName()));
			TS compressed = lp.computeCompressedTS();

			// System.out.println("Writing to: " + outputPath + "agent" + n);
			try (FileOutputStream f = new FileOutputStream(outputPath + "agent" + n++)) {
				try (ObjectOutputStream obs = new ObjectOutputStream(f)) {
					obs.writeObject(compressed);
				}
			} catch (IOException e) {
				System.out.println("Could not save TS to file...");
			}
		}
		System.out.println("Transition systems compressed.");
	}
}
