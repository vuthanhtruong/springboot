package com.example.demo.Repository;

import com.example.demo.OOP.Messages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessagesRepository extends JpaRepository<Messages, Integer> {
    List<Messages> findBySender_IdAndRecipient_Id(String senderId, String recipientId);
}