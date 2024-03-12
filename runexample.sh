#!/usr/bin/env bash

# Script for running an example LTL without having to go to another repo
# Valgrind is necessary to run the memory checks

if [ -z "$1" ]; then
    echo "Example file not supplied"
    echo "Usage: runexample.sh [-c] <filename>"
    echo "Add -c for compilation of teamwork before running the test"
    exit 1
fi

if [ "$1" == "-c" ]; then
    if [ -z "$2" ]; then
        echo "Example file not supplied"
        exit 1
    fi
    FILE="$2"
    echo "Compiling teamwork"
    mvn package
    # echo "Moving jar file"
    # cp "target/teamwork-1.0.jar" "../SynTMPkg/generated/"
else
    FILE="$1"
fi

cd ../SynTMPkg
if [[ -e "Ex/${FILE}" ]]; then
    # rm -rf "./generated/output/"
    # mkdir ./generated/output/
    # node ./bin/cli generate "Ex/${FILE}"
    FILENOEXT="$(echo ${FILE} | sed -r "s/(\w+)\.\w+/\1/")"
    node ./bin/cli parseSpec "Ex/${FILE} -o ../teamwork/target/${FILENOEXT}.spec"
    cd ../teamwork
    # javac ./target/teamwork-1.0.jar
    echo "-- Generating mealy file --"
    time java -jar "./target/teamwork-1.0.jar" gen "target/${FILENOEXT}.spec" "Ex/mealy/Mealy_${FILENOEXT}"

    rm output/tss/*
    echo "-- Reducing the transition system --"
    time java -jar "./target/teamwork-1.0.jar" reduce "target/${FILENOEXT}.spec" "Ex/mealy/Mealy_${FILENOEXT}" "output/tss/"
    # rm output/tss/*
    # valgrind --tool=massif java -jar "./target/teamwork-1.0.jar" reduce "target/${FILENOEXT}.spec" "Ex/mealy/Mealy_${FILENOEXT}" "output/tss/"
    echo "-- Combining the transition systems --"
    time java -jar "./target/teamwork-1.0.jar" combine "output/tss/"
    # javac -classpath ./generated/teamwork-1.0.jar "./generated/${FILENOEXT}.java"
    # time java -classpath ./generated/teamwork-1.0.jar "./generated/${FILENOEXT}.java"
    # valgrind --tool=massif java -classpath ./generated/teamwork-1.0.jar "./generated/${FILENOEXT}.java"
    # rm -rf "../teamwork/output/examples/"
    # cp -r "./generated/output/" "../teamwork/output/examples/"
    # cd - &> /dev/null
    exit 0
else
    echo "File does not exist ($(pwd)/Ex/${FILE})"
    cd - &> /dev/null
    exit 1
fi
