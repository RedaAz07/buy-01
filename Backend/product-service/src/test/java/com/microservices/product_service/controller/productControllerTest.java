package com.microservices.product_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.microservices.product_service.dto.productRequest;
import com.microservices.product_service.dto.productRspons;
import com.microservices.product_service.service.productService;

@ExtendWith(MockitoExtension.class)
class productControllerTest {

    @Mock
    private productService productService;

    @InjectMocks
    private productController productController;

    private productRspons product1;
    private productRspons product2;
    private Principal principal;
    private productRequest productRequest;

    @BeforeEach
    void setUp() {
        product1 = productRspons.builder()
                .id("123")
                .name("Laptop")
                .description("A powerful laptop")
                .price(1000)
                .quantity(10)
                .sellerId("seller123")
                .build();

        product2 = productRspons.builder()
                .id("456")
                .name("Phone")
                .description("A modern phone")
                .price(500)
                .quantity(20)
                .sellerId("seller456")
                .build();

        principal = new Principal() {
            @Override
            public String getName() {
                return "seller123";
            }
        };

        productRequest = productRequest.builder()
                .name("Laptop")
                .description("A powerful laptop")
                .price(1000)
                .quantity(10)
                .build();
    }

    // ========== GetProduct ==========

    @Test
    void testGetProductByIdSuccess() {
        when(productService.getProduct("123")).thenReturn(product1);

        productRspons result = productController.GetProduct("123");

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals("seller123", result.getSellerId());
        verify(productService).getProduct("123");
    }

    @Test
    void testGetProductByIdNotFound() {
        when(productService.getProduct("999"))
                .thenThrow(new com.microservices.product_service.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Product not found"));

        com.microservices.product_service.exception.ApiException exception = assertThrows(
                com.microservices.product_service.exception.ApiException.class,
                () -> productController.GetProduct("999"));

        assertEquals("Product not found", exception.getMessage());
        verify(productService).getProduct("999");
    }

    // ========== GetallProduct ==========

    @Test
    void testGetAllProductsSuccess() {
        when(productService.getall()).thenReturn(List.of(product1, product2));

        List<productRspons> result = productController.GetallProduct();

        assertEquals(2, result.size());
        assertEquals("123", result.get(0).getId());
        assertEquals("Laptop", result.get(0).getName());
        assertEquals("456", result.get(1).getId());
        assertEquals("Phone", result.get(1).getName());
        verify(productService).getall();
    }

    @Test
    void testGetAllProductsEmpty() {
        when(productService.getall()).thenReturn(List.of());

        List<productRspons> result = productController.GetallProduct();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productService).getall();
    }

    @Test
    void testGetAllProductsFailure() {
        when(productService.getall()).thenThrow(new RuntimeException("No products found"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> productController.GetallProduct());

        assertEquals("No products found", exception.getMessage());
        verify(productService).getall();
    }

    // ========== AddProduct ==========

    @Test
    void testAddProductSuccess() {
        when(productService.AddProduct(productRequest, "seller123")).thenReturn(product1);

        productRspons result = productController.AddProduct(productRequest, principal);

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals("seller123", result.getSellerId());
        verify(productService).AddProduct(productRequest, "seller123");
    }

    @Test
    void testAddProductFailure() {
        doThrow(new RuntimeException("all fields are required"))
                .when(productService)
                .AddProduct(productRequest, "seller123");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> productController.AddProduct(productRequest, principal));

        assertEquals("all fields are required", exception.getMessage());
        verify(productService).AddProduct(productRequest, "seller123");
    }

    // ========== GetMyProducts ==========

    @Test
    void testGetMyProductsSuccess() {
        Page<productRspons> page = new PageImpl<>(List.of(product1));
        when(productService.getMyProducts("seller123", 10, 0)).thenReturn(page);

        Page<productRspons> result = productController.GetMyProducts(0, 10, principal);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("123", result.getContent().get(0).getId());
        verify(productService).getMyProducts("seller123", 10, 0);
    }

    @Test
    void testGetMyProductsEmpty() {
        Page<productRspons> emptyPage = new PageImpl<>(List.of());
        when(productService.getMyProducts("seller123", 10, 0)).thenReturn(emptyPage);

        Page<productRspons> result = productController.GetMyProducts(0, 10, principal);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(productService).getMyProducts("seller123", 10, 0);
    }

    // ========== UpdateProduct ==========

    @Test
    void testUpdateProductSuccess() {
        productRspons updated = productRspons.builder()
                .id("123")
                .name("Updated Laptop")
                .description("Updated description")
                .price(1200)
                .quantity(5)
                .sellerId("seller123")
                .build();
        when(productService.UpdateProduct(productRequest, "123", "seller123")).thenReturn(updated);

        productRspons result = productController.UpdateProduct(productRequest, "123", principal);

        assertNotNull(result);
        assertEquals("Updated Laptop", result.getName());
        assertEquals(1200, result.getPrice());
        verify(productService).UpdateProduct(productRequest, "123", "seller123");
    }

    @Test
    void testUpdateProductUnauthorized() {
        doThrow(new com.microservices.product_service.exception.ApiException(
                org.springframework.http.HttpStatus.FORBIDDEN, "You are not allowed to update this product"))
                .when(productService)
                .UpdateProduct(productRequest, "123", "seller123");

        com.microservices.product_service.exception.ApiException exception = assertThrows(
                com.microservices.product_service.exception.ApiException.class,
                () -> productController.UpdateProduct(productRequest, "123", principal));

        assertEquals("You are not allowed to update this product", exception.getMessage());
        verify(productService).UpdateProduct(productRequest, "123", "seller123");
    }

    // ========== DeleteProduct ==========

    @Test
    void testDeleteProductSuccess() {
        productController.DeleteProduct("123", principal);

        verify(productService).DeleteProduct("123", "seller123");
    }

    @Test
    void testDeleteProductNotFound() {
        doThrow(new com.microservices.product_service.exception.ApiException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Product not found"))
                .when(productService)
                .DeleteProduct("999", "seller123");

        com.microservices.product_service.exception.ApiException exception = assertThrows(
                com.microservices.product_service.exception.ApiException.class,
                () -> productController.DeleteProduct("999", principal));

        assertEquals("Product not found", exception.getMessage());
        verify(productService).DeleteProduct("999", "seller123");
    }

    @Test
    void testDeleteProductUnauthorized() {
        doThrow(new com.microservices.product_service.exception.ApiException(
                org.springframework.http.HttpStatus.FORBIDDEN, "You are not allowed to delete this product"))
                .when(productService)
                .DeleteProduct("123", "seller123");

        com.microservices.product_service.exception.ApiException exception = assertThrows(
                com.microservices.product_service.exception.ApiException.class,
                () -> productController.DeleteProduct("123", principal));

        assertEquals("You are not allowed to delete this product", exception.getMessage());
        verify(productService).DeleteProduct("123", "seller123");
    }

    // ========== getUserProduct ==========

    @Test
    void testGetUserProductTrue() {
        Principal seller123Principal = new Principal() {
            @Override
            public String getName() {
                return "seller123";
            }
        };
        when(productService.getUserProduct("123", "seller123")).thenReturn(true);

        boolean result = productController.getUserProduct("123", seller123Principal);

        assertTrue(result);
        verify(productService).getUserProduct("123", "seller123");
    }

    @Test
    void testGetUserProductFalse() {
        Principal otherPrincipal = new Principal() {
            @Override
            public String getName() {
                return "otherSeller";
            }
        };
        when(productService.getUserProduct("123", "otherSeller")).thenReturn(false);

        boolean result = productController.getUserProduct("123", otherPrincipal);

        assertFalse(result);
        verify(productService).getUserProduct("123", "otherSeller");
    }

    // ========== Verify no service calls on wrong path ==========

    @Test
    void testGetProductDoesNotCallAddProduct() {
        when(productService.getProduct("123")).thenReturn(product1);

        productController.GetProduct("123");

        verify(productService).getProduct("123");
        verify(productService, never()).AddProduct(productRequest, "seller123");
    }
}
