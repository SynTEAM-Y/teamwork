package com.syntm;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
RunEngine.java (c) 2024
Desc: Spec synthesis engine
Created:  17/11/2024 09:45:55
Updated:  17/11/2024 19:08:14
Version:  1.1
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Sets;
import com.syntm.analysis.Partitioning;
import com.syntm.lts.Int;
import com.syntm.lts.Label;
import com.syntm.lts.Listen;
import com.syntm.lts.Mealy;
import com.syntm.lts.Spec;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;
import com.syntm.util.Printer;

public class RunEngine {
	// private TS mainTS;
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static void main(final String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		File outputFolder = new File("./generated/output/");
		FileUtils.cleanDirectory(outputFolder);
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
			fos.close();
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
			} else {
				System.out.println(
						"Despite the realizablity of the specification on a single machine\n\n it cannot be realized in our distributed model\n\n The translation to TS resulted in a non-determinstic TS.");
			}

		}
		// Strategy.delete();

	}

	public void processInput(TS ts, Spec spec) throws IOException {
		int i = 0;

		for (Int aInt : spec.getaInterfaces()) {
			ts.initialDecomposition("A" + i, aInt.getChannels(), aInt.getOutput());
			i++;
		}
		ts.toDot();

		Set<TS> sTS = new HashSet<TS>();
		// for (TS a : ts.getAgents()) {
		// a.toDot();
		// }

		for (TS pa : ts.getParameters()) {
			Partitioning lp = new Partitioning(pa, ts.getAgentById(pa.getName()));
			sTS.add(lp.computeCompressedTS());
		}
		// TS cComp = compose(sTS);
		// //writeOutput("comp", cComp);
		// //writeOutput("ts", ts);
		// cComp.toDot();
		// System.out.println(ANSI_GREEN + "Check Equivalence of " + ts.getName() + "
		// and " + cComp.getName()
		// + " -> " + ts.equivCheck(cComp) + ANSI_RESET);
		// this.simulate(sTS);
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String exit = "";
		while (!exit.equals("x")) {
			System.out.println("");
			System.out.println(ANSI_GREEN + "Select an option to proceed" + ANSI_RESET);

			Set<String> choice = new HashSet<>(Arrays.asList("1", "2", "3", "x"));
			System.out.println(ANSI_GREEN +
					"[" + 1 + "] :  Generate distirbuted TS? " + ANSI_RESET);
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
					for (TS tss : sTS) {
						tss.toDot();
					}
					ts.toDot();
					for (TS tss : ts.getAgents()) {
						tss.toDot();
					}
					System.out.println(
							ANSI_BLUE + "Generated" + ANSI_RESET);
					break;
				case "2":
					TS cComp = compose(sTS);
					cComp.toDot();

					System.out.println(
							ANSI_BLUE + "Check Equivalence of " + ts.getName() + " and " + cComp.getName()
									+ " -> " + ts.equivCheck(cComp) + ANSI_RESET);
					break;
				case "3":
					simulate(sTS);
					break;
				case "x":
					exit = "x";
					break;

				default:
					break;
			}
		}
	}

	private String checkFileName(String fileName) {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			if (fileName != null && new File(fileName).exists()) {
				break;
			}

			System.out.println("File \"" + new File(fileName).getAbsolutePath() + "\" not found, try again: ");

			try {
				fileName = stdin.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return fileName;
	}

	public void writeOutput(final String filename, TS ts) {
		try {
			FileWriter out = new FileWriter(new File(filename));

			out.write(ts.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
