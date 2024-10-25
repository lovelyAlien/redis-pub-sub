package com.wapl.reidspubsub.repository;

import com.wapl.reidspubsub.domain.Item;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRepository {
  private final Map<Long, Item> store = new ConcurrentHashMap<>();

  public void saveItem(Item item) {
    store.put(item.getId(), new Item(item.getId(), item.getName(), item.getPrice()));
  }

  public Optional<Item> getItem(Long id) {
    return Optional.ofNullable(store.get(id))
      .map(item -> new Item(item.getId(), item.getName(), item.getPrice()));
  }
}
