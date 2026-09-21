package com.ktx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.AllocationItem;

public interface AllocationItemRepository extends JpaRepository<AllocationItem, Long> {
    List<AllocationItem> findByRunIdOrderByRankNoAsc(Long runId);

    @Query("SELECT i FROM AllocationItem i " +
           "JOIN FETCH i.student s " +
           "JOIN FETCH i.application a " +
           "LEFT JOIN FETCH i.bed b " +
           "LEFT JOIN FETCH b.room r " +
           "LEFT JOIN FETCH r.building bg " +
           "WHERE i.run.id = :runId ORDER BY i.rankNo ASC")
    List<AllocationItem> findByRunIdWithDetailsOrderByRankNoAsc(@Param("runId") Long runId);
}
