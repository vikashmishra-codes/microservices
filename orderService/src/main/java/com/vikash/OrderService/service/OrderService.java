package com.vikash.OrderService.service;

import com.vikash.OrderService.dto.InventoryResponse;
import com.vikash.OrderService.dto.OrderLineItemsDto;
import com.vikash.OrderService.dto.OrderRequest;
import com.vikash.OrderService.event.OrderPlacedEvent;
import com.vikash.OrderService.model.Order;
import com.vikash.OrderService.model.OrderLineItems;
import com.vikash.OrderService.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.SetOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();
//      call to inventory service and place order if product is in
//      stock
        log.info("Inventory API raw response started :");
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                        .uri("http://inventoryservice/api/inventory",
                                uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();
//        String url = webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .scheme("http")
//                        .host("localhost")
//                        .port(8802)
//                        .path("/api/inventory")
//                        .queryParam("skuCode", (Object) skuCodes.toArray(new String[0]))
//                        .build()
//                )
//                .build()
//                .toString();
//        log.info("Calling Inventory Service with URL: {}", url);
        log.info("Inventory API raw response: {}", (Object) inventoryResponseArray);
//        assert inventoryResponsesArray != null;
        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(InventoryResponse::getIsInStock);

        if(allProductsInStock) {
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
        }else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
    }

    public OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto){
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;

    }
}
