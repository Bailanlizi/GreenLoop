package com.campus.trade.service.impl;

import com.campus.trade.config.HybridRecommendationProperties;
import com.campus.trade.config.HybridSearchProperties;
import com.campus.trade.domain.ProductStatus;
import com.campus.trade.dto.AdminProductDTO;
import com.campus.trade.dto.PageResult;
import com.campus.trade.dto.ProductDTO;
import com.campus.trade.entity.Product;
import com.campus.trade.exception.CustomException;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.security.AuthenticatedUser;
import com.campus.trade.service.ProductEmbeddingService;
import com.campus.trade.service.ProductRecommendationService;
import com.campus.trade.service.ProductRiskService;
import com.campus.trade.service.ProductSemanticSearchService;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.ProductDemandService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductEmbeddingService productEmbeddingService;
    private final ProductSemanticSearchService productSemanticSearchService;
    private final ProductRecommendationService productRecommendationService;
    private final ProductRiskService productRiskService;
    private final ProductDemandService productDemandService;
    private final HybridSearchProperties hybridSearchProperties;
    private final HybridRecommendationProperties hybridRecommendationProperties;

    @Autowired
    public ProductServiceImpl(ProductMapper productMapper,
                              ProductImageMapper productImageMapper,
                              ProductEmbeddingService productEmbeddingService,
                              ProductSemanticSearchService productSemanticSearchService,
                              ProductRecommendationService productRecommendationService,
                              ProductRiskService productRiskService,
                              ProductDemandService productDemandService,
                              HybridSearchProperties hybridSearchProperties,
                              HybridRecommendationProperties hybridRecommendationProperties) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.productEmbeddingService = productEmbeddingService;
        this.productSemanticSearchService = productSemanticSearchService;
        this.productRecommendationService = productRecommendationService;
        this.productRiskService = productRiskService;
        this.productDemandService = productDemandService;
        this.hybridSearchProperties = hybridSearchProperties;
        this.hybridRecommendationProperties = hybridRecommendationProperties;
    }

    @Override
//    @Caching
    @Cacheable("products")
    public List<Product> getProducts(String keyword, Integer categoryId, Double minPrice, Double maxPrice, String orderBy) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }
        return productMapper.findProducts(normalizedKeyword, categoryId, minPrice, maxPrice, orderBy);
    }

    @Override
    public List<Product> searchProducts(String keyword, Integer categoryId, Double minPrice, Double maxPrice,
                                        String orderBy, String searchMode) {
        String mode = searchMode == null ? "standard" : searchMode.trim();
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }
        if (!"semantic".equalsIgnoreCase(mode) && !"hybrid".equalsIgnoreCase(mode) || normalizedKeyword == null) {
            return getProducts(keyword, categoryId, minPrice, maxPrice, orderBy);
        }
        int candidateLimit = hybridSearchProperties.getCandidateLimit();
        if (candidateLimit <= 0) {
            candidateLimit = 200;
        }
        List<Product> candidates = productMapper.findProductsWithLimit(normalizedKeyword, categoryId, minPrice, maxPrice, orderBy, candidateLimit);
        if ("hybrid".equalsIgnoreCase(mode)) {
            return productSemanticSearchService.rankHybrid(normalizedKeyword, candidates);
        }
        return productSemanticSearchService.rank(normalizedKeyword, candidates);
    }

    /**
     * 【最终诊断修正】
     * 我已将 @Cacheable 注解暂时注释掉。
     * 这会强制此方法在每一次被调用时，都必须去数据库执行一次全新的查询，
     * 从而彻底绕过任何可能存在的、被污染的旧缓存。
     */
    @Override
    // @Cacheable(value = "product", key = "#productId")
    public Product getProductById(String productId) {
        log.info(">>> [缓存已禁用] 正在为商品ID {} 从数据库强制查询详情...", productId);
        Product product = productMapper.findProductById(productId);
        if (product == null) {
            throw new CustomException("商品不存在或已下架");
        }
        log.info("<<< 数据库查询成功，商品附图数量为: {}", (product.getImageUrls() != null ? product.getImageUrls().size() : 0));
        return product;
    }

    /**
     * 【最终修正】
     * 使用 @CacheEvict 注解，在创建新商品后，同时清除 "products" (列表) 和 "product" (详情) 这两个缓存。
     * allEntries = true 表示将这两个缓存中的所有条目全部清除。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "product"}, allEntries = true)
    public Product createProduct(ProductDTO productDTO, String sellerId) {
        log.info(">>> [新增商品] 清除 'products' 和 'product' 缓存。");
        Product product = new Product();
        product.setTitle(productDTO.getTitle());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setCategoryId(productDTO.getCategoryId());
        product.setConditionLevel(productDTO.getConditionLevel());
        product.setCoverImage(productDTO.getCoverImage());
        if (productDTO.getDeliveryOptions() != null) {
            product.setDeliveryOptions(String.join(",", productDTO.getDeliveryOptions()));
        }
        product.setSellerId(sellerId);

        productMapper.insertProduct(product);

        List<String> imageUrls = productDTO.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            productImageMapper.batchInsert(product.getId(), imageUrls);
        }

        Product created = productMapper.findProductById(product.getId());
        productEmbeddingService.upsertEmbedding(created);
        productRiskService.recordIfRisk(created.getId(), productRiskService.evaluate(created));
        productDemandService.notifyMatches(created);
        return created;
    }


    @Override
    @Transactional
    @CacheEvict(value = {"product::#productId", "products"}, allEntries = true)
    public Product updateProduct(String productId, ProductDTO productDTO, String currentUserId) {
        Product existingProduct = productMapper.findProductById(productId);
        if (existingProduct == null) {
            throw new CustomException("商品不存在");
        }
        if (!Objects.equals(existingProduct.getSellerId(), currentUserId)) {
            throw new CustomException("无权修改他人的商品");
        }
        if (ProductStatus.LOCKED.name().equals(existingProduct.getStatus())
                || ProductStatus.SOLD.name().equals(existingProduct.getStatus())) {
            throw new CustomException("商品已被锁定或售出，无法编辑");
        }

        existingProduct.setTitle(productDTO.getTitle());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setCategoryId(productDTO.getCategoryId());
        existingProduct.setConditionLevel(productDTO.getConditionLevel());
        if (productDTO.getCoverImage() != null && !productDTO.getCoverImage().isEmpty()) {
            existingProduct.setCoverImage(productDTO.getCoverImage());
        }

        productMapper.updateProduct(existingProduct);

        productImageMapper.deleteByProductId(productId);
        if (!CollectionUtils.isEmpty(productDTO.getImageUrls())) {
            productImageMapper.batchInsert(productId, productDTO.getImageUrls());
        }

        Product updated = productMapper.findProductById(productId);
        productEmbeddingService.upsertEmbedding(updated);
        productRiskService.recordIfRisk(updated.getId(), productRiskService.evaluate(updated));
        return updated;
    }

    @Override
    @CacheEvict(value = {"product::#productId", "products"}, allEntries = true)
    public void updateProductStatus(String productId, String status, AuthenticatedUser user) {
        Product existingProduct = productMapper.findProductById(productId);
        if (existingProduct == null) {
            throw new CustomException("商品不存在");
        }

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
        if (!isAdmin && !Objects.equals(existingProduct.getSellerId(), user.getUserId())) {
            throw new CustomException("无权修改他人的商品");
        }
        if (!ProductStatus.AVAILABLE.name().equals(existingProduct.getStatus())
                && !ProductStatus.DELISTED.name().equals(existingProduct.getStatus())) {
            throw new CustomException("锁定或已售出的商品不能变更状态");
        }
        if (!ProductStatus.AVAILABLE.name().equals(status) && !ProductStatus.DELISTED.name().equals(status)) {
            throw new CustomException("不支持的商品状态变更");
        }
        productMapper.updateProductStatus(productId, status);
    }

    @Override
    public PageResult<Product> findAllProductsForAdmin(String keyword, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        // 【最终修正】采用与 UserServiceImpl 完全一致的、更健壮的实现方式
        List<Product> productList = productMapper.findAllForAdmin(keyword);
        return new PageResult<>(productList);
    }

    @Override
    @Cacheable(value = "recommendations", key = "#productId")
    public List<Product> getRecommendedProducts(String productId) {
        int candidateLimit = hybridRecommendationProperties.getCandidateLimit();
        if (candidateLimit <= 0) {
            candidateLimit = 50;
        }
        int resultLimit = hybridRecommendationProperties.getResultLimit();
        if (resultLimit <= 0) {
            resultLimit = 5;
        }
        List<ProductRecommendationService.ScoredProduct> embeddingScores =
                productRecommendationService.scoreByEmbedding(productId, candidateLimit);
        List<Product> collaborative = productMapper.findRecommendedProducts(productId, candidateLimit);

        List<Product> combined = combineRecommendations(embeddingScores, collaborative, resultLimit);
        if (!combined.isEmpty()) {
            return combined;
        }
        Product base = productMapper.findProductById(productId);
        if (base != null && base.getCategoryId() != null) {
            return productMapper.findAvailableByCategory(base.getCategoryId(), productId, resultLimit);
        }
        return List.of();
    }

    private List<Product> combineRecommendations(List<ProductRecommendationService.ScoredProduct> embeddingScores,
                                                 List<Product> collaborative,
                                                 int limit) {
        double embeddingWeight = hybridRecommendationProperties.getEmbeddingWeight();
        double cfWeight = hybridRecommendationProperties.getCfWeight();
        double total = embeddingWeight + cfWeight;
        if (total <= 0) {
            embeddingWeight = 0.7;
            cfWeight = 0.3;
            total = 1.0;
        }
        embeddingWeight /= total;
        cfWeight /= total;

        List<RecommendationScore> scores = new ArrayList<>();
        if (embeddingScores != null) {
            for (ProductRecommendationService.ScoredProduct scored : embeddingScores) {
                if (scored == null || scored.getProduct() == null || scored.getProduct().getId() == null) {
                    continue;
                }
                scores.add(new RecommendationScore(scored.getProduct(), scored.getScore() * embeddingWeight));
            }
        }

        if (collaborative != null && !collaborative.isEmpty()) {
            int size = collaborative.size();
            for (int i = 0; i < size; i++) {
                Product product = collaborative.get(i);
                if (product == null || product.getId() == null) {
                    continue;
                }
                double rankScore = size == 1 ? 1.0 : 1.0 - (double) i / (double) (size - 1);
                scores.add(new RecommendationScore(product, rankScore * cfWeight));
            }
        }

        if (scores.isEmpty()) {
            return List.of();
        }

        scores = mergeScores(scores);
        scores.sort((a, b) -> Double.compare(b.score, a.score));

        List<Product> result = new ArrayList<>();
        for (RecommendationScore score : scores) {
            result.add(score.product);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<RecommendationScore> mergeScores(List<RecommendationScore> scores) {
        List<RecommendationScore> merged = new ArrayList<>();
        for (RecommendationScore score : scores) {
            RecommendationScore existing = null;
            for (RecommendationScore item : merged) {
                if (item.product.getId().equals(score.product.getId())) {
                    existing = item;
                    break;
                }
            }
            if (existing == null) {
                merged.add(new RecommendationScore(score.product, score.score));
            } else {
                existing.score += score.score;
            }
        }
        return merged;
    }

    private static class RecommendationScore {
        private final Product product;
        private double score;

        private RecommendationScore(Product product, double score) {
            this.product = product;
            this.score = score;
        }
    }

    @Override
    public List<Product> getMyProducts(String sellerId) {
        return productMapper.findBySeller(sellerId);
    }

    /**
     * 【新增】为管理员提供的更新方法。
     * 它不检查当前用户是否是商品所有者，因为管理员有权编辑任何商品。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"product::#productId", "products"}, allEntries = true)
    public Product updateProductByAdmin(String productId, ProductDTO productDTO) {
        Product existingProduct = productMapper.findProductById(productId);
        if (existingProduct == null) {
            throw new CustomException("商品不存在");
        }
        if (ProductStatus.LOCKED.name().equals(existingProduct.getStatus())
                || ProductStatus.SOLD.name().equals(existingProduct.getStatus())) {
            throw new CustomException("商品已被锁定或售出，无法编辑");
        }

        // 基本信息
        existingProduct.setTitle(productDTO.getTitle());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setCategoryId(productDTO.getCategoryId());
        existingProduct.setConditionLevel(productDTO.getConditionLevel());

        // 主图（无论是否为空都 set，交由 SQL 控制）
        existingProduct.setCoverImage(productDTO.getCoverImage());

        // 配送方式（为空时 set 空字符串，保证 SQL 能更新，只用'SHIPPING'）
        if (productDTO.getDeliveryOptions() != null) {
            existingProduct.setDeliveryOptions(String.join(",", productDTO.getDeliveryOptions()));
        } else {
            existingProduct.setDeliveryOptions("");
        }

        // 更新商品主表
        productMapper.updateProduct(existingProduct);

        // 附图（最多3张，前端应限制）
        productImageMapper.deleteByProductId(productId);
        if (productDTO.getImageUrls() != null && !productDTO.getImageUrls().isEmpty()) {
            List<String> images = productDTO.getImageUrls();
            if (images.size() > 3) {
                images = images.subList(0, 3);
            }
            productImageMapper.batchInsert(productId, images);
        }

        return productMapper.findProductById(productId);
    }

    /**
     * 【新增】为管理员提供的删除方法。
     * 注意：这将永久删除商品及其所有关联数据（如收藏、订单等，取决于数据库的外键级联设置）。
     */
    @Override
    @CacheEvict(value = {"product::#productId", "products"}, allEntries = true)
    public void deleteProduct(String productId) {
        Product product = productMapper.findProductById(productId);
        if (product == null) {
            throw new CustomException("商品不存在");
        }
        if (ProductStatus.LOCKED.name().equals(product.getStatus()) || ProductStatus.SOLD.name().equals(product.getStatus())) {
            throw new CustomException("锁定或已售出的商品不能删除");
        }
        productMapper.deleteById(productId);
    }

    /**
     * 【新增】为管理员创建商品的方法实现。
     * 它直接使用 DTO 中提供的 sellerId。
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product createProductByAdmin(AdminProductDTO adminProductDTO) {
        Product product = new Product();
        product.setTitle(adminProductDTO.getTitle());
        product.setDescription(adminProductDTO.getDescription());
        product.setPrice(adminProductDTO.getPrice());
        product.setCategoryId(adminProductDTO.getCategoryId());
        product.setConditionLevel(adminProductDTO.getConditionLevel());
        product.setCoverImage(adminProductDTO.getCoverImage());
        product.setSellerId(adminProductDTO.getSellerId()); // 使用指定的卖家ID
        if (adminProductDTO.getDeliveryOptions() != null) {
            product.setDeliveryOptions(adminProductDTO.getDeliveryOptions());
        }

        productMapper.insertProduct(product);

        List<String> imageUrls = adminProductDTO.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            productImageMapper.batchInsert(product.getId(), imageUrls);
        }

        Product created = productMapper.findProductById(product.getId());
        productDemandService.notifyMatches(created);
        return created;
    }
}
