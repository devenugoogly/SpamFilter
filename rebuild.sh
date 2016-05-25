export CLASSPATH=$(hadoop classpath)
export HADOOP_CLASSPATH=$CLASSPATH

mvn clean install

mv target/NaiveBayes.jar NaiveBayes.jar

rm -rf target
rm dependency-reduced-pom.xml
