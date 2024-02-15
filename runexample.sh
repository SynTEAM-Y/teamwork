# Script for running an example LTL without having to go to another repo

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
    echo "Moving jar file"
    cp "target/teamwork-1.0.jar" "../SynTMPkg/generated/"
else
    FILE="$1"
fi

cd ../SynTMPkg
if [[ -e "Ex/${FILE}" ]]; then
    rm -rf "./generated/output/"
    mkdir ./generated/output/
    node ./bin/cli generate "Ex/${FILE}"
    FILENOEXT="$(echo ${FILE} | sed -r "s/(\w+)\.\w+/\1/")"
    javac -classpath ./generated/teamwork-1.0.jar "./generated/${FILENOEXT}.java"
    time java -classpath ./generated/teamwork-1.0.jar "./generated/${FILENOEXT}.java"
    rm -rf "../teamwork/output/examples/"
    cp -r "./generated/output/" "../teamwork/output/examples/"
    cd - &> /dev/null
    exit 0
else
    echo "File does not exist ($(pwd)/Ex/${FILE})"
    cd - &> /dev/null
    exit 1
fi
