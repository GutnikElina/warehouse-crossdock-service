package com.innowise.warehousecrossdock;

import org.springframework.boot.SpringApplication;

public class TestWarehouseApplication {

    public static void main(String[] args) {
        SpringApplication.from(WarehouseCrossdockServiceApplication::main)
            .with(TestcontainersConfiguration.class)
            .run(args);
    }
}
