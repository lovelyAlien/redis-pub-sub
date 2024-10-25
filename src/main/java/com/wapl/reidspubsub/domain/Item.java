package com.wapl.reidspubsub.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Item {
  private Long id;
  private String name;
  private int price;

  public void updatePrice(int newPrice) {
    this.price = newPrice;
  }
}
