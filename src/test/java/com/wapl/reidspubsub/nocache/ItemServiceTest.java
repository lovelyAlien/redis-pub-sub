package com.wapl.reidspubsub.nocache;

import com.wapl.reidspubsub.domain.Item;
import com.wapl.reidspubsub.repository.ItemRepository;
import com.wapl.reidspubsub.service.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemServiceTest {
  @Test
  @DisplayName("Item 가격이 수정될 때 캐시 만료 시간 전 까진 Item을 수정한 서버에서만 수정된 가격이 제공된다.")
  void localCacheInvalidationTest() throws Exception {
    ItemRepository repository = new ItemRepository();
    ItemService serviceForServer1 = new ItemService(repository);
    ItemService serviceForServer2 = new ItemService(repository);
    ItemService serviceForServer3 = new ItemService(repository);

    Item item1 = new Item(1L, "item1", 5000);
    serviceForServer1.addItem(item1);
    assertThat(serviceForServer1.getItem(item1.getId()).getPrice()).isEqualTo(5000); // check item & store item to local cache(server1)
    assertThat(serviceForServer2.getItem(item1.getId()).getPrice()).isEqualTo(5000); // check item & store item to local cache(server2)
    assertThat(serviceForServer3.getItem(item1.getId()).getPrice()).isEqualTo(5000); // check item & store item to local cache(server3)

    serviceForServer1.updateItemPrice(1L, 3000); // update item price in server1

    assertThat(serviceForServer1.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server1)
    assertThat(serviceForServer2.getItem(item1.getId()).getPrice()).isEqualTo(5000); // not updated price(server2)
    assertThat(serviceForServer3.getItem(item1.getId()).getPrice()).isEqualTo(5000); // not updated price(server3)

    Thread.sleep(2000); // wait for cache invalidation

    assertThat(serviceForServer1.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server1)
    assertThat(serviceForServer2.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server2)
    assertThat(serviceForServer3.getItem(item1.getId()).getPrice()).isEqualTo(3000); // updated price(server3)
  }
}
