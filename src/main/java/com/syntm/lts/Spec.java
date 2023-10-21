package com.syntm.lts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.syntm.util.Printer;

public class Spec {
    private String command;
    private Int sInterface;
    private List<String> assupmptions;
    private List<String> guarantees;
    private Set<Int> aInterfaces;
    private HashMap<String, Int> agents;
    private String cCode;
    private String oCode;

    Printer specBuilder = new Printer();

    public Spec() {
        this.aInterfaces = new HashSet<>();
        this.sInterface = new Int();
        this.assupmptions = new ArrayList<>();
        this.guarantees = new ArrayList<>();
        this.agents = new HashMap<>();
    }

    public HashMap<String, Int> getAgents() {
        return agents;
    }

    public void setAgents(HashMap<String, Int> agents) {
        this.agents = agents;
    }

    public String getoCode() {
        return oCode;
    }

    public void setoCode(String oCode) {
        this.oCode = oCode;
    }

    public String getcCode() {
        return cCode;
    }

    public void setcCode(String cCode) {
        this.cCode = cCode;
    }

    public Printer getSpecBuilder() {
        return specBuilder;
    }

    public void setSpecBuilder(Printer specBuilder) {
        this.specBuilder = specBuilder;
    }

    public String getCommand() {
        return command;
    }

    public Int getsInterface() {
        return sInterface;
    }

    public void setsInterface(Int sInterface) {
        this.sInterface = sInterface;
    }

    public List<String> getAssupmptions() {
        return assupmptions;
    }

    public void setAssupmptions(List<String> assupmptions) {
        this.assupmptions = assupmptions;
    }

    public List<String> getGuarantees() {
        return guarantees;
    }

    public void setGuarantees(List<String> guarantees) {
        this.guarantees = guarantees;
    }

    public Set<Int> getaInterfaces() {
        return aInterfaces;
    }

    public void setaInterfaces(Set<Int> aInterfaces) {
        this.aInterfaces = aInterfaces;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((command == null) ? 0 : command.hashCode());
        result = prime * result + ((sInterface == null) ? 0 : sInterface.hashCode());
        result = prime * result + ((assupmptions == null) ? 0 : assupmptions.hashCode());
        result = prime * result + ((guarantees == null) ? 0 : guarantees.hashCode());
        result = prime * result + ((aInterfaces == null) ? 0 : aInterfaces.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Spec other = (Spec) obj;
        if (command == null) {
            if (other.command != null)
                return false;
        } else if (!command.equals(other.command))
            return false;
        if (sInterface == null) {
            if (other.sInterface != null)
                return false;
        } else if (!sInterface.equals(other.sInterface))
            return false;
        if (assupmptions == null) {
            if (other.assupmptions != null)
                return false;
        } else if (!assupmptions.equals(other.assupmptions))
            return false;
        if (guarantees == null) {
            if (other.guarantees != null)
                return false;
        } else if (!guarantees.equals(other.guarantees))
            return false;
        if (aInterfaces == null) {
            if (other.aInterfaces != null)
                return false;
        } else if (!aInterfaces.equals(other.aInterfaces))
            return false;
        return true;
    }

    public String shortString(Set<String> longString) {
        String st = "";
        for (String string : longString) {
            st += string + ",";
        }
        if (st.endsWith(",")) {
            st = st.substring(0, st.length() - 1);
        }
        return st;
    }

    public String interleave(String ch) {
        String st = "(";
        for (String c : this.sInterface.getChannels()) {
            if (!c.equals(ch)) {
                st += "!" + c + " & ";
            }
        }
        if (st.endsWith("& ")) {
            st = st.substring(0, st.length() - 2);
        }
        st += ") | ";
        return st;
    }

    public String inParam() {
        return "--ins=" + this.cCode;
    }

    public String outParam() {
        return "--outs=" + this.oCode;
    }

    public void assumptionBuilder(String[] assumptions) {
        String st = "";
        specBuilder.add("(\n");
        for (String ch : this.sInterface.getChannels()) {
            st += " !" + ch + " &\n";
        }
        specBuilder.add(st);
        specBuilder.add(" X G (");
        st = "";
        for (String ch : this.sInterface.getChannels()) {
            st += ch + " | ";
        }
        if (st.endsWith(" | ")) {
            st = st.substring(0, st.length() - 2);
        }
        st += ") &\n";
        specBuilder.add(st);
        st = "";
        specBuilder.add(" X G (");
        for (String ch : this.sInterface.getChannels()) {
            st += interleave(ch);
        }
        if (st.endsWith("| ")) {
            st = st.substring(0, st.length() - 2);
        }
        specBuilder.add(st + " ) &\n");
        st = "";
        for (String aphi : assumptions) {
            st += " X (" + aphi + ")" + " &\n";
        }
        if (st.endsWith(" &\n")) {
            st = st.substring(0, st.length() - 2);
        }
        specBuilder.add(st);
    }

    public void guaranteeBuilder(String[] guarantees) {
        String st = "";
        String fs = specBuilder.formattedString();
        if (fs.endsWith(" &\n")) {
            fs = fs.substring(0, st.length() - 2);
        }
        Printer sb = new Printer();
        sb.add(fs);
        this.setSpecBuilder(sb);
        ;
        specBuilder.add(") ");
        specBuilder.add("-> ");
        specBuilder.add("(");
        for (String o : this.sInterface.getOutput()) {
            st += " !" + o + " &\n";
        }
        specBuilder.add(st);

        st = "";
        for (String phi : guarantees) {
            st += " X (" + phi + ")" + " &\n";
        }
        if (st.endsWith(" &\n")) {
            st = st.substring(0, st.length() - 2);
        }
        specBuilder.add(st);
         specBuilder.add(")");
    }

    public void agentBuilder(String agentString) {
        String[] agents = agentString.split(";");

        for (String agent : agents) {
            String[] parts = agent.split(":");
            Set<String> achs = new HashSet<String>(Arrays.asList(parts[1].split(",")));
            Set<String> ao = new HashSet<>(Arrays.asList(parts[2].split(",")));
            this.agents.put(parts[0], new Int(achs, ao));
        }
        System.out.println(this.agents);
    }

    public void specReader(final String specFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(specFile));
        String line = "";
        String recent = "";
        String st = "";
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            switch (parts[0].trim()) {
                case "Interface":
                    this.cCode = parts[1];
                    this.oCode = parts[2];
                    Set<String> chan = new HashSet<>(Arrays.asList(parts[1].trim().split(",")));
                    Set<String> out = new HashSet<>(Arrays.asList(parts[2].trim().split(",")));
                    this.sInterface.setChannels(chan);
                    this.sInterface.setOutput(out);
                    break;
                case "Assumptions":
                    recent = "Assumptions";
                    specBuilder.add("(\n");
                    for (String ch : this.sInterface.getChannels()) {
                        st += " !" + ch + " &\n";
                    }
                    specBuilder.add(st);
                    specBuilder.add(" X G (");
                    st = "";
                    for (String ch : this.sInterface.getChannels()) {
                        st += ch + " | ";
                    }
                    if (st.endsWith(" | ")) {
                        st = st.substring(0, st.length() - 2);
                    }
                    st += ") &\n";
                    specBuilder.add(st);
                    st = "";
                    specBuilder.add(" X G (");
                    for (String ch : this.sInterface.getChannels()) {
                        st += interleave(ch);
                    }
                    if (st.endsWith("| ")) {
                        st = st.substring(0, st.length() - 2);
                    }
                    specBuilder.add(st + " ) &\n");
                    st = "";
                    break;
                case "EndAssumptions":
                    if (st.endsWith(" &\n")) {
                        st = st.substring(0, st.length() - 2);
                    }
                    specBuilder.add(st);
                    break;
                case "Guarantees":
                    recent = "Guarantees";
                    st = "";
                    specBuilder.add(") ");
                    specBuilder.add("-> ");
                    specBuilder.add("(");
                    for (String o : this.sInterface.getOutput()) {
                        st += " !" + o + " &\n";
                    }
                    specBuilder.add(st);
                    break;
                case "EndGuarantees":
                    if (st.endsWith(" &\n")) {
                        st = st.substring(0, st.length() - 2);
                    }
                    specBuilder.add(st);
                    specBuilder.add(")");
                    break;
                case "A":
                    Set<String> achs = new HashSet<String>(Arrays.asList(parts[2].trim().split(",")));
                    Set<String> ao = new HashSet<>(Arrays.asList(parts[3].trim().split(",")));
                    aInterfaces.add(new Int(achs, ao));
                    break;
                default:
                    st = "";
                    switch (recent) {
                        case "Assumptions":
                            st += " X (" + line + ")" + " &\n";

                            break;
                        case "Guarantees":

                            st += " X (" + line + ")" + " &\n";
                            specBuilder.addln(" X (" + line + ")" + " &\n");
                            break;

                        default:
                            break;
                    }
                    break;
            }
        }

        reader.close();
    }

    @Override
    public String toString() {
        return this.specBuilder.formattedString();
    }

}
