package edu.ucsb.cs156.example.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "UCSBDININGCOMMONSMENUITEM")
public class UCSBDiningCommonsMenuItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private String diningCommonsCode;
  private String name;
  private String station;
}
