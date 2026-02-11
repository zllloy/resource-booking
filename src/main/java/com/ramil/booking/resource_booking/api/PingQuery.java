package com.ramil.booking.resource_booking.api;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PingQuery {

  @QueryMapping
  public String ping() {
    return "pong";
  }
}
