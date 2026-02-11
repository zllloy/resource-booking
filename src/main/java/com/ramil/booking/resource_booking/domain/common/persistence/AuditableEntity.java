package com.ramil.booking.resource_booking.domain.common.persistence;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @CreatedBy
  @Column(name = "created_by", nullable = false, updatable = false, length = 255)
  private String createdBy;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @LastModifiedBy
  @Column(name = "updated_by", nullable = false, length = 255)
  private String updatedBy;

  public OffsetDateTime getCreatedAt() { return createdAt; }
  public String getCreatedBy() { return createdBy; }
  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public String getUpdatedBy() { return updatedBy; }
}
