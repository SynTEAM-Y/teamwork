package com.syntm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.syntm.analysis.Partitioning;
import com.syntm.lts.*;

public class ParseTS {
	private TS mainTS = new TS("MainTS");
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static void main(final String[] args) throws IOException {
		ParseTS parseTS = new ParseTS();
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

		System.out.println(ANSI_GREEN + "Enter a TS for decomposition: " + ANSI_RESET);
		String fileP = stdin.readLine();
		fileP = parseTS.checkFileName(fileP);
		Set<TS> sTS = parseTS.readInput(fileP);
		// parseTS.writeOutput("TStext");
		parseTS.simulate(sTS);

	}

	public Set<TS> readInput(final String fileP) {
		try {
			this.mainTS.parse(fileP);
			TS mTs = this.mainTS.reduce();
			// this.mainTS.setName("MainTS");
			// mTs.toDot();

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
				// tss.toDot(false,tss.getInitState().getTrans().iterator().next());
				// tss.toDot(true,new Trans());
			}
			// t.toDot();
			t = t.prunedTS(t);
			t.toDot();

			return sTS;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void simulate(Set<TS> sTS) throws IOException {
		String in = "";
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		Set<State> simStates = new HashSet<>();

		System.out.println(ANSI_GREEN + "Simulate? y/n" + ANSI_RESET);
		in = stdin.readLine().toString();
		if (!in.equals("n")) {
			while (true) {
				for (TS ts : sTS) {
					Set<String> ids = new HashSet<>();
					ids = ts.getStates().stream().map(State::getId).collect(Collectors.toSet());
					System.out.println(
							ANSI_GREEN + "Pick a state for Agent " + ts.getName() + " from  -> " + ids + ANSI_RESET);
					in = stdin.readLine().toString();

					while (!ids.contains(in)) {
						System.out.println(ANSI_RED + "State does not exist!" + ANSI_RESET);
						System.out.println(ANSI_RED + "Pick a state for Agent " + ts.getName()
								+ " ONLY from this list -> " + ids + ANSI_RESET);
						in = stdin.readLine().toString();
						ts.toDot(ts.getStateById(in), new Trans());
					}
					simStates.add(ts.getStateById(in));
					ts.toDot(ts.getStateById(in), new Trans());

				}
				onFly(simStates);
				simStates.clear();
			}
		}

	}

	private void onFly(Set<State> states) throws IOException {
		String in = "";
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		
		Set<State> sStates = new HashSet<>(states);
		while (true) {
			Set<Trans> enabled = new HashSet<>();
			enabled = sStates
					.stream()
					.map(State::getTrans)
					.collect(Collectors.toSet())
					.stream()
					.flatMap(tr -> tr.stream())
					.collect(Collectors.toSet());

			Set<Trans> initiateSet = new HashSet<>();

			initiateSet = enabled
					.stream()
					.filter(tr -> tr.getSource().getOwner().getInterface().getChannels().contains(tr.getAction()))
					.collect(Collectors.toSet());
			Set<Trans> recvSet = new HashSet<>();
			recvSet.addAll(enabled);
			recvSet.removeAll(initiateSet);

			if (initiateSet.isEmpty()) {
				System.out.println(ANSI_RED + "System is Deadlocked from your selection!" + ANSI_RESET);
				break;
			} else {
				System.out.println(ANSI_GREEN + "Select a send transition" + ANSI_RESET);
				int c = 0;
				HashMap<String, Trans> tMap = new HashMap<>();
				for (Trans trans : initiateSet) {
					System.out.println(ANSI_GREEN +
							"[" + c + "] : " + trans + " from Agent " + trans.getSource().getOwner().getName()
							+ ANSI_RESET);
					tMap.put(String.valueOf(c), trans);
					c++;
				}
				System.out.println(ANSI_GREEN +
						"[x] :  Previous View " + ANSI_RESET);
				in = stdin.readLine();
				if (in.equals("x")) {
					break;
				}
				tMap.get(in).getSource().getOwner().next(tMap.get(in).getSource(), tMap.get(in));

				sStates.remove(tMap.get(in).getSource());

				sStates.add(
						tMap.get(in).getDestination().getOwner().getStateById(tMap.get(in).getDestination().getId()));

				for (Trans tr : recvSet) {
					if (tr.getAction().equals(tMap.get(in).getAction())
							&& !tr.getSource().getOwner().equals(tMap.get(in).getSource().getOwner())) {
						tr.getSource().getOwner().next(tr.getSource(), tr);
						sStates.remove(tr.getSource());
						sStates.add(tr.getDestination().getOwner().getStateById(tr.getDestination().getId()));
					}
				}

			}
		}

	}

	private String checkFileName(String fileName) {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			if (fileName != null && new File(fileName).exists()) {
				break;
			}

			System.out.println(ANSI_RED + "File \"" + fileName + "\" not found, try again: " + ANSI_RESET);

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
