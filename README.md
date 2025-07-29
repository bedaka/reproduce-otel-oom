- run `mvn package`
- start otel backend in second terminal: `docker run --name lgtm -p 3000:3000 -p 4317:4317 -p 4318:4318 --rm -ti grafana/otel-lgtm`
- execute test `java -Xmx700m -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -javaagent:opentelemetry-javaagent.jar -jar target/reproduceOtelRedissonOOM-0.0.1-SNAPSHOT.jar`
  - this should fail wih an OOM error.
- you can investigate the results in the otel lgtm UI at `http://localhost:3000` (use `admin`/`admin` as credentials)
  - e.g. [here](http://localhost:3000/explore?schemaVersion=1&panes=%7B%229v9%22:%7B%22datasource%22:%22tempo%22,%22queries%22:%5B%7B%22refId%22:%22A%22,%22datasource%22:%7B%22type%22:%22tempo%22,%22uid%22:%22tempo%22%7D,%22queryType%22:%22traceqlSearch%22,%22limit%22:20,%22tableType%22:%22traces%22,%22metricsQueryType%22:%22range%22,%22filters%22:%5B%7B%22id%22:%222c6d4e4a%22,%22operator%22:%22%3D%22,%22scope%22:%22span%22%7D%5D%7D%5D,%22range%22:%7B%22from%22:%22now-1h%22,%22to%22:%22now%22%7D%7D%7D&orgId=1)

the same execution with otel agent disabled:
- `java -Xmx700m -Dotel.instrumentation.redisson.enabled=false -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -javaagent:opentelemetry-javaagent.jar -
jar target/reproduceOtelRedissonOOM-0.0.1-SNAPSHOT.jar 
  - this should succeed, even though redis might log RedisTimeoutExceptions due to the high load it will eventually finish (docker stats shows the container is still busy).
`