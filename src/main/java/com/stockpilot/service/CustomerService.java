package com.stockpilot.service;
import com.stockpilot.exception.CustomerNotFoundException;
import com.stockpilot.model.Customer;
import com.stockpilot.repository.CustomerRepository;

import java.util.List;
public class CustomerService {
    private final CustomerRepository customerRepository = new CustomerRepository();
    public void addCustomer(Customer customer) {
        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email khách hàng '" + customer.getEmail() + "' đã tồn tại trên hệ thống!");
        }
        customerRepository.save(customer);
    }
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    public Customer getCustomerById(int id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Không tìm thấy khách hàng với ID: " + id));
    }
}