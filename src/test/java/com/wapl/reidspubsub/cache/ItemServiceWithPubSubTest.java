package com.wapl.reidspubsub.cache;

import com.wapl.reidspubsub.domain.Item;
import com.wapl.reidspubsub.repository.ItemRepository;
import com.wapl.reidspubsub.service.ItemServiceWithPubSub;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemServiceWithPubSubTest {
  @Test
  @DisplayName("Item 가격이 수정되면 모든 서버에서 수정된 가격이 제공된다.")
  void localCacheInvalidationTest() throws Exception {
    ItemRepository repository = new ItemRepository();
    RedisClient client = RedisClient.create("redis://localhost");
    ItemServiceWithPubSub serviceForServer1 = new ItemServiceWithPubSub(repository, client);
    ItemServiceWithPubSub serviceForServer2 = new ItemServiceWithPubSub(repository, client);
    ItemServiceWithPubSub serviceForServer3 = new ItemServiceWithPubSub(repository, client);

    Item item1 = new Item(1L, "item1", 5000);

    serviceForServer1.addItem(item1);
    assertThat(serviceForServer1.getItem(item1.getId()).getPrice()).isEqualTo(5000); // check item & store item to local cache(server1)
    assertThat(serviceForServer2.getItem(item1.getId()).getPrice()).isEqualTo(5000); // check item & store item to local cache(server2)
    assertThat(serviceForServer3.getItem(item1.getId()).getPrice()).isEqualTo(5000); // check item & store item to local cache(server3)

    serviceForServer1.updateItemPrice(1L, 3000);
    Thread.sleep(10); // wait for redis pub/sub

    assertThat(serviceForServer1.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server1)
    assertThat(serviceForServer2.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server2)
    assertThat(serviceForServer3.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server3)

    Thread.sleep(2000); // wait for cache invalidation
    assertThat(serviceForServer1.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server1)
    assertThat(serviceForServer2.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server2)
    assertThat(serviceForServer3.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server3)
  }
}
