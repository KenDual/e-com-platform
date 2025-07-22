package com.maiphuhai.model;

import java.sql.Timestamp;

public class Product {
    private int ProductID;
    private String Company;
    private String ProductName;
    private String TypeName;
    private double ScreenSize_Inch;
    private String ScreenResolution;
    private String CPU_Company;
    private String CPU_Type;
    private double CPU_Freq_GHz;
    private int Ram_GB;
    private String Storage;
    private String GPU_Company;
    private String GPU_Type;
    private double Weight_Kg;
    private String OperatingSystem;
    private double Price_VND;
    private int StockQty;
    private Timestamp CreatedAt;
    private boolean IsActive;

    public Product(){}

    public Product(int productID, String company, String productName, String typeName, double screenSize_Inch, String screenResolution, String CPU_Company, String CPU_Type, double CPU_Freq_GHz, int ram_GB, String storage, String GPU_Company, String GPU_Type, double weight_Kg, String operatingSystem, double price_VND, int stockQty, Timestamp createdAt, boolean isActive) {
        ProductID = productID;
        Company = company;
        ProductName = productName;
        TypeName = typeName;
        ScreenSize_Inch = screenSize_Inch;
        ScreenResolution = screenResolution;
        this.CPU_Company = CPU_Company;
        this.CPU_Type = CPU_Type;
        this.CPU_Freq_GHz = CPU_Freq_GHz;
        Ram_GB = ram_GB;
        Storage = storage;
        this.GPU_Company = GPU_Company;
        this.GPU_Type = GPU_Type;
        Weight_Kg = weight_Kg;
        OperatingSystem = operatingSystem;
        Price_VND = price_VND;
        StockQty = stockQty;
        CreatedAt = createdAt;
        IsActive = isActive;
    }

    public int getProductID() {
        return ProductID;
    }

    public void setProductID(int productID) {
        ProductID = productID;
    }

    public String getCompany() {
        return Company;
    }

    public void setCompany(String company) {
        Company = company;
    }

    public String getProductName() {
        return ProductName;
    }

    public void setProductName(String productName) {
        ProductName = productName;
    }

    public String getTypeName() {
        return TypeName;
    }

    public void setTypeName(String typeName) {
        TypeName = typeName;
    }

    public double getScreenSize_Inch() {
        return ScreenSize_Inch;
    }

    public void setScreenSize_Inch(double screenSize_Inch) {
        ScreenSize_Inch = screenSize_Inch;
    }

    public String getScreenResolution() {
        return ScreenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        ScreenResolution = screenResolution;
    }

    public String getCPU_Company() {
        return CPU_Company;
    }

    public void setCPU_Company(String CPU_Company) {
        this.CPU_Company = CPU_Company;
    }

    public String getCPU_Type() {
        return CPU_Type;
    }

    public void setCPU_Type(String CPU_Type) {
        this.CPU_Type = CPU_Type;
    }

    public double getCPU_Freq_GHz() {
        return CPU_Freq_GHz;
    }

    public void setCPU_Freq_GHz(double CPU_Freq_GHz) {
        this.CPU_Freq_GHz = CPU_Freq_GHz;
    }

    public int getRam_GB() {
        return Ram_GB;
    }

    public void setRam_GB(int ram_GB) {
        Ram_GB = ram_GB;
    }

    public String getStorage() {
        return Storage;
    }

    public void setStorage(String storage) {
        Storage = storage;
    }

    public String getGPU_Company() {
        return GPU_Company;
    }

    public void setGPU_Company(String GPU_Company) {
        this.GPU_Company = GPU_Company;
    }

    public String getGPU_Type() {
        return GPU_Type;
    }

    public void setGPU_Type(String GPU_Type) {
        this.GPU_Type = GPU_Type;
    }

    public double getWeight_Kg() {
        return Weight_Kg;
    }

    public void setWeight_Kg(double weight_Kg) {
        Weight_Kg = weight_Kg;
    }

    public String getOperatingSystem() {
        return OperatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        OperatingSystem = operatingSystem;
    }

    public double getPrice_VND() {
        return Price_VND;
    }

    public void setPrice_VND(double price_VND) {
        Price_VND = price_VND;
    }

    public int getStockQty() {
        return StockQty;
    }

    public void setStockQty(int stockQty) {
        StockQty = stockQty;
    }

    public Timestamp getCreatedAt() {
        return CreatedAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        CreatedAt = createdAt;
    }

    public boolean isActive() {
        return IsActive;
    }

    public void setActive(boolean active) {
        IsActive = active;
    }
}
