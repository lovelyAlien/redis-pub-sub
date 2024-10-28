package com.wapl.reidspubsub.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wapl.reidspubsub.domain.Item;
import com.wapl.reidspubsub.repository.ItemRepository;
import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

public class ItemServiceWithPubSub {

  public static final String CHANNEL = "item-cache-invalidation";

  private final Cache<Long, Item> cache;
  private final ItemRepository repository;
  private final Consumer<Long> cacheInvalidationMessagePublisher;

  public ItemServiceWithPubSub(ItemRepository repository, RedisClient client) {
    this.cache = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(10)).build();
    this.repository = repository;
    RedisPubSubCommands<String, String> connectionForSub = client.connectPubSub().sync();
    connectionForSub.getStatefulConnection()
      .addListener(new RedisPubSubAdapter<>() { // 캐시를 만료시키는 리스너 추가
        @Override
        public void message(String channel, String message) {
          cache.invalidate(Long.parseLong(message));
        }
      });
    connectionForSub.subscribe(CHANNEL); // 해당 채널을 구독

    RedisPubSubCommands<String, String> connectionForPub = client.connectPubSub().sync();

    // 캐시 무효화 메시지를 전송하는 Publsuher 추가
    this.cacheInvalidationMessagePublisher = id -> connectionForPub.publish(CHANNEL, id.toString());
  }

  public void addItem(Item item) {
    repository.saveItem(item);
    cache.put(item.getId(), item);
  }

  public void updateItemPrice(Long id, int price) {
    Item item = repository.getItem(id).orElseThrow(() -> new IllegalArgumentException("cannot find item. id: " + id));
    item.updatePrice(price);
    addItem(item);
    cacheInvalidationMessagePublisher.accept(id); // cache invalidation message 전송
  }

  public Item getItem(Long id) {
    Item itemFromCache = cache.getIfPresent(id);
    if (itemFromCache == null) {
      Optional<Item> itemFromDB = repository.getItem(id);
      itemFromDB.ifPresent(item -> cache.put(item.getId(), item));
      return itemFromDB.orElseThrow(() -> new IllegalArgumentException("cannot find item. id: " + id));
    }
    return itemFromCache;
  }
}
