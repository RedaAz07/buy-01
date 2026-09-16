package com.microservices.product_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.microservices.product_service.dto.productRequest;
import com.microservices.product_service.dto.productRspons;
import com.microservices.product_service.exception.ApiException;
import com.microservices.product_service.model.Product;
import com.microservices.product_service.repository.productRepository;
import com.microservices.product_service.service.kafka.ProductEventProducer;

@ExtendWith(MockitoExtension.class)
class productServiceTest {

    @Mock
    private productRepository productRepository;

    @Mock
    private ProductEventProducer productEventProducer;

    @InjectMocks
    private productService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private productRequest productRequest;
    private Product existingProduct;

    @BeforeEach
    void setUp() {
        productRequest = productRequest.builder()
                .name("Laptop")
                .description("A powerful laptop")
                .price(1000)
                .quantity(10)
                .imageUrls(new ArrayList<>())
                .build();

        existingProduct = Product.builder()
                .id("123")
                .name("Laptop")
                .description("A powerful laptop")
                .price(1000)
                .quantity(10)
                .sellerId("seller123")
                .imageUrls(new ArrayList<>())
                .build();
    }

    // ========== AddProduct ==========

    @Test
    void testAddProductSuccess() {
        String sellerId = "seller123";

        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        productRspons result = productService.AddProduct(productRequest, sellerId);

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        assertEquals("seller123", result.getSellerId());
        assertEquals(1000, result.getPrice());
        assertEquals(10, result.getQuantity());

        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertEquals("Laptop", savedProduct.getName());
        assertEquals("seller123", savedProduct.getSellerId());
        assertEquals(1000, savedProduct.getPrice());
        assertEquals(10, savedProduct.getQuantity());
    }

    @Test
    void testAddProductWithImages() {
        List<String> imageUrls = List.of("http://img1.jpg", "http://img2.jpg");
        productRequest reqWithImages = productRequest.builder()
                .name("Phone")
                .description("A phone")
                .price(500)
                .quantity(5)
                .imageUrls(new ArrayList<>(imageUrls))
                .build();

        Product saved = Product.builder()
                .id("456")
                .name("Phone")
                .description("A phone")
                .price(500)
                .quantity(5)
                .sellerId("seller123")
                .imageUrls(new ArrayList<>(imageUrls))
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        productRspons result = productService.AddProduct(reqWithImages, "seller123");

        assertNotNull(result);
        assertEquals("Phone", result.getName());
        assertEquals(2, result.getImageUrls().size());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testAddProductMissingName() {
        productRequest.setName("");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.AddProduct(productRequest, "seller123"));

        assertEquals("all fields are required", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testAddProductMissingDescription() {
        productRequest.setDescription("");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.AddProduct(productRequest, "seller123"));

        assertEquals("all fields are required", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testAddProductMissingDescriptionNull() {
        productRequest.setDescription(null);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.AddProduct(productRequest, "seller123"));

        assertEquals("all fields are required", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testAddProductMissingPrice() {
        productRequest.setPrice(0);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.AddProduct(productRequest, "seller123"));

        assertEquals("all fields are required", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testAddProductMissingQuantity() {
        productRequest.setQuantity(0);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.AddProduct(productRequest, "seller123"));

        assertEquals("all fields are required", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testAddProductNameNull() {
        productRequest.setName(null);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.AddProduct(productRequest, "seller123"));

        assertEquals("all fields are required", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    // ========== getProduct ==========

    @Test
    void testGetProductByIdSuccess() {
        when(productRepository.findById("123")).thenReturn(Optional.of(existingProduct));

        productRspons result = productService.getProduct("123");

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals("A powerful laptop", result.getDescription());
        assertEquals(1000, result.getPrice());
        assertEquals(10, result.getQuantity());
        assertEquals("seller123", result.getSellerId());
        verify(productRepository).findById("123");
    }

    @Test
    void testGetProductByIdNotFound() {
        when(productRepository.findById("999")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.getProduct("999"));

        assertEquals("Product not found", exception.getMessage());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatus());
        verify(productRepository).findById("999");
    }

    // ========== getall ==========

    @Test
    void testGetAllProductsSuccess() {
        Product product2 = Product.builder()
                .id("456")
                .name("Phone")
                .description("A phone")
                .price(500)
                .quantity(20)
                .sellerId("seller456")
                .imageUrls(new ArrayList<>())
                .build();

        when(productRepository.findAll()).thenReturn(List.of(existingProduct, product2));

        List<productRspons> result = productService.getall();

        assertEquals(2, result.size());
        assertEquals("123", result.get(0).getId());
        assertEquals("Laptop", result.get(0).getName());
        assertEquals("456", result.get(1).getId());
        assertEquals("Phone", result.get(1).getName());
        verify(productRepository).findAll();
    }

    @Test
    void testGetAllProductsEmpty() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<productRspons> result = productService.getall();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository).findAll();
    }

    // ========== getMyProducts ==========

    @Test
    void testGetMyProductsSuccess() {
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("id").descending());
        Page<Product> productPage = new PageImpl<>(List.of(existingProduct), pageable, 1);

        when(productRepository.findBySellerId("seller123", pageable)).thenReturn(productPage);

        Page<productRspons> result = productService.getMyProducts("seller123", 10, 0);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("123", result.getContent().get(0).getId());
        assertEquals("Laptop", result.getContent().get(0).getName());
        verify(productRepository).findBySellerId("seller123", pageable);
    }

    @Test
    void testGetMyProductsEmpty() {
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("id").descending());
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(productRepository.findBySellerId("seller123", pageable)).thenReturn(emptyPage);

        Page<productRspons> result = productService.getMyProducts("seller123", 10, 0);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(productRepository).findBySellerId("seller123", pageable);
    }

    @Test
    void testGetMyProductsPagination() {
        Pageable pageable = PageRequest.of(1, 5, org.springframework.data.domain.Sort.by("id").descending());
        Product product2 = Product.builder()
                .id("789")
                .name("Tablet")
                .description("A tablet")
                .price(300)
                .quantity(15)
                .sellerId("seller123")
                .imageUrls(new ArrayList<>())
                .build();
        Page<Product> productPage = new PageImpl<>(List.of(product2), pageable, 2);

        when(productRepository.findBySellerId("seller123", pageable)).thenReturn(productPage);

        Page<productRspons> result = productService.getMyProducts("seller123", 5, 1);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("789", result.getContent().get(0).getId());
        verify(productRepository).findBySellerId("seller123", pageable);
    }

    // ========== UpdateProduct ==========

    @Test
    void testUpdateProductSuccess() {
        when(productRepository.findById("123")).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        productRspons result = productService.UpdateProduct(productRequest, "123", "seller123");

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        assertEquals(1000, result.getPrice());

        verify(productRepository).findById("123");
        verify(productRepository).save(productCaptor.capture());
        Product updatedProduct = productCaptor.getValue();
        assertEquals("Laptop", updatedProduct.getName());
        assertEquals(1000, updatedProduct.getPrice());
    }

    @Test
    void testUpdateProductNotFound() {
        when(productRepository.findById("999")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.UpdateProduct(productRequest, "999", "seller123"));

        assertEquals("Product not found", exception.getMessage());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatus());
        verify(productRepository).findById("999");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testUpdateProductUnauthorized() {
        when(productRepository.findById("123")).thenReturn(Optional.of(existingProduct));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.UpdateProduct(productRequest, "123", "differentSeller"));

        assertEquals("You are not allowed to update this product", exception.getMessage());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getStatus());
        verify(productRepository).findById("123");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testUpdateProductChangesFields() {
        productRequest.setName("Gaming Laptop");
        productRequest.setPrice(2000);
        productRequest.setQuantity(5);
        productRequest.setDescription("High-end gaming laptop");

        when(productRepository.findById("123")).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        productService.UpdateProduct(productRequest, "123", "seller123");

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals("Gaming Laptop", saved.getName());
        assertEquals(2000, saved.getPrice());
        assertEquals(5, saved.getQuantity());
        assertEquals("High-end gaming laptop", saved.getDescription());
    }

    // ========== DeleteProduct ==========

    @Test
    void testDeleteProductSuccess() {
        when(productRepository.findById("123")).thenReturn(Optional.of(existingProduct));
        doNothing().when(productRepository).delete(existingProduct);
        doNothing().when(productEventProducer).sendProductDeletedEvent("123", "Product Deleted");

        productService.DeleteProduct("123", "seller123");

        verify(productRepository).findById("123");
        verify(productRepository).delete(existingProduct);
        verify(productEventProducer).sendProductDeletedEvent("123", "Product Deleted");
    }

    @Test
    void testDeleteProductNotFound() {
        when(productRepository.findById("999")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.DeleteProduct("999", "seller123"));

        assertEquals("Product not found", exception.getMessage());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatus());
        verify(productRepository).findById("999");
        verify(productRepository, never()).delete(any(Product.class));
        verify(productEventProducer, never()).sendProductDeletedEvent(any(), any());
    }

    @Test
    void testDeleteProductUnauthorized() {
        when(productRepository.findById("123")).thenReturn(Optional.of(existingProduct));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.DeleteProduct("123", "differentSeller"));

        assertEquals("You are not allowed to delete this product", exception.getMessage());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getStatus());
        verify(productRepository).findById("123");
        verify(productRepository, never()).delete(any(Product.class));
        verify(productEventProducer, never()).sendProductDeletedEvent(any(), any());
    }

    // ========== getUserProduct ==========

    @Test
    void testGetUserProductTrue() {
        when(productRepository.existsByIdAndSellerId("123", "seller123")).thenReturn(true);

        boolean result = productService.getUserProduct("123", "seller123");

        assertTrue(result);
        verify(productRepository).existsByIdAndSellerId("123", "seller123");
    }

    @Test
    void testGetUserProductFalse() {
        when(productRepository.existsByIdAndSellerId("123", "otherSeller")).thenReturn(false);

        boolean result = productService.getUserProduct("123", "otherSeller");

        assertFalse(result);
        verify(productRepository).existsByIdAndSellerId("123", "otherSeller");
    }

    // ========== Edge cases ==========

    @Test
    void testAddProductDoesNotCallDeleteOrEvent() {
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        productService.AddProduct(productRequest, "seller123");

        verify(productRepository, never()).delete(any(Product.class));
        verify(productEventProducer, never()).sendProductDeletedEvent(any(), any());
    }

    @Test
    void testDeleteProductDoesNotCallSave() {
        when(productRepository.findById("123")).thenReturn(Optional.of(existingProduct));
        doNothing().when(productRepository).delete(existingProduct);
        doNothing().when(productEventProducer).sendProductDeletedEvent("123", "Product Deleted");

        productService.DeleteProduct("123", "seller123");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testGetAllProductsDoesNotCallSave() {
        when(productRepository.findAll()).thenReturn(List.of(existingProduct));

        productService.getall();

        verify(productRepository, never()).save(any(Product.class));
    }
}
