package com.maiphuhai.repository;

import com.maiphuhai.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ProductRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    //Row mapper
    private static final RowMapper<Product> PRODUCT_ROW_MAPPER = new RowMapper<Product>() {
        @Override
        public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
            Product product = new Product();

            product.setProductID(rs.getInt("ProductId"));
            product.setCompany(rs.getString("Company"));
            product.setProductName(rs.getString("ProductName"));
            product.setTypeName(rs.getString("TypeName"));
            product.setScreenSize_Inch(rs.getDouble("ScreenSize_Inch"));
            product.setScreenResolution(rs.getString("ScreenResolution"));
            product.setCPU_Company(rs.getString("CPU_Company"));
            product.setCPU_Type(rs.getString("CPU_Type"));
            product.setCPU_Freq_GHz(rs.getDouble("CPU_Freq_GHz"));
            product.setRam_GB(rs.getInt("Ram_GB"));
            product.setStorage(rs.getString("Storage"));
            product.setGPU_Company(rs.getString("GPU_Company"));
            product.setGPU_Type(rs.getString("GPU_Type"));
            product.setWeight_Kg(rs.getDouble("Weight_Kg"));
            product.setOperatingSystem(rs.getString("OperatingSystem"));
            product.setPrice_VND(rs.getDouble("Price_VND"));
            product.setStockQty(rs.getInt("StockQty"));
            product.setCreatedAt(rs.getTimestamp("CreatedAt"));
            product.setActive(rs.getBoolean("IsActive"));

            return product;
        }
    };

    //Find All products
    public List<Product> findAll() {
        String sql = "SELECT * FROM Products WHERE IsActive = 1";
        return jdbcTemplate.query(sql, PRODUCT_ROW_MAPPER);
    }

    //Find product by ID
    public Product findById(int productId) {
        String sql = "SELECT * FROM Products WHERE ProductId = ? AND IsActive = 1";
        return jdbcTemplate.queryForObject(sql, PRODUCT_ROW_MAPPER, productId);
    }

    //Find products by company, price range, etc.
    public List<Product> findByCompany(String company) {
        String sql = "SELECT * FROM Products WHERE Company = ? AND IsActive = 1";
        return jdbcTemplate.query(sql, PRODUCT_ROW_MAPPER, company);
    }

    // Find products by price range
    public List<Product> findByPriceRange(double minPrice, double maxPrice) {
        String sql = "SELECT * FROM Products WHERE Price_VND BETWEEN ? AND ? AND IsActive = 1";
        return jdbcTemplate.query(sql, PRODUCT_ROW_MAPPER, minPrice, maxPrice);
    }
}
