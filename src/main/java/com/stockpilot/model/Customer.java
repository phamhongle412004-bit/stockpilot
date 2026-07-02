package com.stockpilot.model;
import java.util.Objects;
import java.util.regex.Pattern;

public class Customer {
    private int id;
    private String name;
    private String email;
    private String phone;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final String PHONE_REGEX = "^(0|\\+84)[35789]\\d{8}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    public Customer(String name, String email, String phone) {
        this.name = name;
        setEmail(email);
        setPhone(phone);
    }
    public Customer(int id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        setEmail(email);
        setPhone(phone);
    }
     public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Định dạng Email không hợp lệ!");
        }
        this.email = email;
    }
    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam! (VD: 0912345678)");
        }
        this.phone = phone;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(email, customer.email);
    }
    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
    @Override
    public String toString() {
        return String.format("Customer[ID=%d, Name='%s', Email='%s', Phone='%s']", id, name, email, phone);
    }
}