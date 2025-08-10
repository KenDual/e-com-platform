package com.maiphuhai.DTO;

public class AiProduct {
    private int id;
    private String name;
    private String brand;
    private String company;
    private String specs;
    private String image;
    private Double price;
    private Double weight;
    private String battery;

    public AiProduct() {}

    public AiProduct(int id, String name, String brand, String company, String specs, String image, Double price, Double weight, String battery) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.company = company;
        this.specs = specs;
        this.image = image;
        this.price = price;
        this.weight = weight;
        this.battery = battery;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getSpecs() {
        return specs;
    }

    public void setSpecs(String specs) {
        this.specs = specs;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }
}
