package eu.example.reproduceotelredissonoom;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.testcontainers.containers.GenericContainer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class ReproduceOtelRedissonOomApplication {

  private static final String SET_NAME = ReproduceOtelRedissonOomApplication.class.getName() + "_" + "reproduceOtelRedissonOomMap3";
  private static final String BUCKET_NAME = ReproduceOtelRedissonOomApplication.class.getName() + "_" + "reproduceOtelRedissonOomBucket3";
  private static final String LONG_STRING = "a".repeat(1000);


  public static void main(String[] args) throws InterruptedException {
    try (GenericContainer<?> redis = new GenericContainer<>("redis:7.2").withExposedPorts(6379)) {
      redis.start();
      String address = "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort();
      Config config = new Config().setCodec(new Kryo5Codec());
      config.useSingleServer().setAddress(address);
      RedissonClient redissonClient = Redisson.create(config);
      execute(redissonClient);

      Thread.sleep(70000); // Keep the application running for a while to observe behavior
      redissonClient.shutdown();
    }
  }

  @WithSpan
  private static void execute(RedissonClient redissonClient) throws InterruptedException {
    executeTransaction(redissonClient);

    RBucket<UUID> bucket = redissonClient.getBucket(BUCKET_NAME);
    System.out.println("Bucket value: " + bucket.get());
  }

  private static void executeTransaction(RedissonClient redissonClient) {
    RTransaction transaction = redissonClient.createTransaction(TransactionOptions.defaults()
        .timeout(10, TimeUnit.MINUTES));
    RSet<PayloadPojo> txSet = transaction.getSet(SET_NAME);
    RBucket<UUID> txBucket = transaction.getBucket(BUCKET_NAME);

    System.out.println("Start to add items to transaction map");
    for (int i = 0; i < 18000; i++) {
      final var pojo = new PayloadPojo(i);
      txSet.add(pojo);
      txSet.remove(pojo);
      txSet.add(pojo);
      txBucket.set(UUID.randomUUID());
    }
    System.out.println("Commiting transaction");
    transaction.commit();
    System.out.println("Transaction committed.");
  }

  private static class PayloadPojo implements Serializable {
    private UUID id;
    private int numericValue;
    private String payload;
    private List<String> listOfStrings = List.of("a", "b", "c");
    private NestedPojo nestedPojo = new NestedPojo(BigDecimal.valueOf(1232331), "nestedListItem2");

    public PayloadPojo(int numericValue) {
      this.id = UUID.randomUUID();
      this.numericValue = numericValue;
      this.payload = LONG_STRING + numericValue;
    }

    @Override
    public String toString() {
      return "PayloadPojo{" +
          "id=" + id +
          ", numericValue=" + numericValue +
          ", payload='" + payload + '\'' +
          ", listOfStrings=" + listOfStrings +
          ", nestedPojo=" + nestedPojo +
          '}';
    }
  }

  private static class NestedPojo implements Serializable {
    private BigDecimal value;
    private String otherValue;

    public NestedPojo(BigDecimal value, String otherValue) {
      this.value = value;
      this.otherValue = otherValue;
    }

    @Override
    public String toString() {
      return "NestedPojo{" +
          "value=" + value +
          ", otherValue='" + otherValue + '\'' +
          '}';
    }
  }
}
