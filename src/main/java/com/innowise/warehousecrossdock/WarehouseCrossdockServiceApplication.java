package com.innowise.warehousecrossdock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public final class WarehouseCrossdockServiceApplication {
  /**
   * Main method to start the Spring Boot application.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    Class<?> appClass = WarehouseCrossdockServiceApplication.class;
    SpringApplication.run(appClass, args);
  }

  private WarehouseCrossdockServiceApplication() {
    super();
  }
}
