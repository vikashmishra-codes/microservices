package com.vikash.OrderService.service;

import com.vikash.OrderService.dto.InventoryResponse;
import com.vikash.OrderService.dto.OrderLineItemsDto;
import com.vikash.OrderService.dto.OrderRequest;
import com.vikash.OrderService.model.Order;
import com.vikash.OrderService.model.OrderLineItems;
import com.vikash.OrderService.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

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
        InventoryResponse[] inventoryResponseArray = webClient.get()
                        .uri("http://localhost:8802/api/inventory",
                                uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes.toArray()).build())
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();


//        assert inventoryResponsesArray != null;
        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(InventoryResponse::isInStock);

        if(allProductsInStock) {
            orderRepository.save(order);
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
