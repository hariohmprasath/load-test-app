mvn clean install
docker buildx build --platform=linux/amd64 -t load-test .
docker tag load-test:latest 775448517459.dkr.ecr.us-east-1.amazonaws.com/load-test:latest
docker push 775448517459.dkr.ecr.us-east-1.amazonaws.com/load-test