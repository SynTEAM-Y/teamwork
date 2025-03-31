# teamwork
Synthesis of Teamwork Multi-Agent Systems

# Tool that supports both TS decomposition and LTL Distributed Synthesis

Resources Folders:

- SPEC : Contains LTL specifications.
         file extension:  .syntm  (stands for synthesis of team)

Currently, we use a basic syntax to specify the Input/Output for the formula 
and the interfaces of agents for decomposition. We have collected these 
formulas from the well-known Strix synthesis tool repository. We would like
to stress that we have no relations to Strix or any of its team. We merely 
use Strix to solve the single agent synthesis instance for us. We work on 
the output of Strix.

- TS : Contains TS files for decomposition.
         file extension: .dot

Currently, we accept TS as a regular dot file. It has a special node, named
spec, to specify how we want to distribute it.

- generated : Contains dot files storing the output result

# To Run the Engine.jar type the following command:

        java -jar Engine.jar 

The console will guide you on the go to either distribute a TS or perform 
LTL distributed synthesis


1. TS Decomposition:

       - Choose a dot file from the TS folder. e.g.,  TS/caseStudy.dot 
       - Press Enter
       - The engine will perform trivial decomposition for each interface and generate a
          dot file for each (agent.dot). It will report the size of each of them.
       - The engine will also produce Marked TS for each (Marked[agent].dot), showing 
          how each TS will be compressed.
       - Select option [1] to generate a minimised TS for each ([agent]r.dot)
       - it will also generate a minimisation based on strong bisimulation to compare <agent>.dot
       - select option [2] to compose the result and check strong bisimulation w.r.t. the centralised one.
       - select option [3] to do simulation for the generated distributed implementation. The engine will
         generate SimulationEnv.dot file. Together with console you can do on fly simulation.
         The best way to use our tool is to open the tool folder using vscode and install interactive graphvis viewer. 
        
2. LTL Distributed Synthesis

       - You need Docker up and running to use this feature, because we use an external synthesis tool
         , named Strix, for single agent synthesis. We would like to stress that we have no relations to Strix 
         or any of its team. We merely use Strix to solve the single agent synthesis instance for us. We work
          on the output of Strix. Our choice of Strix is because of its reputation and performance.

       - Choose a spec file from the SPEC folder. e.g.,  SPEC/ltl4reset.syntm
       - The engine will generate dot files for Mealy and its TS implementation.
       - The engine will also perform initial decomposition and produce marked TSs.
       - Choose option [1] to generate the distributed implementation.
       - The reset of the options are as before.
    
# Note: we accept general LTL formulas and without any restrictions. We automatically 
# adjust the formulas to respect our specialised mealy.

# We would like to stress that this is the first tool of its kind to solve such a problem and 
provide affordable (complexity wise) distributed implementation. Thus, all of our benchmarks
are locally developed. Many of the LTL specifications were taken from Strix repository and
distributed in our engine. We started an effort to collect benchmarks used for single-agent
synthesis and transform them into teamwork synthesis. 

# We need to make decisions about few points. For instance, the same problem can be 
distributed in many different ways depending on the chosen interfaces, number of agents,
which channels to control, and what combinations of them. All of these parameters play a
role in the final result for the same benchmark. 

To illustrate  the previous point, consider the two benchmarks below:

TS/piped.dot

TS/pipeline.dot

Both are the same input TS, but they differ on how many agent to distribute it to.
