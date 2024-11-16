package com.syntm.util;

import java.io.FileOutputStream;
import java.io.IOException;

public class Printer {

    private StringBuilder sBuilder = new StringBuilder();
    private String filePrefix;

    public Printer() {
    }

    public Printer(String filePrefix) {
        this.filePrefix = filePrefix;
    }
    
    public StringBuilder getsBuilder() {
        return sBuilder;
    }

    public void setsBuilder(StringBuilder sBuilder) {
        this.sBuilder = sBuilder;
    }

    public void addln(String line) {
        sBuilder.append(line).append("\n");
    }

    public void add(String text) {
        sBuilder.append(text);
    }

    public String formattedString() {
        return sBuilder.toString();
    }

    public void addnewln() {
        sBuilder.append("\n");
    }

    public void print() {
        try {

            if (filePrefix == null || filePrefix.isEmpty()) {
                filePrefix = "output";
            }

            sBuilder.insert(0, "digraph G {").append("\n");
            sBuilder.append("}").append("\n");
            writeTextToFile(filePrefix + ".dot", sBuilder.toString());

            StringBuilder command = new StringBuilder();
            command.append("dot -Tpng "). // output type
                    append(filePrefix).append(".dot ");
            // . // input dot file
            // append("-o ").append(filePrefix).append(".jpg"); // output image

            executeCommand(command.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Printer clusterIt(String name) {
        getsBuilder().insert(0, "\n subgraph cluster"+name+" {").append("\n");
        getsBuilder().append("}").append("\n");
        return this;
    }

    public void printNested(String orientation) {
        try {

            if (filePrefix == null || filePrefix.isEmpty()) {
                filePrefix = "output";
            }

            sBuilder.insert(0,
                    "digraph G {\n" + //
                                                " graph [fontcolor=\"green\",fontsize=10,rankdir="+orientation+",ranksep=0.6,nodesep=0.5];\n" + //
                            " peripheries=0;\n   compound=true;")
                    .append("\n");
            sBuilder.append("}").append("\n");
            writeTextToFile(filePrefix + ".dot", sBuilder.toString());

            StringBuilder command = new StringBuilder();
            command.append("dot -Tpng "). // output type
                    append(filePrefix).append(".dot ");
            // . // input dot file
            // append("-o ").append(filePrefix).append(".jpg"); // output image

            executeCommand(command.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void printText() {
        try {

            if (filePrefix == null || filePrefix.isEmpty()) {
                filePrefix = "output";
            }

            sBuilder.insert(0, "[").append("\n");
            sBuilder.append("]").append("\n");
            writeTextToFile(filePrefix, sBuilder.toString());

            StringBuilder command = new StringBuilder();
            command.append("dot -Tpng ");

            executeCommand(command.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void executeCommand(String command) throws Exception {
        Runtime.getRuntime().exec(command);
    }

    private void writeTextToFile(String fileName, String text) throws IOException {
        FileOutputStream outputStream = new FileOutputStream("./generated/output/" + fileName);
        outputStream.write(text.getBytes());
        outputStream.close();
    }
}
