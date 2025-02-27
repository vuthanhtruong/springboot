package com.example.demo.service;

import com.example.demo.OOP.Messages;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveMessage(Messages msg) {
        entityManager.persist(msg);
    }
}
