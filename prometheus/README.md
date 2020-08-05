Right now, run prometheus by doing the following:

    docker build -t my-prometheus .
    docker run -p 9090:9090 my-prometheus 
    
TODO - some docker-compose shizzle to make this and grafana work together effortlessly
