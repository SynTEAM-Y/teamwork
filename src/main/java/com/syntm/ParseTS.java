package com.syntm;

import org.apache.commons.io.FileUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.Sets;
import com.syntm.analysis.Partitioning;
import com.syntm.lts.*;
import com.syntm.util.Printer;

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

	public static void main(final String[] args) throws IOException, InterruptedException {
		ParseTS parseTS = new ParseTS();
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

		File outputFolder = new File("./generated/output/");
		FileUtils.cleanDirectory(outputFolder);

		System.out.println(ANSI_GREEN + "Enter a TS for decomposition: " + ANSI_RESET);
		String fileP = stdin.readLine();
		fileP = parseTS.checkFileName(fileP);
		Set<TS> sTS = parseTS.readInput(fileP);
		
		parseTS.mainTS.toDot();
		// parseTS.writeOutput("TStext");
		String exit = "";
		while (!exit.equals("x")) {
			System.out.println("");
			System.out.println(ANSI_GREEN + "Select an option to proceed" + ANSI_RESET);

			Set<String> choice = new HashSet<>(Arrays.asList("1", "2", "3", "x"));
			System.out.println(ANSI_GREEN +
					"[" + 1 + "] :  Generate distributed TS? " + ANSI_RESET);
			System.out.println(ANSI_GREEN +
					"[" + 2 + "] :  Compute composition & check Strong bisimulation? " + ANSI_RESET);
			System.out.println(ANSI_GREEN +
					"[" + 3 + "] :  On fly Simulatation of composition? " + ANSI_RESET);
			System.out.println(ANSI_GREEN +
					"[" + "x" + "] :  Exit? " + ANSI_RESET);

			String in = "";

			in = stdin.readLine();

			while (!choice.contains(in.toString())) {
				System.out.println(ANSI_RED + "Option does not exist!" + ANSI_RESET);
				System.out.println(ANSI_RED + "Try again " + ANSI_RESET);
				in = stdin.readLine();
			}
			switch (in.toString()) {
				case "1":
					for (TS ts : sTS) {
						ts.toDot();
					}
					parseTS.mainTS.toDot();
					for (TS ts : parseTS.mainTS.getAgents()) {
						ts.toDot();
					}
					System.out.println(
							ANSI_BLUE + "Generated" + ANSI_RESET);
					break;
				case "2":
					TS cComp = parseTS.compose(sTS);
					cComp.toDot();

					System.out.println(
							ANSI_BLUE + "Check Equivalence of " + parseTS.mainTS.getName() + " and " + cComp.getName()
									+ " -> " + parseTS.mainTS.equivCheck(cComp) + ANSI_RESET);
					break;
				case "3":
					parseTS.simulate(sTS);
					break;
				case "x":
					exit = "x";
					break;

				default:
					break;
			}
		}

	}

	public Set<TS> readInput(final String fileP) throws InterruptedException {
		try {
			this.mainTS.parse(fileP);
			TS mTs = this.mainTS.reduce();
			this.mainTS = mTs;
			Set<TS> sTS = new HashSet<TS>();

			for (TS pa : mTs.getParameters()) {

				Partitioning lp = new Partitioning(pa, mTs.getAgentById(pa.getName()));
				sTS.add(lp.computeCompressedTS());
			}

			return sTS;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void simulate(Set<TS> sTS) throws IOException {
		String in = "";
		String orientation = "";
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		Set<State> simStates = new HashSet<>();
		Printer simEnv = new Printer("SimulationEnv");
		System.out.println(ANSI_GREEN +
				"Choose a graph orientation, LR or T ?! " + ANSI_RESET);
		orientation = stdin.readLine().toString();
		// while (true) {
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
				simEnv.add(ts.toDot(ts.getStateById(in), new Trans()).formattedString());
			}
			simStates.add(ts.getStateById(in));
			simEnv.add(ts.toDot(ts.getStateById(in), new Trans()).formattedString());

		}
		simEnv.printNested(orientation);
		simEnv.setsBuilder(new StringBuilder());
		onFly(simStates, orientation);
		simStates.clear();
		// }

	}

	public Set<List<State>> cartesianProduct(List<Set<State>> sets) {
		Set<List<State>> cartesianProduct = Sets.cartesianProduct(sets);
		return cartesianProduct;
	}

	private TS compose(Set<TS> sTS) {
		TS t = new TS("");
		Set<String> chan = new HashSet<>();
		Set<String> output = new HashSet<>();
		List<Set<State>> stateSets = new ArrayList<>();
		for (TS ts : sTS) {
			t.setName(ts.getName() + "|" + t.getName());
			chan.addAll(ts.getInterface().getChannels());
			output.addAll(ts.getInterface().getOutput());
			stateSets.add(ts.getStates());

		}
		t.setName(t.getName().substring(0, t.getName().length() - 1));

		Set<List<State>> cproduct = new HashSet<>();
		cproduct = cartesianProduct(stateSets);
		for (List<State> list : cproduct) {
			State sc = new State("");
			Set<String> ls = new HashSet<>();
			Set<String> ch = new HashSet<>();
			Set<String> out = new HashSet<>();
			for (State st : list) {
				ls.addAll(st.getListen().getChannels());
				ch.addAll(st.getLabel().getChannel());
				out.addAll(st.getLabel().getOutput());
				if (out.size() > 1) {
					out.remove("-");
				}

				sc.getComStates().add(st);
				sc.setId(sc.getId() + "" + st.getId());
			}
			sc.setListen(new Listen(ls));
			sc.setLabel(new Label(ch, out));
			t.addState(sc);
		}

		final Set<State> initSet = new HashSet<>(
				sTS.stream().map(TS::getInitState).collect(Collectors.toSet()));

		State initState = t.getStates()
				.stream()
				.filter(tstate -> tstate.getComStates().equals(initSet))
				.collect(Collectors.toSet())
				.iterator()
				.next();
		t.setInitState(initState.getId());

		Int i = new Int(chan, output);
		t.setInterface(i);

		for (State state : t.getStates()) {
			Set<State> crntStates = new HashSet<>();
			crntStates.addAll(state.getComStates());

			Set<Trans> enabled = new HashSet<>();

			enabled = crntStates
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

			Set<State> rcv = new HashSet<>();
			if (!initiateSet.isEmpty()) {
				for (Trans sTR : initiateSet) {
					rcv.addAll(crntStates);
					rcv.remove(sTR.getSource());
					Set<State> nxtSet = new HashSet<>();
					for (State rcvST : rcv) {
						Set<Trans> rcvTRs = new HashSet<>();

						rcvTRs = rcvST.getTrans()
								.stream()
								.filter(tran -> tran.getAction().equals(sTR.getAction()))
								.collect(Collectors.toSet());
						if (!rcvTRs.isEmpty()) {
							nxtSet.add(rcvTRs.iterator().next().getDestination());
						} else {
							nxtSet.add(rcvST);
						}
					}
					nxtSet.add(sTR.getDestination());
					t.addTransition(t, t.getStateByComposite(t, crntStates), sTR.getAction(),
							t.getStateByComposite(t, nxtSet));
					t.getStateByComposite(t, crntStates).addTrans(new Trans(t.getStateByComposite(t, crntStates),
							sTR.getAction(), t.getStateByComposite(t, nxtSet)), t);

				}
			}

		}

		t.setStates(reachFrom(t, t.getInitState()));
		Set<Trans> trans = new HashSet<>();
		for (Trans tr : t.getTransitions()) {
			if (!t.getStates().contains(tr.getSource())) {
				trans.add(tr);
			}
		}
		t.getTransitions().removeAll(trans);

		return t;
	}

	private void onFly(Set<State> states, String orientation) throws IOException {
		String in = "";
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		Set<State> sStates = new HashSet<>(states);
		while (true) {
			Set<Trans> enabled = new HashSet<>();
			Printer simEnv = new Printer("SimulationEnv");
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
				System.out.println(ANSI_RED + "System is Deadlocked!" + ANSI_RESET);
				break;
			} else {
				System.out.println(ANSI_GREEN + "Select a send transition" + ANSI_RESET);
				int c = 0;
				Set<String> choice = new HashSet<>();
				choice.add("x");

				HashMap<String, Trans> tMap = new HashMap<>();
				for (Trans trans : initiateSet) {
					System.out.println(ANSI_GREEN +
							"[" + c + "] : " + trans + " from Agent " + trans.getSource().getOwner().getName()
							+ ANSI_RESET);
					tMap.put(String.valueOf(c), trans);
					choice.add(String.valueOf(c));
					c++;
				}

				System.out.println(ANSI_GREEN +
						"[x] :  Previous View " + ANSI_RESET);

				in = stdin.readLine();

				// System.err.println(Integer.parseInt(in)+"???");
				while (!choice.contains(in.toString())) {
					System.out.println(ANSI_RED + "Option does not exist!" + ANSI_RESET);
					System.out.println(ANSI_RED + "Try again " + ANSI_RESET);
					in = stdin.readLine();
				}

				final String input = in;
				if (in.equals("x")) {
					break;
				}
				Set<State> rcv = new HashSet<>();

				simEnv.add(tMap.get(in).getSource().getOwner().next(tMap.get(in)).formattedString());

				sStates.remove(tMap.get(in).getSource());

				sStates.add(
						tMap.get(in).getDestination().getOwner().getStateById(tMap.get(in).getDestination().getId()));
				rcv.addAll(sStates);
				rcv.remove(
						tMap.get(in).getDestination().getOwner().getStateById(tMap.get(in).getDestination().getId()));
				for (State st : rcv) {
					Set<Trans> trs = new HashSet<>();
					trs = st.getTrans()
							.stream()
							.filter(tr -> tr.getAction().equals(tMap.get(input).getAction()))
							.collect(Collectors.toSet());
					if (!trs.isEmpty()) {
						Trans tr = new Trans();
						tr = trs.iterator().next();
						simEnv.add(st.getOwner().next(tr).formattedString());
						sStates.remove(tr.getSource());
						sStates.add(tr.getDestination().getOwner().getStateById(tr.getDestination().getId()));
					} else {
						simEnv.add(st.getOwner()
								.toDot(st.getOwner().getStateById(st.getId()), new Trans())
								.formattedString());
					}
				}

				simEnv.printNested(orientation);
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

	public Set<State> reachFrom(TS ts, State s) {

		HashMap<State, Boolean> visited = new HashMap<>();
		for (State st : ts.getStates()) {
			visited.put(st, false);
		}
		LinkedList<State> queue = new LinkedList<State>();
		Set<Trans> transitiveC = new HashSet<>();
		Set<State> reach = new HashSet<>();
		visited.put(s, false);
		queue.add(s);
		while (queue.size() != 0) {
			s = queue.poll();
			reach.add(s);
			transitiveC = hasTransitions(ts, s);
			if (transitiveC != null) {
				if (transitiveC.size() != 0) {
					for (Trans tr : transitiveC) {
						if (!visited.get(tr.getDestination())) {
							visited.put(tr.getDestination(), true);
							queue.add(tr.getDestination());
						}
					}
				}
			}
		}
		return reach;
	}

	private Set<Trans> hasTransitions(TS ts, State s) {
		Set<Trans> trSet = new HashSet<>();
		trSet = ts.getTransitions()
				.stream()
				.filter(tr -> tr.getSource().equals(s))
				.collect(Collectors.toSet());
		return trSet;
	}

}
