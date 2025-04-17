GRADLE := ./gradlew

build:
	$(GRADLE) build

test:
	$(GRADLE) test

run:
	java "-javaagent:jacocoagent.jar=output=tcpserver,address=*,port=8008,includes=com.yubi.coverage.*" -jar build/libs/coverage-0.0.1-SNAPSHOT.jar

# Report is now generated via the REST API at /api/coverage/report
report:
	curl -X GET http://localhost:8080/api/coverage/report

# You can also check agent status
status:
	curl -X GET http://localhost:8080/api/coverage/status

# Reset coverage data
reset:
	curl -X POST http://localhost:8080/api/coverage/reset

# Save coverage data to a file
save:
	curl -X GET http://localhost:8080/api/coverage/save

clean:
	$(GRADLE) clean

all: clean build run