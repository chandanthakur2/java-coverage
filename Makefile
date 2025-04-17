GRADLE := ./gradlew

build:
	$(GRADLE) build

test:
	$(GRADLE) test

run:
	java "-javaagent:jacocoagent.jar=destfile=build/jacoco/runtime.exec,includes=com.yubi.coverage.*,append=true" -jar build/libs/coverage-0.0.1-SNAPSHOT.jar

report:
	java -jar jacococli.jar report build/jacoco/runtime.exec \
      --classfiles build/classes/java/main/ \
      --sourcefiles src/main/java/ \
      --html build/reports/jacoco/runtime/html

clean:
	$(GRADLE) clean

all: clean build run