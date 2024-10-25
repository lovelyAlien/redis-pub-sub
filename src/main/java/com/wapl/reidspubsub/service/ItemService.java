package com.wapl.reidspubsub.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wapl.reidspubsub.domain.Item;
import com.wapl.reidspubsub.repository.ItemRepository;

import java.time.Duration;
import java.util.Optional;

public class ItemService {
  private final Cache<Long, Item> cache;
  private final ItemRepository repository;

  public ItemService(ItemRepository repository) {
    this.cache = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(1))
      .build();
    this.repository = repository;
  }

  public void addItem(Item item) {
    repository.saveItem(item);
    cache.put(item.getId(), item);
  }

  public void updateItemPrice(Long id, int price) {
    Item item = repository.getItem(id)
      .orElseThrow(() -> new IllegalArgumentException("cannot find item. id: " + id));
    item.updatePrice(price);
    addItem(item);
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