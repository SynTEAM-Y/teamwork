package com.syntm.util;

import java.io.FileOutputStream;
import java.io.IOException;

public class GraphPrinter {

    private StringBuilder graphBuilder = new StringBuilder();
    private String filePrefix;

    public GraphPrinter() {
    }

    public GraphPrinter(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    public void addln(String line) {
        graphBuilder.append(line).append("\n");
    }

    public void add(String text) {
        graphBuilder.append(text);
    }

    public void addnewln() {
        graphBuilder.append("\n");
    }

    public void print() {
        try {

            if (filePrefix == null || filePrefix.isEmpty()) {
                filePrefix = "output";
            }

            graphBuilder.insert(0, "digraph G {").append("\n");
            graphBuilder.append("}").append("\n");
            writeTextToFile(filePrefix + ".dot", graphBuilder.toString());

            StringBuilder command = new StringBuilder();
            command.append("dot -Tpng "). // output type
                    append(filePrefix).append(".dot ");
                    //. // input dot file
                   // append("-o ").append(filePrefix).append(".jpg"); // output image

            executeCommand(command.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void executeCommand(String command) throws Exception {
        Runtime.getRuntime().exec(command);
    }

    private void writeTextToFile(String fileName, String text) throws IOException {
        FileOutputStream outputStream = new FileOutputStream("Ex/SynrTS/"+fileName);
        outputStream.write(text.getBytes());
        outputStream.close();
    }
}
