package com.prakashbankers.repository;
import com.prakashbankers.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByNameIgnoreCase(String name);
    java.util.List<Material> findAllByOrderByNameAsc();
}
