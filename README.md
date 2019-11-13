# Demo Server
This is a demo server written in scala, which exposes a single api ```/v1/api/users``` and 
exports some JMX metrics
 
## How to run
### Prerequisites
#### sbt
install sbt on your laptop:
```
wget https://piccolo.link/sbt-1.3.3.zip
unzip sbt-1.3.3.zip
export PATH=./sbt/bin:$PATH
```

### Running the app
From the command prompt issue:
```
./runme.sh
```

### Execute some calls
```
for l in {1..50}; do curl localhost:8080/api/v1/users; done
```

### Check JMX values exported 
* Open your browser at ```http://localhost:8081```
* Check upstream metrics are exported 
