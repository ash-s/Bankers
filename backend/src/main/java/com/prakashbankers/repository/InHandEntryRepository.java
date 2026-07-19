package com.prakashbankers.repository;
import com.prakashbankers.entity.InHandEntry;
import com.prakashbankers.entity.InHandType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InHandEntryRepository extends JpaRepository<InHandEntry, Long> {
    List<InHandEntry> findByType(InHandType type);
    List<InHandEntry> findAllByOrderByDateDescIdDesc();
}