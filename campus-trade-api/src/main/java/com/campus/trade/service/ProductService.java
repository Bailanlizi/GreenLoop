package com.campus.trade.service;

import com.campus.trade.dto.AdminProductDTO;
import com.campus.trade.dto.PageResult;
import com.campus.trade.dto.ProductDTO;
import com.campus.trade.entity.Product;
import com.campus.trade.security.AuthenticatedUser;
import java.util.List;

public interface ProductService {

    PageResult<Product> getProducts(String keyword, Integer categoryId, Double minPrice, Double maxPrice,
                                    String orderBy, Integer page, Integer size);

    PageResult<Product> searchProducts(String keyword, Integer categoryId, Double minPrice, Double maxPrice,
                                       String orderBy, String searchMode, Integer page, Integer size);

    Product getProductById(String productId);

    Product createProduct(ProductDTO productDTO, String sellerId);

    Product updateProduct(String productId, ProductDTO productDTO, String currentUserId);

    void updateProductStatus(String productId, String status, AuthenticatedUser user);

    PageResult<Product> findAllProductsForAdmin(String keyword, Integer page, Integer size);

    List<Product> getRecommendedProducts(String productId);

    List<Product> getMyProducts(String sellerId);

    Product updateProductByAdmin(String productId, ProductDTO productDTO);

    void deleteProduct(String productId);

    Product createProductByAdmin(AdminProductDTO adminProductDTO);
}
