package com.ramil.booking.resource_booking.domain.payment.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramil.booking.resource_booking.domain.payment.entity.PaymentEntity;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
}
