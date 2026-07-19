package com.prakashbankers.repository;
import com.prakashbankers.entity.Borrowing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    Optional<Borrowing> findByBorrowingId(String borrowingId);
    List<Borrowing> findByLenderId(Long lenderId);

    @Query("SELECT DISTINCT b FROM Borrowing b JOIN FETCH b.lender LEFT JOIN FETCH b.transactions")
    List<Borrowing> findAllWithDetails();
}